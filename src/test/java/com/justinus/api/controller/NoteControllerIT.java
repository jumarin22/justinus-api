package com.justinus.api.controller;

import com.justinus.api.repository.TagRepository;
import com.justinus.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoteControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagRepository tagRepository;

    private String createSource() throws Exception {
        String body = """
                {
                  "title": "Enchiridion",
                  "author": "Epictetus",
                  "type": "BOOK",
                  "dateStarted": "2026-01-01",
                  "status": "READING"
                }
                """;
        String response = mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void createsANoteUnderASourceViaNestedRoute() throws Exception {
        String sourceId = createSource();

        String noteBody = """
                {
                  "content": "The dichotomy of control is the whole of the philosophy.",
                  "locationRef": "Book 1, Ch. 2",
                  "tags": ["control", "core-idea"]
                }
                """;

        String response = mockMvc.perform(post("/sources/{id}/notes", sourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceId").value(sourceId))
                .andExpect(jsonPath("$.locationRef").value("Book 1, Ch. 2"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        String noteId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/notes/{id}", noteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("The dichotomy of control is the whole of the philosophy."));
    }

    @Test
    void createsANoteViaTopLevelRouteWithSourceIdInBody() throws Exception {
        String sourceId = createSource();

        String noteBody = String.format("""
                { "sourceId": "%s", "content": "Top-level create." }
                """, sourceId);

        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceId").value(sourceId));
    }

    @Test
    void rejectsTopLevelCreateWithoutSourceId() throws Exception {
        String noteBody = """
                { "content": "Missing sourceId." }
                """;

        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noteBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreationWithoutContent() throws Exception {
        String sourceId = createSource();

        String noteBody = String.format("""
                { "sourceId": "%s" }
                """, sourceId);

        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noteBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404WhenCreatingNoteForUnknownSource() throws Exception {
        String noteBody = """
                { "content": "Orphan note." }
                """;

        mockMvc.perform(post("/sources/{id}/notes", "does-not-exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noteBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns404WhenListingNotesForUnknownSource() throws Exception {
        mockMvc.perform(get("/sources/{id}/notes", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsNotesForASource() throws Exception {
        String sourceId = createSource();
        String noteBody = """
                { "content": "First note." }
                """;
        mockMvc.perform(post("/sources/{id}/notes", sourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(noteBody));

        mockMvc.perform(get("/sources/{id}/notes", sourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sourceId").value(sourceId));
    }

    @Test
    void reusesAnExistingTagCaseInsensitivelyInsteadOfDuplicatingIt() throws Exception {
        String sourceId = createSource();
        long tagCountBefore = tagRepository.count();

        String firstNoteBody = """
                { "content": "First note.", "tags": ["Stoicism", "control"] }
                """;
        mockMvc.perform(post("/sources/{id}/notes", sourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstNoteBody))
                .andExpect(status().isCreated());

        // "stoicism" (different casing) should reuse the existing "Stoicism"
        // row, not create a second one; "ethics" is genuinely new.
        String secondNoteBody = """
                { "content": "Second note.", "tags": ["stoicism", "ethics"] }
                """;
        String response = mockMvc.perform(post("/sources/{id}/notes", sourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondNoteBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var tags = objectMapper.readTree(response).get("tags");
        boolean reusedOriginalCasing = false;
        for (var tag : tags) {
            if (tag.asText().equals("Stoicism")) {
                reusedOriginalCasing = true;
            }
        }
        assertThat(reusedOriginalCasing).isTrue();

        // Exactly 3 new tags total across both notes (Stoicism, control,
        // ethics) -- not 4, which is what a bug in the reuse logic would
        // produce.
        assertThat(tagRepository.count() - tagCountBefore).isEqualTo(3);
    }
}
