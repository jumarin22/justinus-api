package com.justinus.api.service;

import com.justinus.api.domain.Source;
import com.justinus.api.dto.SourceRequest;
import com.justinus.api.dto.SourceResponse;
import com.justinus.api.repository.SourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SourceService {

    private final SourceRepository sourceRepository;

    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional
    public SourceResponse create(SourceRequest request) {
        Source source = new Source(
                request.title(),
                request.author(),
                request.type(),
                request.dateStarted(),
                request.dateFinished(),
                request.status(),
                request.rating(),
                request.generalNotes()
        );
        return SourceResponse.from(sourceRepository.save(source));
    }

    public SourceResponse getById(Long id) {
        // Not-found handling (404 vs a raw exception) is deferred to step 7,
        // once GlobalExceptionHandler exists. For now this throws
        // NoSuchElementException, which Spring maps to a 500.
        return sourceRepository.findById(id)
                .map(SourceResponse::from)
                .orElseThrow();
    }

    public Page<SourceResponse> list(Pageable pageable) {
        return sourceRepository.findAll(pageable).map(SourceResponse::from);
    }
}
