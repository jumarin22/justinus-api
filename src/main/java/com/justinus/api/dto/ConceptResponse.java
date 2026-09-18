package com.justinus.api.dto;

import com.justinus.api.domain.Concept;

public record ConceptResponse(
        String id,
        String name,
        String description
) {
    public static ConceptResponse from(Concept concept) {
        return new ConceptResponse(concept.getId(), concept.getName(), concept.getDescription());
    }
}
