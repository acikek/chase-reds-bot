package com.acikek.crbot.core;

import java.util.Collections;
import java.util.List;

public class Target {

    public enum Type {
        POSITION,
        CARDS
    }

    public Position position = null;
    public List<Integer> powers = Collections.emptyList();
    public Type type;

    public Target(Position position) {
        this.position = position;
        type = Type.POSITION;
    }

    public Target(List<Integer> powers) {
        this.powers = powers;
        type = Type.CARDS;
    }

    public Target(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        if (type == Type.POSITION) {
            return position.toString();
        }
        List<String> characters = powers.stream()
                .map(Card::getCharacter)
                .toList();
        return String.join("", characters);
    }

    public static Target parse(String string) {
        if (Character.isDigit(string.charAt(0))) {
            return new Target(Position.fromRank(Integer.parseInt(string)));
        }
        List<Integer> powers = string.chars().mapToObj(i -> (char) i)
                .map(c -> Card.getPower(String.valueOf(c)))
                .toList();
        return new Target(powers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Target target) {
            return position == target.position && powers.equals(target.powers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = position != null ? position.hashCode() : 0;
        result = 31 * result + (powers != null ? powers.hashCode() : 0);
        result = 31 * result + type.hashCode();
        return result;
    }
}
