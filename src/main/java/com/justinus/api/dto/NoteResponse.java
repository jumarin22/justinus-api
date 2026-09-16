package com.justinus.api.dto;

import java.time.Instant;
import java.util.List;

public record NoteResponse(
        Long id,
        Long sourceId,
        String content,
        String locationRef,
        Instant createdAt,
        List<String> tags
) {
    // from(Note) removed with the old JPA entity in the Neo4j pivot --
    // re-added against the new @Node Note in BUILD_PLAN step 10.
}
