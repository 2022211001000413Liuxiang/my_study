package com.study.model;

import java.util.List;

public record GraphNodeDto(
    String id,
    String path,
    String title,
    String category,
    List<String> tags,
    int wordCount,
    String status,
    String reviewLevel,
    boolean dueReview,
    String updatedAt
) {}
