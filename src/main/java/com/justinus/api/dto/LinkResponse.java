package com.justinus.api.dto;

import com.justinus.api.domain.LinkType;
import com.justinus.api.domain.LinkableType;

import java.time.Instant;

public record LinkResponse(
        String id,
        LinkableType fromType,
        String fromId,
        LinkableType toType,
        String toId,
        LinkType type,
        Instant createdAt
) {
}
