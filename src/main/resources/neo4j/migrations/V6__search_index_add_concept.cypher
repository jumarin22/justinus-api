// Add Concept (name, description) to the search index. A fulltext
// index's labels and properties can't be altered in place, so drop and
// recreate it; the index repopulates from existing nodes on creation.
DROP INDEX search_index IF EXISTS;

CREATE FULLTEXT INDEX search_index IF NOT EXISTS
FOR (n:Note|Source|Concept)
ON EACH [n.content, n.title, n.author, n.generalNotes, n.name, n.description];
