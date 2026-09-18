// Backfill Concept.nameLower for nodes created before it existed, so
// the unique constraint in V5 doesn't fail on (or ignore) them. A
// separate migration because Neo4j refuses a data write and a schema
// change in the same transaction. If two existing concepts differ only
// by case, V5 then fails loudly instead of silently picking a winner.
MATCH (c:Concept) WHERE c.nameLower IS NULL SET c.nameLower = toLower(trim(c.name));
