package com.justinus.api.repository;

import com.justinus.api.domain.Concept;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ConceptRepository extends Neo4jRepository<Concept, String> {
}
