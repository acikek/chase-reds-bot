package com.acikek.crbot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {

    public static final List<Integer> POWERS = List.of(
            0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13
    );

    public List<Integer> cards = new ArrayList<>();
    public List<Integer> hand = new ArrayList<>();

    public Deck(Random random) {
        cards.addAll(POWERS);
        cards.addAll(POWERS);
        Collections.shuffle(cards, random);
    }

    public void initHand() {
        hand.addAll(draw(3));
    }

    public List<Integer> draw(int amount, boolean remove) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            result.add(remove ? cards.remove(0) : cards.get(i));
        }
        return result;
    }

    public List<Integer> draw(int amount) {
        return draw(amount, true);
    }

    public int draw() {
        return draw(1).get(0);
    }

    public boolean isEmpty() {
        return hand.isEmpty() && cards.isEmpty();
    }
}
