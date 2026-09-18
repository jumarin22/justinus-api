package com.justinus.api.controller;

import com.justinus.api.dto.NoteRequest;
import com.justinus.api.dto.NoteResponse;
import com.justinus.api.dto.SourcePatchRequest;
import com.justinus.api.dto.SourceRequest;
import com.justinus.api.dto.SourceResponse;
import com.justinus.api.domain.LinkableType;
import com.justinus.api.dto.LinkResponse;
import com.justinus.api.service.LinkService;
import com.justinus.api.service.NoteService;
import com.justinus.api.service.SourceService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sources")
public class SourceController {

    private final SourceService sourceService;
    private final LinkService linkService;
    private final NoteService noteService;

    public SourceController(SourceService sourceService, NoteService noteService, LinkService linkService) {
        this.linkService = linkService;
        this.sourceService = sourceService;
        this.noteService = noteService;
    }

    @GetMapping
    public PagedModel<SourceResponse> list(Pageable pageable) {
        return new PagedModel<>(sourceService.list(pageable));
    }

    @PostMapping
    public ResponseEntity<SourceResponse> create(@Valid @RequestBody SourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sourceService.create(request));
    }

    @GetMapping("/{id}")
    public SourceResponse getById(@PathVariable String id) {
        return sourceService.getById(id);
    }

    @PatchMapping("/{id}")
    public SourceResponse patch(@PathVariable String id, @Valid @RequestBody SourcePatchRequest request) {
        return sourceService.patch(id, request);
    }

    @GetMapping("/{id}/notes")
    public PagedModel<NoteResponse> listNotes(@PathVariable String id, Pageable pageable) {
        return new PagedModel<>(noteService.listBySource(id, pageable));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<NoteResponse> createNote(@PathVariable String id, @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(id, request));
    }

    @GetMapping("/{id}/links")
    public PagedModel<LinkResponse> listLinks(@PathVariable String id, Pageable pageable) {
        return new PagedModel<>(linkService.listTouching(LinkableType.SOURCE, id, pageable));
    }
}
