package com.justinus.api.dto;

import com.justinus.api.domain.SourceStatus;
import com.justinus.api.domain.SourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

/**
 * All fields optional -- only non-null fields are applied to the
 * existing Source. There's no way to explicitly clear a field back to
 * null via PATCH; that would need a full PUT-style replace, which is
 * out of scope for now.
 */
public record SourcePatchRequest(
        String title,
        String author,
        SourceType type,
        LocalDate dateStarted,
        LocalDate dateFinished,
        SourceStatus status,
        @Min(1) @Max(5) Integer rating,
        String generalNotes
) {
}
