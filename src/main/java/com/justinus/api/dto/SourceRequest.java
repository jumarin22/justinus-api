package com.justinus.api.dto;

import com.justinus.api.domain.SourceStatus;
import com.justinus.api.domain.SourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SourceRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotNull SourceType type,
        @NotNull LocalDate dateStarted,
        LocalDate dateFinished,
        @NotNull SourceStatus status,
        @Min(1) @Max(5) Integer rating,
        String generalNotes
) {
}
