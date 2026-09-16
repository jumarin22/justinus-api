package com.justinus.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "location_ref")
    private String locationRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "note_tags",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Note() {
        // JPA
    }

    public Note(Source source, String content, String locationRef, Set<Tag> tags) {
        this.source = source;
        this.content = content;
        this.locationRef = locationRef;
        this.createdAt = Instant.now();
        this.tags = tags != null ? new LinkedHashSet<>(tags) : new LinkedHashSet<>();
    }

    public Long getId() {
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
}
