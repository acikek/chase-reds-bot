package com.acikek.crbot.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActionSet {

    public Card card;
    public List<Action> actions = new ArrayList<>();

    public ActionSet(Card card) {
        this.card = card;
    }

    public void addAction(Action.Type type, Target target) {
        actions.add(new Action(card, type, target));
    }

    @Override
    public String toString() {
        List<String> actionStrings = actions.stream()
                .map(Action::toString)
                .toList();
        return (card != null ? card.toString() : "") + String.join("", actionStrings);
    }

    public static ActionSet parse(String string) {
        String[] components = Action.getActionComponents(string);
        Card card = Card.parse(components[0]);
        List<Action> actions = Arrays.stream(components)
                .skip(1)
                .map(s -> Action.parse(card, s))
                .toList();
        ActionSet set = new ActionSet(card);
        set.actions.addAll(actions);
        return set;
    }
}
