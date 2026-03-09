package com.example.releasethekraken;

public enum UserRole {
    ENTRANT("Entrant"),
    ORGANIZER("Organizer"),
    ADMIN("Admin");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
