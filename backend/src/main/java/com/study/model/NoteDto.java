package com.study.model;

import java.util.List;

public record NoteDto(
    String id,
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
) {}
