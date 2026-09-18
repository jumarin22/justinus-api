package com.justinus.api.repository;

import com.justinus.api.domain.LinkableType;
import com.justinus.api.dto.LinkResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Links are LINKS_TO relationships between arbitrary Source/Note/Concept
 * nodes. SDN's relationship mapping wants a statically typed target
 * entity, which doesn't fit "any of three labels", so this talks to
 * Neo4jClient with plain Cypher instead of a Neo4jRepository.
 */
@Repository
public class LinkRepository {

    private static final String RETURN_LINK = """
            RETURN l.id AS id, labels(a) AS fromLabels, a.id AS fromId,
                   labels(b) AS toLabels, b.id AS toId,
                   l.type AS type, l.createdAt AS createdAt
            """;

    private final Neo4jClient client;

    public LinkRepository(Neo4jClient client) {
        this.client = client;
    }

    public boolean nodeExists(LinkableType type, String id) {
        return client.query("MATCH (n:" + type.label() + " {id: $id}) RETURN count(n) AS c")
                .bind(id).to("id")
                .fetchAs(Long.class)
                .mappedBy((ts, r) -> r.get("c").asLong())
                .one()
                .orElse(0L) > 0;
    }

    public LinkResponse create(LinkableType fromType, String fromId, LinkableType toType, String toId,
                               String linkType) {
        String cypher = "MATCH (a:" + fromType.label() + " {id: $fromId}), (b:" + toType.label()
                + " {id: $toId}) CREATE (a)-[l:LINKS_TO {id: $id, type: $type, createdAt: $createdAt}]->(b) "
                + RETURN_LINK;
        return client.query(cypher)
                .bindAll(Map.of(
                        "fromId", fromId,
                        "toId", toId,
                        "id", UUID.randomUUID().toString(),
                        "type", linkType,
                        "createdAt", Instant.now().atOffset(ZoneOffset.UTC)))
                .fetchAs(LinkResponse.class)
                .mappedBy((ts, r) -> map(r))
                .one()
                .orElseThrow();
    }

    public Optional<LinkResponse> findById(String id) {
        return client.query("MATCH (a)-[l:LINKS_TO {id: $id}]->(b) " + RETURN_LINK)
                .bind(id).to("id")
                .fetchAs(LinkResponse.class)
                .mappedBy((ts, r) -> map(r))
                .one();
    }

    public Page<LinkResponse> findTouching(LinkableType type, String id, Pageable pageable) {
        String match = "MATCH (n:" + type.label() + " {id: $id})-[l:LINKS_TO]-(m) ";
        long total = client.query(match + "RETURN count(l) AS c")
                .bind(id).to("id")
                .fetchAs(Long.class)
                .mappedBy((ts, r) -> r.get("c").asLong())
                .one().orElse(0L);

        // Report the relationship in its stored direction, whichever end n is.
        String cypher = match
                + "WITH l, startNode(l) AS a, endNode(l) AS b " + RETURN_LINK
                + "ORDER BY l.createdAt, l.id SKIP $skip LIMIT $limit";
        List<LinkResponse> content = client.query(cypher)
                .bindAll(Map.of("id", id, "skip", pageable.getOffset(), "limit", pageable.getPageSize()))
                .fetchAs(LinkResponse.class)
                .mappedBy((ts, r) -> map(r))
                .all().stream().toList();
        return new PageImpl<>(content, pageable, total);
    }

    private static LinkResponse map(org.neo4j.driver.Record r) {
        return new LinkResponse(
                r.get("id").asString(),
                LinkableType.fromLabels(r.get("fromLabels").asList(org.neo4j.driver.Value::asString)),
                r.get("fromId").asString(),
                LinkableType.fromLabels(r.get("toLabels").asList(org.neo4j.driver.Value::asString)),
                r.get("toId").asString(),
                com.justinus.api.domain.LinkType.valueOf(r.get("type").asString()),
                r.get("createdAt").asZonedDateTime().toInstant());
    }
}
