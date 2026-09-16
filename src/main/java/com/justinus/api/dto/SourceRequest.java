package com.justinus.api.dto;

import com.justinus.api.domain.SourceStatus;
import com.justinus.api.domain.SourceType;

import java.time.LocalDate;

public record SourceRequest(
        String title,
        String author,
        SourceType type,
        LocalDate dateStarted,
        LocalDate dateFinished,
        SourceStatus status,
        Short rating,
        String generalNotes
) {
}
