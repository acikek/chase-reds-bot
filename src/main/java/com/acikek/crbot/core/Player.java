package com.acikek.crbot.core;

import java.util.Random;

public class Player {

    public enum Type {

        SETUP,
        BLACK,
        RED;

        public Type next() {
            return switch (this) {
                case SETUP, RED -> BLACK;
                case BLACK -> RED;
            };
        }

        public static Type fromTurn(int turn) {
            if (turn == 0) {
                return SETUP;
            }
            return turn % 2 == 0 ? RED : BLACK;
        }

        @Override
        public String toString() {
            return switch (this) {
                case SETUP -> "Setup";
                case BLACK -> "Black";
                case RED -> "Red";
            };
        }
    }

    public Army army = new Army();
    public Deck deck;

    public Player(Random random) {
        deck = new Deck(random);
    }

    public boolean isEmpty() {
        return army.isEmpty() && deck.isEmpty();
    }
}
