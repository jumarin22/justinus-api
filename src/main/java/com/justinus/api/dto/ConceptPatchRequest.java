package com.justinus.api.dto;

import jakarta.validation.constraints.Pattern;

public record ConceptPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
        String description
) {
}
