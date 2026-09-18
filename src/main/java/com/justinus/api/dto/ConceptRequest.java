package com.justinus.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ConceptRequest(
        @NotBlank String name,
        String description
) {
}
