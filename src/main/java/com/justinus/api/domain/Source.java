package com.justinus.api.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDate;

@Node
public class Source {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;

    private String title;

    private String author;

    private SourceType type;

    private LocalDate dateStarted;

    private LocalDate dateFinished;

    private SourceStatus status;

    private Integer rating;

    private String generalNotes;

    protected Source() {
        // SDN
    }

    public Source(String title, String author, SourceType type, LocalDate dateStarted,
                  LocalDate dateFinished, SourceStatus status, Integer rating, String generalNotes) {
        this.title = title;
        this.author = author;
        this.type = type;
        this.dateStarted = dateStarted;
        this.dateFinished = dateFinished;
        this.status = status;
        this.rating = rating;
        this.generalNotes = generalNotes;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public SourceType getType() {
        return type;
    }

    public void setType(SourceType type) {
        this.type = type;
    }

    public LocalDate getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(LocalDate dateStarted) {
        this.dateStarted = dateStarted;
    }

    public LocalDate getDateFinished() {
        return dateFinished;
    }

    public void setDateFinished(LocalDate dateFinished) {
        this.dateFinished = dateFinished;
    }

    public SourceStatus getStatus() {
        return status;
    }

    public void setStatus(SourceStatus status) {
        this.status = status;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getGeneralNotes() {
        return generalNotes;
    }

    public void setGeneralNotes(String generalNotes) {
        this.generalNotes = generalNotes;
    }
}
