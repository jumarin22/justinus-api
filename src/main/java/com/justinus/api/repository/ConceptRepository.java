package com.justinus.api.repository;

import com.justinus.api.domain.Concept;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface ConceptRepository extends Neo4jRepository<Concept, String> {

    Optional<Concept> findByNameLower(String nameLower);
}
