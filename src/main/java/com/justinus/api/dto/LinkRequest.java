package com.justinus.api.dto;

import com.justinus.api.domain.LinkType;
import com.justinus.api.domain.LinkableType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LinkRequest(
        @NotNull LinkableType fromType,
        @NotBlank String fromId,
        @NotNull LinkableType toType,
        @NotBlank String toId,
        @NotNull LinkType type
) {
}
