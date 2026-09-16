package com.justinus.api.dto;

import com.justinus.api.domain.SourceStatus;
import com.justinus.api.domain.SourceType;

import java.time.LocalDate;

public record SourceResponse(
        Long id,
        String title,
        String author,
        SourceType type,
        LocalDate dateStarted,
        LocalDate dateFinished,
        SourceStatus status,
        Integer rating,
        String generalNotes
) {
    // from(Source) removed with the old JPA entity in the Neo4j pivot --
    // re-added against the new @Node Source in BUILD_PLAN step 5.
}
