package com.acikek.crbot.core;

import java.util.*;

public class Army  {

    public Map<Position, Card> board = new EnumMap<>(Position.class);

    public Army() {
        clear();
    }

    public void clear(Card.Type type) {
        for (Position position : Position.values()) {
            if (type == null || board.get(position).type == type) {
                board.put(position, position.empty());
            }
        }
    }

    public void clear() {
        clear(null);
    }

    public Card addCard(int power, Position position) {
        Card card = new Card(power, position);
        board.put(position, card);
        return card;
    }

    public void loadPowers(List<Integer> powers) {
        for (int i = 0; i < Position.values().length; i++) {
            addCard(powers.get(i), Position.values()[i]);
        }
    }

    public boolean isFallbackOpen() {
        return board.get(Position.CENTER).type == Card.Type.EMPTY;
    }

    public List<Card> all(Position[] positions) {
        return Arrays.stream(positions)
                .map(pos -> board.get(pos))
                .toList();
    }

    public List<Card> all() {
        return all(Position.values());
    }

    public boolean hasAce() {
        for (Card card : all(Position.FRONT)) {
            if (card.type == Card.Type.ACE) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        for (Card card : all()) {
            if (card.type != Card.Type.EMPTY) {
                return false;
            }
        }
        return true;
    }

    public List<String> getBoardPowerStrings() {
        return board.values().stream()
                .map(Card::toPowerString)
                .toList();
    }

    @Override
    public String toString() {
        return String.join("", getBoardPowerStrings());
    }
}
