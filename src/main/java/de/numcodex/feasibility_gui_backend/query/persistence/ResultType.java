package de.numcodex.feasibility_gui_backend.query.persistence;

public enum ResultType {
    SUCCESS("S"),
    ERROR("E");

    private final String shortcode;

    ResultType(String shortcode) {
        this.shortcode = shortcode;
    }

    public String getShortcode() {
        return shortcode;
    }

    public static ResultType fromShortcode(String shortcode) {
        return switch (shortcode) {
            case "S" -> ResultType.SUCCESS;
            case "E" -> ResultType.ERROR;
            default -> throw new IllegalArgumentException("No ResultType with shortcode " + shortcode + " found.");
        };
    }
}
