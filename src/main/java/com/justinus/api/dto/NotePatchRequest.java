package com.justinus.api.dto;

import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Fields left out (null) are unchanged. {@code tags}, when present,
 * replaces the whole tag set -- an empty list clears it.
 */
public record NotePatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") String content,
        String locationRef,
        List<String> tags
) {
}
