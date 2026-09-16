package com.justinus.api.service;

import com.justinus.api.domain.Note;
import com.justinus.api.domain.Source;
import com.justinus.api.domain.Tag;
import com.justinus.api.dto.NoteRequest;
import com.justinus.api.dto.NoteResponse;
import com.justinus.api.exception.InvalidRequestException;
import com.justinus.api.exception.ResourceNotFoundException;
import com.justinus.api.repository.NoteRepository;
import com.justinus.api.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final SourceService sourceService;

    public NoteService(NoteRepository noteRepository, TagRepository tagRepository, SourceService sourceService) {
        this.noteRepository = noteRepository;
        this.tagRepository = tagRepository;
        this.sourceService = sourceService;
    }

    @Transactional
    public NoteResponse create(String sourceId, NoteRequest request) {
        Source source = sourceService.findOrThrow(sourceId);
        Set<Tag> tags = resolveTags(request.tags());
        Note note = new Note(source, request.content(), request.locationRef(), tags);
        return NoteResponse.from(noteRepository.save(note));
    }

    @Transactional
    public NoteResponse createTopLevel(NoteRequest request) {
        if (request.sourceId() == null) {
            throw new InvalidRequestException("sourceId is required");
        }
        return create(request.sourceId(), request);
    }

    public NoteResponse getById(String id) {
        return NoteResponse.from(findOrThrow(id));
    }

    public Page<NoteResponse> list(Pageable pageable) {
        return noteRepository.findAll(pageable).map(NoteResponse::from);
    }

    public Page<NoteResponse> listBySource(String sourceId, Pageable pageable) {
        sourceService.findOrThrow(sourceId);
        return noteRepository.findBySourceId(sourceId, pageable).map(NoteResponse::from);
    }

    private Note findOrThrow(String id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + id));
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Tag> tags = new LinkedHashSet<>();
        for (String name : tagNames) {
            Tag tag = tagRepository.findByNameLower(name.toLowerCase())
                    .orElseGet(() -> tagRepository.save(new Tag(name)));
            tags.add(tag);
        }
        return tags;
    }
}
