package com.justinus.api.controller;

import com.justinus.api.dto.NoteRequest;
import com.justinus.api.dto.NoteResponse;
import com.justinus.api.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
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
    public NoteResponse getById(@PathVariable Long id) {
        return noteService.getById(id);
    }
}
