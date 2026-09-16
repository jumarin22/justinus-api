package com.justinus.api.repository;

import com.justinus.api.domain.Tag;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface TagRepository extends Neo4jRepository<Tag, String> {

    Optional<Tag> findByNameLower(String nameLower);
}
