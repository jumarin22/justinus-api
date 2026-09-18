package com.justinus.api.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.util.Locale;

@Node
public class Concept {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;

    private String name;

    private String description;

    // Maintained alongside name to carry the uniqueness constraint (see
    // V5__concept_name_lower_unique.cypher); never exposed via the API.
    private String nameLower;

    protected Concept() {
        // SDN
    }

    public Concept(String name, String description) {
        setName(name);
        this.description = description;
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

    public void setName(String name) {
        this.name = name.strip();
        this.nameLower = this.name.toLowerCase(Locale.ROOT);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
