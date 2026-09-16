package com.justinus.api.repository;

import com.justinus.api.domain.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Page<Note> findBySourceId(Long sourceId, Pageable pageable);
}
