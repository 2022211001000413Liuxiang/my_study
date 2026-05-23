package com.study.model;

import java.util.List;

public record NotesResponse(List<NoteDto> notes, List<CategoryDto> categories, StatsDto stats) {}
