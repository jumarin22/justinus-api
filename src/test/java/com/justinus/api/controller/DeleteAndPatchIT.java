package com.justinus.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.justinus.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeleteAndPatchIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String create(String path, String body) throws Exception {
        String response = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String concept(String name) throws Exception {
        return create("/concepts", "{ \"name\": \"" + name + "\" }");
    }

    private String source() throws Exception {
        return create("/sources", """
                { "title": "Delete test", "author": "Someone", "type": "BOOK",
                  "dateStarted": "2026-01-01", "status": "READING" }
                """);
    }

    private String note(String sourceId) throws Exception {
        return create("/sources/" + sourceId + "/notes", "{ \"content\": \"a note\", \"tags\": [\"keep-me\"] }");
    }

    private String link(String fromType, String fromId, String toType, String toId) throws Exception {
        return create("/links", """
                { "fromType": "%s", "fromId": "%s", "toType": "%s", "toId": "%s", "type": "RELATES_TO" }
                """.formatted(fromType, fromId, toType, toId));
    }

    // ---- DELETE

    @Test
    void deletesALinkAndLeavesItsEndpoints() throws Exception {
        String a = concept("Del link A");
        String b = concept("Del link B");
        String linkId = link("CONCEPT", a, "CONCEPT", b);

        mockMvc.perform(delete("/links/{id}", linkId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/links/{id}", linkId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/concepts/{id}", a)).andExpect(status().isOk());
        mockMvc.perform(get("/concepts/{id}", b)).andExpect(status().isOk());
        mockMvc.perform(get("/concepts/{id}/links", a)).andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(delete("/links/{id}", linkId)).andExpect(status().isNotFound());
    }

    @Test
    void deletingAConceptRemovesItsLinksButNotTheOtherEnds() throws Exception {
        String sourceId = source();
        String noteId = note(sourceId);
        String conceptId = concept("Del concept");
        String linkId = link("NOTE", noteId, "CONCEPT", conceptId);

        mockMvc.perform(delete("/concepts/{id}", conceptId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/concepts/{id}", conceptId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/links/{id}", linkId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/notes/{id}", noteId)).andExpect(status().isOk());
        mockMvc.perform(get("/notes/{id}/links", noteId)).andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void deletingAConceptFreesItsName() throws Exception {
        String id = concept("Reusable name");
        mockMvc.perform(delete("/concepts/{id}", id)).andExpect(status().isNoContent());
        concept("Reusable name");
    }

    @Test
    void deletingANoteRemovesItsLinksAndLeavesSourceAndTags() throws Exception {
        String sourceId = source();
        String noteId = note(sourceId);
        String conceptId = concept("Del note concept");
        String linkId = link("NOTE", noteId, "CONCEPT", conceptId);

        mockMvc.perform(delete("/notes/{id}", noteId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/notes/{id}", noteId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/links/{id}", linkId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/sources/{id}/notes", sourceId)).andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get("/sources/{id}", sourceId)).andExpect(status().isOk());
        mockMvc.perform(get("/concepts/{id}", conceptId)).andExpect(status().isOk());

        // The tag survives even though no note uses it now: a new note
        // reuses it rather than tripping the unique constraint.
        note(sourceId);
    }

    @Test
    void refusesToDeleteASourceThatStillHasNotes() throws Exception {
        String sourceId = source();
        String noteId = note(sourceId);

        mockMvc.perform(delete("/sources/{id}", sourceId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        mockMvc.perform(get("/sources/{id}", sourceId)).andExpect(status().isOk());
        mockMvc.perform(get("/notes/{id}", noteId)).andExpect(status().isOk());
    }

    @Test
    void deletesANotelessSourceAndItsLinks() throws Exception {
        String sourceId = source();
        String conceptId = concept("Del source concept");
        String linkId = link("SOURCE", sourceId, "CONCEPT", conceptId);

        mockMvc.perform(delete("/sources/{id}", sourceId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/sources/{id}", sourceId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/links/{id}", linkId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/concepts/{id}", conceptId)).andExpect(status().isOk());
    }

    @Test
    void sourceCanBeDeletedOnceItsNotesAre() throws Exception {
        String sourceId = source();
        String noteId = note(sourceId);
        mockMvc.perform(delete("/notes/{id}", noteId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/sources/{id}", sourceId)).andExpect(status().isNoContent());
    }

    @Test
    void deletingUnknownIdsIs404() throws Exception {
        for (String path : new String[]{"/links/nope", "/concepts/nope", "/notes/nope", "/sources/nope"}) {
            mockMvc.perform(delete(path)).andExpect(status().isNotFound());
        }
    }

    // ---- PATCH /notes/{id}

    @Test
    void patchesOnlyProvidedNoteFieldsAndPersists() throws Exception {
        String noteId = note(source());

        mockMvc.perform(patch("/notes/{id}", noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"fixed typo\", \"locationRef\": \"p. 12\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("fixed typo"))
                .andExpect(jsonPath("$.locationRef").value("p. 12"))
                .andExpect(jsonPath("$.tags[0]").value("keep-me"));

        mockMvc.perform(get("/notes/{id}", noteId))
                .andExpect(jsonPath("$.content").value("fixed typo"))
                .andExpect(jsonPath("$.tags.length()").value(1));
    }

    @Test
    void patchReplacesTagsAndEmptyListClearsThem() throws Exception {
        String noteId = note(source());

        mockMvc.perform(patch("/notes/{id}", noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"tags\": [\"Alpha\", \"beta\"] }"))
                .andExpect(jsonPath("$.tags.length()").value(2));
        mockMvc.perform(get("/notes/{id}", noteId))
                .andExpect(jsonPath("$.tags[0]").value("Alpha"))
                .andExpect(jsonPath("$.tags[1]").value("beta"));

        mockMvc.perform(patch("/notes/{id}", noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"tags\": [] }"))
                .andExpect(jsonPath("$.tags.length()").value(0));
        mockMvc.perform(get("/notes/{id}", noteId))
                .andExpect(jsonPath("$.tags.length()").value(0));
    }

    @Test
    void patchRejectsBlankContentAndUnknownNote() throws Exception {
        String noteId = note(source());
        mockMvc.perform(patch("/notes/{id}", noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"  \" }"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/notes/{id}", "nope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"content\": \"x\" }"))
                .andExpect(status().isNotFound());
    }
}
