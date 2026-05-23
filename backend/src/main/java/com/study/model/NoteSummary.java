package com.study.model;

import java.nio.file.Path;
import java.util.List;

public record NoteSummary(
    String id,
    Path absolutePath,
    String path,
    String title,
    String category,
    List<String> tags,
    List<HeadingDto> headings,
    String updatedAt,
    int wordCount,
    boolean favorite,
    String status,
    int reviewCount,
    String lastReviewedAt,
    String nextReviewAt,
    String reviewLevel
) {
  public NoteDto toDto() {
    return new NoteDto(
        id,
        path,
        title,
        category,
        tags,
        headings,
        updatedAt,
        wordCount,
        favorite,
        status,
        reviewCount,
        lastReviewedAt,
        nextReviewAt,
        reviewLevel
    );
  }
}
