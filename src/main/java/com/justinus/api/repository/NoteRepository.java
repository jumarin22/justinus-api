package com.justinus.api.repository;

import com.justinus.api.domain.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface NoteRepository extends Neo4jRepository<Note, String> {

    Page<Note> findBySourceId(String sourceId, Pageable pageable);

    boolean existsBySourceId(String sourceId);
}
