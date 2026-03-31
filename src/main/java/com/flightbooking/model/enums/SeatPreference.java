package com.flightbooking.model.enums;

import java.util.Set;

public enum SeatPreference {
    WINDOW(Set.of('A', 'F')),
    MIDDLE(Set.of('B', 'E')),
    AISLE(Set.of('C', 'D'));

    private final Set<Character> columns;

    SeatPreference(Set<Character> columns) {
        this.columns = columns;
    }

    public boolean matches(String seatLabel) {
        char col = seatLabel.charAt(seatLabel.length() - 1);
        return columns.contains(col);
    }
}
