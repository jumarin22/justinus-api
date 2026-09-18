package com.justinus.api.repository;

import com.justinus.api.domain.LinkableType;
import com.justinus.api.dto.SearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SearchRepository {

    private final Neo4jClient client;

    public SearchRepository(Neo4jClient client) {
        this.client = client;
    }

    /** {@code luceneQuery} must already be escaped; see SearchService. */
    public Page<SearchResult> search(String luceneQuery, Pageable pageable) {
        long total = client.query("CALL db.index.fulltext.queryNodes('search_index', $q) YIELD node "
                        + "RETURN count(node) AS c")
                .bind(luceneQuery).to("q")
                .fetchAs(Long.class)
                .mappedBy((ts, r) -> r.get("c").asLong())
                .one().orElse(0L);

        List<SearchResult> content = client.query("CALL db.index.fulltext.queryNodes('search_index', $q) "
                        + "YIELD node, score "
                        + "RETURN node.id AS id, labels(node) AS labels, "
                        + "coalesce(node.title, left(node.content, 80)) AS label, score "
                        + "ORDER BY score DESC, id SKIP $skip LIMIT $limit")
                .bindAll(Map.of("q", luceneQuery, "skip", pageable.getOffset(), "limit", pageable.getPageSize()))
                .fetchAs(SearchResult.class)
                .mappedBy((ts, r) -> new SearchResult(
                        r.get("id").asString(),
                        LinkableType.fromLabels(r.get("labels").asList(org.neo4j.driver.Value::asString)),
                        r.get("label").asString(null),
                        r.get("score").asDouble()))
                .all().stream().toList();
        return new PageImpl<>(content, pageable, total);
    }
}
