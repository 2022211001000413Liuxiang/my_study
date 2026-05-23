package com.study.model;

import java.util.List;

public record ReviewOverviewDto(
    int dueToday,
    int reviewedToday,
    ReviewLevelStatsDto levels,
    List<ReviewDayDto> completedDays,
    List<ReviewDayDto> upcomingDays,
    List<NoteDto> recentReviewed
) {}
