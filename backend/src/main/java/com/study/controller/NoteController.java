package com.study.controller;

import com.study.model.NoteDto;
import com.study.model.NotesResponse;
import com.study.model.ReviewQueueDto;
import com.study.service.AuthService;
import com.study.service.NoteService;
import com.study.service.NoteService.MetaPatch;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NoteController {
  private final NoteService noteService;
  private final AuthService authService;

  public NoteController(NoteService noteService, AuthService authService) {
    this.noteService = noteService;
    this.authService = authService;
  }

  @GetMapping("/notes")
  public NotesResponse notes(
      @RequestParam(defaultValue = "") String category,
      @RequestParam(defaultValue = "") String q
  ) throws IOException {
    return noteService.list(category, q);
  }

  @GetMapping("/notes/{id}")
  public NoteService.NoteDetail note(@PathVariable String id) throws IOException {
    return noteService.read(id);
  }

  @GetMapping("/reviews/today")
  public ReviewQueueDto reviewsToday() {
    return noteService.reviewsToday();
  }

  @PostMapping("/reviews/{id}")
  public ReviewQueueDto submitReview(
      @PathVariable String id,
      @RequestBody ReviewRequest request
  ) throws IOException {
    return noteService.submitReview(id, request.level);
  }

  @PostMapping("/notes")
  public ResponseEntity<?> create(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestBody Map<String, Object> body
  ) throws IOException {
    ResponseEntity<?> unauthorized = requireAuth(authorization);
    if (unauthorized != null) {
      return unauthorized;
    }
    NoteDto note = noteService.create(
        String.valueOf(body.getOrDefault("title", "新的学习记录")),
        String.valueOf(body.getOrDefault("category", "inbox")),
        body.get("markdown") == null ? "" : String.valueOf(body.get("markdown"))
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("note", note));
  }

  @PutMapping("/notes/{id}")
  public ResponseEntity<?> update(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable String id,
      @RequestBody SaveNoteRequest request
  ) throws IOException {
    ResponseEntity<?> unauthorized = requireAuth(authorization);
    if (unauthorized != null) {
      return unauthorized;
    }
    NoteDto note = noteService.update(id, request.markdown, request.meta);
    return ResponseEntity.ok(Map.of("note", note));
  }

  @PatchMapping("/notes/{id}/move")
  public ResponseEntity<?> move(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable String id,
      @RequestBody Map<String, Object> body
  ) throws IOException {
    ResponseEntity<?> unauthorized = requireAuth(authorization);
    if (unauthorized != null) {
      return unauthorized;
    }
    NoteDto note = noteService.move(
        id,
        String.valueOf(body.getOrDefault("title", "")),
        String.valueOf(body.getOrDefault("category", ""))
    );
    return ResponseEntity.ok(Map.of("note", note));
  }

  @DeleteMapping("/notes/{id}")
  public ResponseEntity<?> delete(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable String id
  ) throws IOException {
    ResponseEntity<?> unauthorized = requireAuth(authorization);
    if (unauthorized != null) {
      return unauthorized;
    }
    noteService.delete(id);
    return ResponseEntity.ok(Map.of("ok", true));
  }

  @GetMapping("/assets/**")
  public ResponseEntity<Resource> asset(HttpServletRequest request) throws IOException {
    String uri = request.getRequestURI();
    String relativePath = uri.substring("/api/assets/".length());
    return noteService.resolveAssetPath(relativePath)
        .map(path -> buildAssetResponse(path))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private ResponseEntity<Resource> buildAssetResponse(Path path) {
    try {
      String contentType = Files.probeContentType(path);
      MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
      return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(path));
    } catch (IOException error) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private ResponseEntity<?> requireAuth(String authorization) {
    if (!authService.verify(authorization)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "请先登录后台。"));
    }
    return null;
  }

  public static class SaveNoteRequest {
    public String markdown = "";
    public MetaPatch meta = new MetaPatch();
  }

  public static class ReviewRequest {
    public String level = "";
  }
}
