package com.src.utils;

public enum Command {
    INITIALIZE("INITIALIZE"),
    MAPPING("MAPPING"),
    SHUFFLING("SHUFFLING"),
    SORT_MAPPING("SORT_MAPPING"),
    SORT_SHUFFLING("SORT_SHUFFLING"),
    END("END"),
    OK("OK"),
    QUIT("QUIT");

    private final String label;

    private Command(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }
}
