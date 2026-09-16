package com.justinus.api.service;

import com.justinus.api.domain.Source;
import com.justinus.api.dto.SourcePatchRequest;
import com.justinus.api.dto.SourceRequest;
import com.justinus.api.dto.SourceResponse;
import com.justinus.api.exception.InvalidRequestException;
import com.justinus.api.exception.ResourceNotFoundException;
import com.justinus.api.repository.SourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class SourceService {

    private final SourceRepository sourceRepository;

    public SourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional
    public SourceResponse create(SourceRequest request) {
        validateDates(request.dateStarted(), request.dateFinished());
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
        return SourceResponse.from(findOrThrow(id));
    }

    public Page<SourceResponse> list(Pageable pageable) {
        return sourceRepository.findAll(pageable).map(SourceResponse::from);
    }

    @Transactional
    public SourceResponse patch(Long id, SourcePatchRequest request) {
        Source source = findOrThrow(id);

        if (request.title() != null) {
            source.setTitle(request.title());
        }
        if (request.author() != null) {
            source.setAuthor(request.author());
        }
        if (request.type() != null) {
            source.setType(request.type());
        }
        if (request.dateStarted() != null) {
            source.setDateStarted(request.dateStarted());
        }
        if (request.dateFinished() != null) {
            source.setDateFinished(request.dateFinished());
        }
        if (request.status() != null) {
            source.setStatus(request.status());
        }
        if (request.rating() != null) {
            source.setRating(request.rating());
        }
        if (request.generalNotes() != null) {
            source.setGeneralNotes(request.generalNotes());
        }

        validateDates(source.getDateStarted(), source.getDateFinished());
        return SourceResponse.from(source);
    }

    private Source findOrThrow(Long id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Source not found: " + id));
    }

    private void validateDates(LocalDate dateStarted, LocalDate dateFinished) {
        if (dateStarted != null && dateFinished != null && dateFinished.isBefore(dateStarted)) {
            throw new InvalidRequestException("dateFinished cannot be before dateStarted");
        }
    }
}
