package com.acikek.crbot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Turn {

    public List<ActionSet> actionSets = new ArrayList<>();

    public void addAction(Card card, Action action) {
        if (actionSets.isEmpty() || !actionSets.get(actionSets.size() - 1).card.equals(card)) {
            ActionSet set = new ActionSet(card);
            set.actions.add(action);
            actionSets.add(set);
        }
        else {
            ActionSet last = actionSets.get(actionSets.size() - 1);
            last.actions.add(action);
        }
    }

    public void addAction(Card card, Action.Type type, Target target) {
        addAction(card, new Action(card, type, target));
    }

    public void addAction(Action action) {
        addAction(action.card, action);
    }

    public void addAction(Action.Type type, Target target) {
        addAction(null, type, target);
    }

    @Override
    public String toString() {
        List<String> setStrings = actionSets.stream()
                .map(ActionSet::toString)
                .toList();
        return String.join(";", setStrings);
    }

    public static Turn parseSetupTurn(String string) {
        List<Integer> powers = string.replace("0", "")
                .chars()
                .mapToObj(i -> String.valueOf((char) i))
                .map(c -> c.equals("1") ? "10" : c)
                .map(Card::getPower)
                .toList();
        Turn turn = new Turn();
        turn.addAction(Action.Type.SETUP, new Target(powers));
        return turn;
    }

    public static Turn parse(String string) {
        if (string.matches("[\\dJQKA]+")) {
            return parseSetupTurn(string);
        }
        List<ActionSet> sets = Arrays.stream(string.split(";"))
                .map(ActionSet::parse)
                .toList();
        Turn turn = new Turn();
        turn.actionSets.addAll(sets);
        return turn;
    }
}
