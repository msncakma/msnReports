package dev.msntech.msnreports.models;

public enum ReportStatus {
    OPEN("Open", "§a"),
    IN_PROGRESS("In Progress", "§e"),
    RESOLVED("Resolved", "§2"),
    REJECTED("Rejected", "§c");

    private final String display;
    private final String color;

    ReportStatus(String display, String color) {
        this.display = display;
        this.color = color;
    }

    public String getDisplay() {
        return color + display;
    }

    public static ReportStatus fromString(String status) {
        try {
            return valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OPEN;
        }
    }
}