package com.acikek.crbot.core;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.*;

public class Card {

    public enum Type {

        EMPTY,
        PAWN,
        MINOR,
        MAJOR,
        JACK,
        QUEEN,
        KING,
        ACE;

        public static final Map<Integer, Type> TABLE = new HashMap<>();

        static {
            TABLE.put(0, ACE);
            TABLE.put(11, JACK);
            TABLE.put(12, QUEEN);
            TABLE.put(13, KING);
        }

        public static Type fromPower(int power) {
            if (power == -1) {
                return EMPTY;
            }
            if (power >= 2 && power <= 4) {
                return PAWN;
            }
            if (power >= 5 && power <= 7) {
                return MINOR;
            }
            if (power >= 8 && power <= 10) {
                return MAJOR;
            }
            return TABLE.get(power);
        }
    }

    public int power;
    public int servant = 0;
    public Type type;
    public Position position;

    public Card(int power, Position position) {
        this.power = power;
        type = Type.fromPower(power);
        this.position = position;
    }

    public Card(String string, Position position) {
        this(getPower(string), position);
    }

    public Card(Card other) {
        power = other.power;
        servant = other.servant;
        type = other.type;
        position = other.position;
    }

    public static final BidiMap<String, Integer> ROYALTY_CHAR = new DualHashBidiMap<>();

    static {
        ROYALTY_CHAR.put("A", 0);
        ROYALTY_CHAR.put("J", 11);
        ROYALTY_CHAR.put("Q", 12);
        ROYALTY_CHAR.put("K", 13);
    }

    public static int getPower(String string) {
        if (ROYALTY_CHAR.containsKey(string)) {
            return ROYALTY_CHAR.get(string);
        }
        return Integer.parseInt(string);
    }

    public static String getCharacter(int power) {
        if (power >= 2 && power <= 10) {
            return String.valueOf(power);
        }
        String c = ROYALTY_CHAR.inverseBidiMap().get(power);
        return c != null ? c : " ";
    }

    public int totalPower() {
        return power + servant;
    }

    public int placePower() {
        return servant > 0 ? servant : power;
    }

    public boolean canRangeAttack(boolean ace) {
        return !ace && type == Type.MINOR && position == Position.FALLBACK;
    }

    public boolean canAttack(boolean open, boolean ace) {
        if (canRangeAttack(ace)) {
            return true;
        }
        return open;
    }

    public List<Position> getQueenAttackPositions() {
        List<Position> positions = new ArrayList<>(List.of(Position.LEFT, Position.CENTER, Position.RIGHT));
        if (!position.isCenterFile()) {
            positions.remove(position.relative());
        }
        return positions;
    }

    public List<Position> getPossibleAttackPositions(boolean open, boolean otherFallbackOpen, boolean ace) {
        if (!canAttack(open, ace)) {
            return Collections.emptyList();
        }
        if (canRangeAttack(ace)) {
            return Arrays.asList(Position.values());
        }
        List<Position> positions = new ArrayList<>();
        if (!ace && type == Type.QUEEN) {
            positions.addAll(getQueenAttackPositions());
        }
        else {
            positions.add(position.forward(open));
        }
        return positions.stream()
                .map(pos -> pos.relative().back(otherFallbackOpen))
                .toList();
    }

    public boolean canDestroy(int power, boolean ace) {
        return (!ace && type == Type.JACK) || totalPower() >= power;
    }

    public boolean shouldDiscard(int power, boolean ace) {
        if (canRangeAttack(ace)) {
            return false;
        }
        if (servant > 0) {
            return true;
        }
        return this.power <= power;
    }

    public boolean isAttackingForward(Position targetPosition) {
        if (type == Type.MINOR && position == Position.FALLBACK) {
            return false;
        }
        return position.isSameFile(targetPosition);
    }

    public boolean canHaveServant(Type type) {
        return this.type == Card.Type.MAJOR && servant <= 0 && type == Card.Type.PAWN;
    }

    public Card getPlacedCard(int power) {
        Card card = new Card(this.power, this.position);
        if (this.type == Type.EMPTY) {
            card.power = power;
            return card;
        }
        card.servant = power;
        return card;
    }

    public List<Position> getMovePositions(boolean ace) {
        if (!ace && type == Type.MAJOR) {
            List<Position> positions = new ArrayList<>(List.of(Position.values()));
            positions.remove(position);
            return positions;
        }
        return position.adjacent();
    }

    public String toPowerString() {
        String base = getCharacter(power);
        if (servant > 0) {
            return base + "+" + servant;
        }
        return base;
    }

    @Override
    public String toString() {
        String powerString = toPowerString();
        return powerString + ":" + position;
    }

    public static Card parse(String string) {
        String[] components = string.split(":");
        String[] powerComponents = components[0].split("\\+");
        Card card = new Card(getPower(powerComponents[0]), Position.fromRank(Integer.parseInt(components[1])));
        if (powerComponents.length > 1) {
            card.servant = Integer.parseInt(powerComponents[1]);
        }
        return card;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Card card) {
            return power == card.power && servant == card.servant && type == card.type && position == card.position;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = power;
        result = 31 * result + servant;
        result = 31 * result + type.hashCode();
        result = 31 * result + position.hashCode();
        return result;
    }
}
