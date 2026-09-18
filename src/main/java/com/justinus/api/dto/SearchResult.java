package com.justinus.api.dto;

import com.justinus.api.domain.LinkableType;

public record SearchResult(String id, LinkableType type, String label, double score) {
}
