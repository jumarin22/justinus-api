package com.justinus.api.domain;

/**
 * The node kinds a LINKS_TO relationship can connect. The label is a
 * closed set of constants (never user input), so it's safe to splice
 * into Cypher, which can't parameterize labels.
 */
public enum LinkableType {
    SOURCE("Source"),
    NOTE("Note"),
    CONCEPT("Concept");

    private final String label;

    LinkableType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static LinkableType fromLabels(Iterable<String> labels) {
        for (String label : labels) {
            for (LinkableType t : values()) {
                if (t.label.equals(label)) {
                    return t;
                }
            }
        }
        throw new IllegalStateException("Node has no linkable label: " + labels);
    }
}
