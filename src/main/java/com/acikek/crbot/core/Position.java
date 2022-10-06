package com.acikek.crbot.core;

import java.util.List;

public enum Position {

    LEFT(1),
    CENTER(2),
    RIGHT(3),
    FALLBACK(4);

    public static final Position[] FRONT = new Position[] {
            LEFT, CENTER, RIGHT
    };

    public final int number;

    Position(int number) {
        this.number = number;
    }

    public static Position fromRank(int rank) {
        return switch (rank) {
            case 1 -> LEFT;
            case 2 -> CENTER;
            case 3 -> RIGHT;
            case 4 -> FALLBACK;
            default -> null;
        };
    }

    public Card empty() {
        return new Card(-1, this);
    }

    public Position relative() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            default -> this;
        };
    }

    public Position back(boolean fallbackOpen) {
        return this == CENTER && fallbackOpen
                ? FALLBACK
                : this;
    }

    public Position forward(boolean fallbackOpen) {
        return this == FALLBACK && fallbackOpen
                ? CENTER
                : this;
    }

    public List<Position> adjacent() {
        return this == CENTER
                ? List.of(LEFT, RIGHT, FALLBACK)
                : List.of(CENTER);
    }

    public boolean isSameFile(Position other) {
        return back(true) == other.back(true).relative();
    }

    public boolean isCenterFile() {
        return switch (this) {
            case CENTER, FALLBACK -> true;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return String.valueOf(number);
    }
}
