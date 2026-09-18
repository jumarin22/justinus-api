// Concept.nameLower uniqueness, same reasoning as V2 for Tag: native
// constraints can't target lower(name), so a maintained nameLower
// property carries the constraint.
CREATE CONSTRAINT concept_name_lower_unique IF NOT EXISTS
FOR (c:Concept)
REQUIRE c.nameLower IS UNIQUE;
