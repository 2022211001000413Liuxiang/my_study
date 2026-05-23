package com.study.model;

import java.util.List;

public record StatsDto(int total, int words, int favorites, int dueReviews, List<TagDto> tags) {}
