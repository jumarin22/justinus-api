package com.justinus.api.repository;

import com.justinus.api.domain.Source;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface SourceRepository extends Neo4jRepository<Source, String> {
}
