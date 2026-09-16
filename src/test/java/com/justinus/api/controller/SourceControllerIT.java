package com.justinus.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.justinus.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SourceControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndFetchesASource() throws Exception {
        String body = """
                {
                  "title": "Meditations",
                  "author": "Marcus Aurelius",
                  "type": "BOOK",
                  "dateStarted": "2026-01-01",
                  "status": "READING"
                }
                """;

        String response = mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Meditations"))
                .andExpect(jsonPath("$.status").value("READING"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/sources/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Marcus Aurelius"));
    }

    @Test
    void rejectsCreationWithoutRequiredFields() throws Exception {
        String body = """
                { "type": "BOOK" }
                """;

        mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsDateFinishedBeforeDateStarted() throws Exception {
        String body = """
                {
                  "title": "Letters from a Stoic",
                  "author": "Seneca",
                  "type": "BOOK",
                  "dateStarted": "2026-02-01",
                  "dateFinished": "2026-01-01",
                  "status": "FINISHED"
                }
                """;

        mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForUnknownSource() throws Exception {
        mockMvc.perform(get("/sources/{id}", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void patchesOnlyProvidedFields() throws Exception {
        String createBody = """
                {
                  "title": "Discourses",
                  "author": "Epictetus",
                  "type": "BOOK",
                  "dateStarted": "2026-01-15",
                  "status": "READING"
                }
                """;

        String response = mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        String patchBody = """
                { "status": "FINISHED", "rating": 5 }
                """;

        mockMvc.perform(patch("/sources/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.author").value("Epictetus"));
    }

    @Test
    void rejectsPatchWithOutOfRangeRating() throws Exception {
        String createBody = """
                {
                  "title": "Enchiridion",
                  "author": "Epictetus",
                  "type": "BOOK",
                  "dateStarted": "2026-01-15",
                  "status": "READING"
                }
                """;

        String response = mockMvc.perform(post("/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/sources/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"rating\": 9 }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsSourcesWithPagination() throws Exception {
        mockMvc.perform(get("/sources").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.size").value(5));
    }
}
