package com.justinus.api.controller;

import com.justinus.api.dto.LinkRequest;
import com.justinus.api.dto.LinkResponse;
import com.justinus.api.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody LinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(linkService.create(request));
    }

    @GetMapping("/{id}")
    public LinkResponse getById(@PathVariable String id) {
        return linkService.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        linkService.delete(id);
    }
}
