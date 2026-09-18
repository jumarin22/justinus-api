package com.justinus.api.service;

import com.justinus.api.domain.Concept;
import com.justinus.api.dto.ConceptPatchRequest;
import com.justinus.api.dto.ConceptRequest;
import com.justinus.api.dto.ConceptResponse;
import com.justinus.api.exception.ConflictException;
import com.justinus.api.exception.ResourceNotFoundException;
import com.justinus.api.repository.ConceptRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
        requireNameFree(concept.getNameLower(), null);
        return ConceptResponse.from(save(concept));
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
            requireNameFree(concept.getNameLower(), concept.getId());
        }
        if (request.description() != null) {
            concept.setDescription(request.description());
        }
        return ConceptResponse.from(save(concept));
    }

    /** Removes the concept and, with it, every LINKS_TO relationship touching it. */
    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        conceptRepository.deleteById(id);
    }

    Concept findOrThrow(String id) {
        return conceptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found: " + id));
    }

    private void requireNameFree(String nameLower, String selfId) {
        conceptRepository.findByNameLower(nameLower)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ConflictException("A concept named \"" + existing.getName() + "\" already exists");
                });
    }

    // The pre-check above gives a friendly message, but two concurrent
    // requests can both pass it; the unique constraint is the real guard.
    private Concept save(Concept concept) {
        try {
            return conceptRepository.save(concept);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A concept with that name already exists");
        }
    }
}
