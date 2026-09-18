package com.justinus.api.repository;

import com.justinus.api.domain.LinkableType;
import com.justinus.api.dto.GraphNode;
import com.justinus.api.dto.GraphResponse;
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

    public boolean deleteById(String id) {
        return client.query("MATCH ()-[l:LINKS_TO {id: $id}]->() DELETE l RETURN count(*) AS c")
                .bind(id).to("id")
                .fetchAs(Long.class)
                .mappedBy((ts, r) -> r.get("c").asLong())
                .one().orElse(0L) > 0;
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

    /**
     * Everything reachable from a concept within {@code depth} LINKS_TO hops,
     * in either direction. Cypher can't parameterize a path-length bound, so
     * depth is spliced in as an int -- the caller has already range-checked it.
     */
    public GraphResponse graph(String conceptId, int depth) {
        List<GraphNode> nodes = client.query(
                        "MATCH (c:Concept {id: $id})-[:LINKS_TO*0.." + depth + "]-(n) "
                                + "WITH DISTINCT n "
                                + "RETURN n.id AS id, labels(n) AS labels, "
                                + "coalesce(n.title, n.name, left(n.content, 80)) AS label")
                .bind(conceptId).to("id")
                .fetchAs(GraphNode.class)
                .mappedBy((ts, r) -> new GraphNode(
                        r.get("id").asString(),
                        LinkableType.fromLabels(r.get("labels").asList(org.neo4j.driver.Value::asString)),
                        r.get("label").asString(null)))
                .all().stream().toList();

        // relationships(p) yields each hop; DISTINCT collapses relationships
        // shared by several paths into one edge.
        List<LinkResponse> edges = client.query(
                        "MATCH p = (:Concept {id: $id})-[:LINKS_TO*1.." + depth + "]-() "
                                + "UNWIND relationships(p) AS l "
                                + "WITH DISTINCT l, startNode(l) AS a, endNode(l) AS b "
                                + RETURN_LINK
                                + "ORDER BY l.createdAt, l.id")
                .bind(conceptId).to("id")
                .fetchAs(LinkResponse.class)
                .mappedBy((ts, r) -> map(r))
                .all().stream().toList();
        return new GraphResponse(nodes, edges);
    }

    /** Ids of Notes with a LINKS_TO relationship (either direction) to the concept. */
    public Page<String> findNoteIdsLinkedToConcept(String conceptId, Pageable pageable) {
        String match = "MATCH (n:Note)-[:LINKS_TO]-(:Concept {id: $id}) ";
        long total = client.query(match + "RETURN count(DISTINCT n) AS c")
                .bind(conceptId).to("id")
                .fetchAs(Long.class)
                .mappedBy((ts, r) -> r.get("c").asLong())
                .one().orElse(0L);
        List<String> ids = client.query(match
                        + "WITH DISTINCT n RETURN n.id AS id ORDER BY n.createdAt DESC, n.id SKIP $skip LIMIT $limit")
                .bindAll(Map.of("id", conceptId, "skip", pageable.getOffset(), "limit", pageable.getPageSize()))
                .fetchAs(String.class)
                .mappedBy((ts, r) -> r.get("id").asString())
                .all().stream().toList();
        return new PageImpl<>(ids, pageable, total);
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
