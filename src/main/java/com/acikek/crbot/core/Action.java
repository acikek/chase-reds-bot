package com.acikek.crbot.core;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class Action implements Comparable<Action> {

    public enum Type {

        SETUP(null, Target.Type.CARDS, true),
        PLACE('p', Target.Type.POSITION, false),
        MOVE('m', Target.Type.POSITION, false),
        SERVE('s', Target.Type.POSITION, false),
        ATTACK('x', Target.Type.POSITION, true),
        CHASE('c', Target.Type.CARDS, true),
        PASS('-', null, true);

        public final Character character;
        public final Target.Type targetType;
        public final boolean endsTurn;

        Type(Character character, Target.Type targetType, boolean endsTurn) {
            this.character = character;
            this.targetType = targetType;
            this.endsTurn = endsTurn;
        }

        public static Type fromCharacter(char c) {
            return switch (c) {
                case 'p' -> PLACE;
                case 'm' -> MOVE;
                case 's' -> SERVE;
                case 'x' -> ATTACK;
                case 'c' -> CHASE;
                case '-' -> PASS;
                default -> null;
            };
        }

        public boolean denoteTarget(Card card, Target target) {
            if (this.targetType != target.type) {
                return false;
            }
            if (target.type == Target.Type.CARDS) {
                return true;
            }
            return switch (this) {
                case MOVE, SERVE -> true;
                case ATTACK -> !card.isAttackingForward(target.position);
                default -> false;
            };
        }

        public Target getImpliedTarget(Card card) {
            return switch (this) {
                case PLACE -> new Target(card.position);
                case ATTACK -> new Target(card.position.relative());
                default -> null;
            };
        }
    }

    public static final Action PASS = new Action(null, Type.PASS, null);

    public Card card;
    public Type type;
    public Target target;

    public Action(Card card, Type type, Target target) {
        this.card = card;
        this.type = type;
        this.target = target;
    }

    public static List<Action> filterByType(List<Action> actions, List<Type> types) {
        return actions.stream()
                .filter(action -> types.contains(action.type))
                .toList();
    }

    @Override
    public String toString() {
        String targetString = target != null && (card == null || type.denoteTarget(card, target)) ? target.toString() : "";
        return (type.character != null ? type.character : "") + targetString;
    }

    public String toTurnString() {
        Turn turn = new Turn();
        turn.addAction(this);
        return turn.toString();
    }

    public static String[] getActionComponents(String string) {
        return string.split("(?=[pmsxc])");
    }

    public static Action parse(Card card, String string) {
        Type type = Type.fromCharacter(string.charAt(0));
        Target target = string.length() > 1
                ? Target.parse(string.substring(1))
                : type.getImpliedTarget(card);
        return new Action(card, type, target);
    }

    public static Action parseTurnString(String turnString) {
        return ActionSet.parse(turnString).actions.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Action action) {
            return Objects.equals(card, action.card) && type == action.type && Objects.equals(target, action.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = card != null ? card.hashCode() : 0;
        result = 31 * result + type.hashCode();
        result = 31 * result + (target != null ? target.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NotNull Action o) {
        return type.compareTo(o.type);
    }
}
