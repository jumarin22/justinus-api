package com.justinus.api.service;

import com.justinus.api.domain.Concept;
import com.justinus.api.dto.ConceptPatchRequest;
import com.justinus.api.dto.ConceptRequest;
import com.justinus.api.dto.ConceptResponse;
import com.justinus.api.exception.ResourceNotFoundException;
import com.justinus.api.repository.ConceptRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConceptService {

    private final ConceptRepository conceptRepository;

    public ConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    @Transactional
    public ConceptResponse create(ConceptRequest request) {
        Concept concept = new Concept(request.name(), request.description());
        return ConceptResponse.from(conceptRepository.save(concept));
    }

    public ConceptResponse getById(String id) {
        return ConceptResponse.from(findOrThrow(id));
    }

    public Page<ConceptResponse> list(Pageable pageable) {
        return conceptRepository.findAll(pageable).map(ConceptResponse::from);
    }

    @Transactional
    public ConceptResponse patch(String id, ConceptPatchRequest request) {
        Concept concept = findOrThrow(id);
        if (request.name() != null) {
            concept.setName(request.name());
        }
        if (request.description() != null) {
            concept.setDescription(request.description());
        }
        return ConceptResponse.from(conceptRepository.save(concept));
    }

    Concept findOrThrow(String id) {
        return conceptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found: " + id));
    }
}
