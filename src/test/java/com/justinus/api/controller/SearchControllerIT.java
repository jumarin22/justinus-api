package com.justinus.api.controller;

import tools.jackson.databind.ObjectMapper;
import com.justinus.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchControllerIT extends AbstractIntegrationTest {

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

    private String source(String title, String author) throws Exception {
        return create("/sources", """
                { "title": "%s", "author": "%s", "type": "BOOK",
                  "dateStarted": "2026-01-01", "status": "READING" }
                """.formatted(title, author));
    }

    @Test
    void findsNotesAndSourcesByTheirText() throws Exception {
        String sourceId = source("Zanzibar Chronicles", "Quillon Featherstone");
        String noteId = create("/sources/" + sourceId + "/notes",
                "{ \"content\": \"Perspicacious remarks on xylophones\" }");

        mockMvc.perform(get("/search").param("q", "xylophones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(noteId))
                .andExpect(jsonPath("$.content[0].type").value("NOTE"))
                .andExpect(jsonPath("$.content[0].score").isNumber());

        mockMvc.perform(get("/search").param("q", "zanzibar"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(sourceId))
                .andExpect(jsonPath("$.content[0].type").value("SOURCE"))
                .andExpect(jsonPath("$.content[0].label").value("Zanzibar Chronicles"));

        // Matches on a different property (author) of the same node.
        mockMvc.perform(get("/search").param("q", "Featherstone"))
                .andExpect(jsonPath("$.content[0].id").value(sourceId));
    }

    @Test
    void returnsNoResultsForNoMatch() throws Exception {
        mockMvc.perform(get("/search").param("q", "qqqnonexistentqqq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void treatsLuceneSyntaxCharactersAsPlainText() throws Exception {
        // Each of these is a Lucene parse error if passed through raw.
        for (String q : new String[]{"\"unbalanced", "(open", "a AND", "why?", "foo:bar", "[x TO"}) {
            mockMvc.perform(get("/search").param("q", q)).andExpect(status().isOk());
        }
    }

    @Test
    void rejectsBlankOrMissingQuery() throws Exception {
        mockMvc.perform(get("/search").param("q", "  ")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/search")).andExpect(status().isBadRequest());
    }

    @Test
    void paginatesResults() throws Exception {
        String sourceId = source("Paginatorium", "Someone");
        for (int i = 0; i < 3; i++) {
            create("/sources/" + sourceId + "/notes", "{ \"content\": \"pagination marker\" }");
        }
        mockMvc.perform(get("/search").param("q", "pagination").param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }
}
