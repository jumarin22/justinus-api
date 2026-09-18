package com.justinus.api.service;

import com.justinus.api.dto.SearchResult;
import com.justinus.api.exception.InvalidRequestException;
import com.justinus.api.repository.SearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class SearchService {

    // Lucene query-syntax characters (and the && / || operators, covered by & and |).
    private static final Pattern LUCENE_SPECIAL = Pattern.compile("([+\\-!(){}\\[\\]^\"~*?:\\\\/&|])");

    // Bare uppercase AND/OR/NOT are operators too ("a AND" is a parse error).
    private static final Pattern LUCENE_OPERATOR_WORD = Pattern.compile("\\b(AND|OR|NOT)\\b");

    private final SearchRepository searchRepository;

    public SearchService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    public Page<SearchResult> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            throw new InvalidRequestException("q must not be blank");
        }
        // The index speaks Lucene syntax, where an unbalanced quote or a
        // stray "(" is a parse error (a 500). Treat user input as plain
        // words by escaping the syntax characters.
        String escaped = LUCENE_SPECIAL.matcher(q.trim()).replaceAll("\\\\$1");
        escaped = LUCENE_OPERATOR_WORD.matcher(escaped)
                .replaceAll(m -> m.group().toLowerCase(java.util.Locale.ROOT));
        return searchRepository.search(escaped, pageable);
    }
}
