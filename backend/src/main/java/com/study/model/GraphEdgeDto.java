package com.study.model;

import java.util.List;

public record GraphEdgeDto(
    String source,
    String target,
    int weight,
    List<String> reasons
) {}
