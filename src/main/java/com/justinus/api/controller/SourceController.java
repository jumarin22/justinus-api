package com.justinus.api.controller;

import com.justinus.api.dto.SourcePatchRequest;
import com.justinus.api.dto.SourceRequest;
import com.justinus.api.dto.SourceResponse;
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

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
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
    public SourceResponse getById(@PathVariable Long id) {
        return sourceService.getById(id);
    }

    @PatchMapping("/{id}")
    public SourceResponse patch(@PathVariable Long id, @Valid @RequestBody SourcePatchRequest request) {
        return sourceService.patch(id, request);
    }
}
