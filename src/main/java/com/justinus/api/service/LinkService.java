package com.justinus.api.service;

import com.justinus.api.domain.LinkableType;
import com.justinus.api.dto.GraphResponse;
import com.justinus.api.dto.LinkRequest;
import com.justinus.api.dto.LinkResponse;
import com.justinus.api.exception.InvalidRequestException;
import com.justinus.api.exception.ResourceNotFoundException;
import com.justinus.api.repository.LinkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LinkService {

    // Variable-length path cost grows fast with depth; cap it.
    private static final int MAX_GRAPH_DEPTH = 5;

    private final LinkRepository linkRepository;

    public LinkService(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @Transactional
    public LinkResponse create(LinkRequest request) {
        if (request.fromType() == request.toType() && request.fromId().equals(request.toId())) {
            throw new InvalidRequestException("A node cannot link to itself");
        }
        // Cypher needs a label to MATCH against, so each endpoint is looked
        // up by its stated type; a wrong type or id is a 404, not a silent
        // no-op CREATE that matched nothing.
        requireNode(request.fromType(), request.fromId());
        requireNode(request.toType(), request.toId());
        return linkRepository.create(request.fromType(), request.fromId(),
                request.toType(), request.toId(), request.type().name());
    }

    public LinkResponse getById(String id) {
        return linkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found: " + id));
    }

    public Page<LinkResponse> listTouching(LinkableType type, String id, Pageable pageable) {
        requireNode(type, id);
        return linkRepository.findTouching(type, id, pageable);
    }

    public GraphResponse conceptGraph(String conceptId, int depth) {
        if (depth < 1 || depth > MAX_GRAPH_DEPTH) {
            throw new InvalidRequestException("depth must be between 1 and " + MAX_GRAPH_DEPTH);
        }
        requireNode(LinkableType.CONCEPT, conceptId);
        return linkRepository.graph(conceptId, depth);
    }

    public Page<String> listNoteIdsLinkedToConcept(String conceptId, Pageable pageable) {
        requireNode(LinkableType.CONCEPT, conceptId);
        return linkRepository.findNoteIdsLinkedToConcept(conceptId, pageable);
    }

    private void requireNode(LinkableType type, String id) {
        if (!linkRepository.nodeExists(type, id)) {
            throw new ResourceNotFoundException(type.label() + " not found: " + id);
        }
    }
}
