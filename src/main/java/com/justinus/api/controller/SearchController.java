package com.justinus.api.controller;

import com.justinus.api.dto.SearchResult;
import com.justinus.api.service.SearchService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public PagedModel<SearchResult> search(@RequestParam String q, Pageable pageable) {
        return new PagedModel<>(searchService.search(q, pageable));
    }
}
