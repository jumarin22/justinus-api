package com.justinus.api.dto;

import java.util.List;

public record GraphResponse(List<GraphNode> nodes, List<LinkResponse> edges) {
}
