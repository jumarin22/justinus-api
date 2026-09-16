// BUILD_PLAN.md step 3: deliberately minimal -- there's no real schema
// to migrate yet (Neo4j is schema-optional, and no entity's constraints
// are decided until step 4 onward). This migration exists purely to
// prove neo4j-migrations runs end to end and records itself in its own
// history subgraph. Tag's real nameLower uniqueness constraint, the
// first migration with actual schema content, lands in step 9.
RETURN 1;
