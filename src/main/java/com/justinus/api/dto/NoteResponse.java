package com.justinus.api.dto;

import com.justinus.api.domain.Note;
import com.justinus.api.domain.Tag;

import java.time.Instant;
import java.util.List;

public record NoteResponse(
        String id,
        String sourceId,
        String content,
        String locationRef,
        Instant createdAt,
        List<String> tags
) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getSource().getId(),
                note.getContent(),
                note.getLocationRef(),
                note.getCreatedAt(),
                note.getTags().stream().map(Tag::getName).sorted().toList()
        );
    }
}
