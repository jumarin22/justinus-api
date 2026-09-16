package com.justinus.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * {@code sourceId} is required on {@code POST /notes} and ignored on
 * {@code POST /sources/{id}/notes}, where the path variable wins --
 * enforced in the service, not here, since it's contextual rather than
 * a fixed per-field rule.
 *
 * {@code tags} are plain names, not ids: the service finds an existing
 * Tag case-insensitively or creates a new one, so callers never need
 * to know a tag's id up front.
 */
public record NoteRequest(
        Long sourceId,
        @NotBlank String content,
        String locationRef,
        List<String> tags
) {
}
