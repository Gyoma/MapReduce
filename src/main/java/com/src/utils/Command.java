package com.src.utils;

public enum Command {
    INITIALIZE("INITIALIZE"),
    MAPPING("MAPPING"),
    SHUFFLING("SHUFFLING"),
    REDUCING("REDUCING"),
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
