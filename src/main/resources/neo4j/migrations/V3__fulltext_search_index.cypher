// GET /search: one full-text index spanning Note and Source, queried via
// db.index.fulltext.queryNodes. A multi-label index takes the union of
// properties; a node simply lacks the ones its label doesn't have.
CREATE FULLTEXT INDEX search_index IF NOT EXISTS
FOR (n:Note|Source)
ON EACH [n.content, n.title, n.author, n.generalNotes];
