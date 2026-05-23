package com.study.model;

public record ReviewLevelStatsDto(
    int unreviewed,
    int again,
    int normal,
    int easy,
    int scheduled
) {}
