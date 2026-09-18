package com.justinus.api.controller;

import com.justinus.api.dto.NotePatchRequest;
import com.justinus.api.dto.NoteRequest;
import com.justinus.api.dto.NoteResponse;
import com.justinus.api.domain.LinkableType;
import com.justinus.api.dto.LinkResponse;
import com.justinus.api.service.LinkService;
import com.justinus.api.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;
    private final LinkService linkService;

    public NoteController(NoteService noteService, LinkService linkService) {
        this.linkService = linkService;
        this.noteService = noteService;
    }

    @GetMapping
    public PagedModel<NoteResponse> list(Pageable pageable) {
        return new PagedModel<>(noteService.list(pageable));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.createTopLevel(request));
    }

    @GetMapping("/{id}")
    public NoteResponse getById(@PathVariable String id) {
        return noteService.getById(id);
    }

    @GetMapping("/{id}/links")
    public PagedModel<LinkResponse> listLinks(@PathVariable String id, Pageable pageable) {
        return new PagedModel<>(linkService.listTouching(LinkableType.NOTE, id, pageable));
    }

    @PatchMapping("/{id}")
    public NoteResponse patch(@PathVariable String id, @Valid @RequestBody NotePatchRequest request) {
        return noteService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        noteService.delete(id);
    }
}
