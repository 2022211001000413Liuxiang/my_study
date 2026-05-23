package com.study.controller;

import com.study.service.AuthService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, Object> body) {
    try {
      String token = authService.login(String.valueOf(body.getOrDefault("password", "")));
      return ResponseEntity.ok(Map.of("token", token));
    } catch (IllegalStateException error) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", error.getMessage()));
    } catch (IllegalArgumentException error) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", error.getMessage()));
    }
  }
}
