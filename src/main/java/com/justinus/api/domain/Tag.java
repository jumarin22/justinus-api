package com.justinus.api.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@Node
public class Tag {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;

    private String name;

    // Maintained alongside name specifically to carry the real
    // uniqueness constraint (see V2__tag_name_lower_unique.cypher) --
    // Neo4j constraints can't target an expression like lower(name),
    // so this property exists purely to be the thing the constraint
    // targets. Never exposed via the API; name keeps its display case.
    private String nameLower;

    protected Tag() {
        // SDN
    }

    public Tag(String name) {
        this.name = name;
        this.nameLower = name.toLowerCase();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameLower() {
        return nameLower;
    }
}
