package com.justinus.api.controller;

import com.justinus.api.dto.ConceptPatchRequest;
import com.justinus.api.dto.ConceptRequest;
import com.justinus.api.dto.ConceptResponse;
import com.justinus.api.dto.GraphResponse;
import com.justinus.api.dto.NoteResponse;
import com.justinus.api.domain.LinkableType;
import com.justinus.api.dto.LinkResponse;
import com.justinus.api.service.ConceptService;
import com.justinus.api.service.LinkService;
import com.justinus.api.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/concepts")
public class ConceptController {

    private final ConceptService conceptService;
    private final LinkService linkService;
    private final NoteService noteService;

    public ConceptController(ConceptService conceptService, LinkService linkService,
                             NoteService noteService) {
        this.noteService = noteService;
        this.linkService = linkService;
        this.conceptService = conceptService;
    }

    @GetMapping
    public PagedModel<ConceptResponse> list(Pageable pageable) {
        return new PagedModel<>(conceptService.list(pageable));
    }

    @PostMapping
    public ResponseEntity<ConceptResponse> create(@Valid @RequestBody ConceptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conceptService.create(request));
    }

    @GetMapping("/{id}")
    public ConceptResponse getById(@PathVariable String id) {
        return conceptService.getById(id);
    }

    @PatchMapping("/{id}")
    public ConceptResponse patch(@PathVariable String id, @Valid @RequestBody ConceptPatchRequest request) {
        return conceptService.patch(id, request);
    }

    @GetMapping("/{id}/links")
    public PagedModel<LinkResponse> listLinks(@PathVariable String id, Pageable pageable) {
        return new PagedModel<>(linkService.listTouching(LinkableType.CONCEPT, id, pageable));
    }

    @GetMapping("/{id}/notes")
    public PagedModel<NoteResponse> listNotes(@PathVariable String id, Pageable pageable) {
        return new PagedModel<>(noteService.listByConcept(id, pageable));
    }

    @GetMapping("/{id}/graph")
    public GraphResponse graph(@PathVariable String id, @RequestParam(defaultValue = "2") int depth) {
        return linkService.conceptGraph(id, depth);
    }
}
