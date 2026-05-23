package com.study.model;

import java.util.List;

public record GraphDto(
    List<GraphNodeDto> nodes,
    List<GraphEdgeDto> edges
) {}
