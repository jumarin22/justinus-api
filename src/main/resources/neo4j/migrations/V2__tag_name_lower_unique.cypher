// Tag.nameLower uniqueness constraint (SPEC.md's Tag section): Neo4j's
// native uniqueness constraints only support exact property equality,
// not expressions like lower(name), so a maintained nameLower property
// is what actually gets the constraint -- name stays the
// display-cased value.
CREATE CONSTRAINT tag_name_lower_unique IF NOT EXISTS
FOR (t:Tag)
REQUIRE t.nameLower IS UNIQUE;
