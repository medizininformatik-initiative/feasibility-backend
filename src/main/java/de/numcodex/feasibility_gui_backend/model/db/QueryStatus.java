package de.numcodex.feasibility_gui_backend.model.db;

public enum QueryStatus {
    ACTIVE("A"),
    DELETED("D");

    private final String shortcode;

    QueryStatus(String shortCode) {
        this.shortcode = shortCode;
    }

    public String getShortcode() {
        return shortcode;
    }

    public static QueryStatus fromShortcode(String shortcode) {
        if ("D".equals(shortcode)) {
            return DELETED;
        }
        throw new IllegalArgumentException("No QueryStatus with shortcode " + shortcode + " found.");
    }
}
