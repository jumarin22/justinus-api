package com.justinus.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.justinus.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.justinus.api.domain.Concept;
import com.justinus.api.repository.ConceptRepository;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConceptControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConceptRepository conceptRepository;

    private String createConcept(String body) throws Exception {
        String response = mockMvc.perform(post("/concepts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void createsAndFetchesAConcept() throws Exception {
        String id = createConcept("""
                { "name": "Free will", "description": "Can we choose otherwise?" }
                """);

        mockMvc.perform(get("/concepts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Free will"))
                .andExpect(jsonPath("$.description").value("Can we choose otherwise?"));
    }

    @Test
    void createsAConceptWithoutDescription() throws Exception {
        String id = createConcept("{ \"name\": \"Moral luck\" }");

        mockMvc.perform(get("/concepts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Moral luck"))
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/concepts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"  \" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForUnknownConcept() throws Exception {
        mockMvc.perform(get("/concepts/{id}", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void patchesOnlyProvidedFieldsAndPersists() throws Exception {
        String id = createConcept("""
                { "name": "Amor fati", "description": "Love of fate" }
                """);

        mockMvc.perform(patch("/concepts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"description\": \"Embrace what happens\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Amor fati"));

        mockMvc.perform(get("/concepts/{id}", id))
                .andExpect(jsonPath("$.description").value("Embrace what happens"));
    }

    @Test
    void rejectsPatchWithBlankName() throws Exception {
        String id = createConcept("{ \"name\": \"Eudaimonia\" }");

        mockMvc.perform(patch("/concepts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsConceptsWithPagination() throws Exception {
        mockMvc.perform(get("/concepts").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.size").value(5));
    }

    @Test
    void rejectsDuplicateNamesCaseInsensitively() throws Exception {
        createConcept("{ \"name\": \"Hedonic treadmill\" }");

        for (String dup : new String[]{"Hedonic treadmill", "hedonic TREADMILL", "  Hedonic treadmill  "}) {
            mockMvc.perform(post("/concepts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"name\": \"" + dup + "\" }"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Test
    void rejectsRenamingOntoAnExistingName() throws Exception {
        createConcept("{ \"name\": \"Occupied name\" }");
        String id = createConcept("{ \"name\": \"Other name\" }");

        mockMvc.perform(patch("/concepts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"occupied NAME\" }"))
                .andExpect(status().isConflict());
    }

    @Test
    void allowsRenamingToADifferentCaseOfItsOwnName() throws Exception {
        String id = createConcept("{ \"name\": \"self rename\" }");

        mockMvc.perform(patch("/concepts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Self Rename\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Self Rename"));
    }

    @Test
    void databaseConstraintBacksUpTheServiceCheck() {
        // Bypasses the service's pre-check, standing in for the race where
        // two concurrent creates both pass it.
        conceptRepository.save(new Concept("Constraint check", null));
        assertThrows(DataIntegrityViolationException.class,
                () -> conceptRepository.save(new Concept("CONSTRAINT CHECK", null)));
    }
}
