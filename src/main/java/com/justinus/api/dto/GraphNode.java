package com.justinus.api.dto;

import com.justinus.api.domain.LinkableType;

public record GraphNode(String id, LinkableType type, String label) {
}
