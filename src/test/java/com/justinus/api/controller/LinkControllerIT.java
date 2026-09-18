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

class LinkControllerIT extends AbstractIntegrationTest {

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
                { "title": "Meditations", "author": "Marcus Aurelius", "type": "BOOK",
                  "dateStarted": "2026-01-01", "status": "READING" }
                """);
    }

    private String note(String sourceId) throws Exception {
        return create("/sources/" + sourceId + "/notes", "{ \"content\": \"Impediments to action\" }");
    }

    private String linkBody(String fromType, String fromId, String toType, String toId, String type) {
        return """
                { "fromType": "%s", "fromId": "%s", "toType": "%s", "toId": "%s", "type": "%s" }
                """.formatted(fromType, fromId, toType, toId, type);
    }

    @Test
    void linksANoteToAConceptAndFetchesIt() throws Exception {
        String noteId = note(source());
        String conceptId = concept("Amor fati");

        String linkId = create("/links", linkBody("NOTE", noteId, "CONCEPT", conceptId, "SUPPORTS"));

        mockMvc.perform(get("/links/{id}", linkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromType").value("NOTE"))
                .andExpect(jsonPath("$.fromId").value(noteId))
                .andExpect(jsonPath("$.toType").value("CONCEPT"))
                .andExpect(jsonPath("$.toId").value(conceptId))
                .andExpect(jsonPath("$.type").value("SUPPORTS"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void linksAnyTwoNodeKinds() throws Exception {
        String sourceId = source();
        String otherSourceId = source();
        String conceptId = concept("Logos");

        create("/links", linkBody("SOURCE", sourceId, "SOURCE", otherSourceId, "EXTENDS"));
        create("/links", linkBody("CONCEPT", conceptId, "SOURCE", sourceId, "RELATES_TO"));
    }

    @Test
    void listsLinksTouchingANodeInEitherDirection() throws Exception {
        String a = concept("Virtue");
        String b = concept("Duty");
        String c = concept("Fate");

        create("/links", linkBody("CONCEPT", a, "CONCEPT", b, "SUPPORTS"));
        create("/links", linkBody("CONCEPT", c, "CONCEPT", a, "CONTRADICTS"));

        mockMvc.perform(get("/concepts/{id}/links", a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get("/concepts/{id}/links", b))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fromId").value(a))
                .andExpect(jsonPath("$.content[0].toId").value(b));
    }

    @Test
    void listsLinksForSourcesAndNotes() throws Exception {
        String sourceId = source();
        String noteId = note(sourceId);
        String conceptId = concept("Attention");
        create("/links", linkBody("NOTE", noteId, "CONCEPT", conceptId, "RELATES_TO"));
        create("/links", linkBody("SOURCE", sourceId, "CONCEPT", conceptId, "RELATES_TO"));

        mockMvc.perform(get("/notes/{id}/links", noteId))
                .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(get("/sources/{id}/links", sourceId))
                .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(get("/concepts/{id}/links", conceptId))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void returns404WhenAnEndpointDoesNotExist() throws Exception {
        String conceptId = concept("Ataraxia");
        mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON)
                        .content(linkBody("NOTE", "nope", "CONCEPT", conceptId, "SUPPORTS")))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns404WhenStatedTypeDoesNotMatchTheId() throws Exception {
        String conceptId = concept("Apatheia");
        String otherId = concept("Pathos");
        mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON)
                        .content(linkBody("NOTE", conceptId, "CONCEPT", otherId, "SUPPORTS")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsSelfLinks() throws Exception {
        String conceptId = concept("Self");
        mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON)
                        .content(linkBody("CONCEPT", conceptId, "CONCEPT", conceptId, "RELATES_TO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingFieldsAndUnknownLinkType() throws Exception {
        mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        String a = concept("X1");
        String b = concept("X2");
        mockMvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON)
                        .content(linkBody("CONCEPT", a, "CONCEPT", b, "BOGUS")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForUnknownLinkAndUnknownNodeListing() throws Exception {
        mockMvc.perform(get("/links/{id}", "nope")).andExpect(status().isNotFound());
        mockMvc.perform(get("/concepts/{id}/links", "nope")).andExpect(status().isNotFound());
    }
}
