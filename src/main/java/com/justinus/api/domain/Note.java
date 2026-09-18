package com.justinus.api.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Node
public class Note {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;

    @Relationship(type = "FROM_SOURCE", direction = Relationship.Direction.OUTGOING)
    private Source source;

    private String content;

    private String locationRef;

    private Instant createdAt;

    @Relationship(type = "TAGGED", direction = Relationship.Direction.OUTGOING)
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Note() {
        // SDN
    }

    public Note(Source source, String content, String locationRef, Set<Tag> tags) {
        this.source = source;
        this.content = content;
        this.locationRef = locationRef;
        this.createdAt = Instant.now();
        this.tags = tags != null ? new LinkedHashSet<>(tags) : new LinkedHashSet<>();
    }

    public String getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public String getLocationRef() {
        return locationRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLocationRef(String locationRef) {
        this.locationRef = locationRef;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = new LinkedHashSet<>(tags);
    }
}
