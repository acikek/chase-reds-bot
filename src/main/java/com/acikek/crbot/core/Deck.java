package com.acikek.crbot.core;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Deck {

    public static final List<Integer> POWERS = List.of(
            0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13
    );

    public static final Map<Integer, Double> BUILDUP_MAP = Map.of(
            0, .1,
            11, .2,
            12, .25,
            13, .5
    );

    public List<Integer> cards = new ArrayList<>();
    public List<Integer> hand = new ArrayList<>();

    public Deck(Random random, boolean buildup) {
        System.out.println(buildup);
        cards.addAll(POWERS);
        cards.addAll(POWERS);
        Collections.shuffle(cards, random);
        System.out.println(cards);
        if (buildup) {
            for (int i = 0; i < cards.size() / 2; i++) {
                int power = cards.get(i);
                if (!BUILDUP_MAP.containsKey(power)) {
                    continue;
                }
                double area = BUILDUP_MAP.get(power);
                System.out.println(power);
                System.out.println(area);
                System.out.println((double) i / cards.size());
                if ((double) i / cards.size() <= area) {
                    cards.remove(i);
                    int index = ThreadLocalRandom.current().nextInt((int) (cards.size() * area), cards.size());
                    cards.add(index, power);
                    i--;
                }
            }
        }
        System.out.println(cards);
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
