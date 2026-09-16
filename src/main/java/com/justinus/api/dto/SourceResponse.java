package com.justinus.api.dto;

import com.justinus.api.domain.Source;
import com.justinus.api.domain.SourceStatus;
import com.justinus.api.domain.SourceType;

import java.time.LocalDate;

public record SourceResponse(
        String id,
        String title,
        String author,
        SourceType type,
        LocalDate dateStarted,
        LocalDate dateFinished,
        SourceStatus status,
        Integer rating,
        String generalNotes
) {
    public static SourceResponse from(Source source) {
        return new SourceResponse(
                source.getId(),
                source.getTitle(),
                source.getAuthor(),
                source.getType(),
                source.getDateStarted(),
                source.getDateFinished(),
                source.getStatus(),
                source.getRating(),
                source.getGeneralNotes()
        );
    }
}
