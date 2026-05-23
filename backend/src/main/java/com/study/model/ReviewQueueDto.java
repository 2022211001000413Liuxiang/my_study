package com.study.model;

import java.util.List;

public record ReviewQueueDto(
    List<NoteDto> notes,
    int dueCount,
    int reviewedToday,
    String nextReviewAt
) {}
