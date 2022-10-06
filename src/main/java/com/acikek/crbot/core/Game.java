package com.acikek.crbot.core;

import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game {

    public Random random = new Random();

    public Player black = new Player(random);
    public Player red = new Player(random);

    public Player.Type currentPlayer = Player.Type.SETUP;
    public Turn currentTurn;
    public Action lastAction;
    public boolean hasMoved;
    public boolean hasPlacedKing;
    public boolean lastPlayerCouldAct = true;
    public int passes;
    public boolean turnEnded;

    public List<Turn> turns = new ArrayList<>();

    public Game(List<Turn> turns) {
        if (turns == null) {
            return;
        }
        for (Turn turn : turns) {
            for (ActionSet set : turn.actionSets) {
                for (Action action : set.actions) {
                    submitAction(action);
                }
            }
            endTurn();
        }
    }

    public static List<String> getTurnLines(String fileData) {
        return fileData.lines()
                .map(line -> line.replaceAll("#.*", ""))
                .map(String::trim)
                .filter(line -> line.length() > 0 && !line.startsWith("@") && !line.startsWith("#"))
                .toList();
    }

    public Action getSetupAction() {
        List<Integer> powers = black.deck.draw(4, false);
        powers.addAll(red.deck.draw(4, false));
        return new Action(null, Action.Type.SETUP, new Target(powers));
    }

    public Player getPlayer(Player.Type player) {
        if (player == Player.Type.SETUP) {
            return null;
        }
        return player == Player.Type.BLACK ? black : red;
    }

    public List<Action> getAvailableAttackActions(Card card, Army current, Army other, boolean ace) {
        List<Action> result = new ArrayList<>();
        boolean open = card.position != Position.FALLBACK || current.isFallbackOpen();
        List<Position> attackPositions = card.getPossibleAttackPositions(open, other.isFallbackOpen(), ace);
        for (Position position : attackPositions) {
            Card opponent = other.board.get(position);
            if (opponent.type != Card.Type.EMPTY && card.canDestroy(opponent.totalPower(), ace)) {
                Action action = new Action(card, Action.Type.ATTACK, new Target(position));
                result.add(action);
            }
        }
        return result;
    }

    public List<Action> getAvailablePlaceActions(Card card, Deck current, boolean ace) {
        List<Action> result = new ArrayList<>();
        for (int power : current.hand) {
            Card.Type type = Card.Type.fromPower(power);
            if (type == Card.Type.KING && hasPlacedKing) {
                continue;
            }
            if (card.type == Card.Type.EMPTY || (!ace && card.canHaveServant(type))) {
                Card placingCard = card.getPlacedCard(power);
                Action action = new Action(placingCard, Action.Type.PLACE, new Target(card.position));
                result.add(action);
            }
        }
        return result;
    }

    public List<Action> getAvailableMoveActions(Card card, Army current, boolean ace) {
        List<Action> result = new ArrayList<>();
        List<Position> positions = card.getMovePositions(ace);
        for (Position position : positions) {
            Card existing = current.board.get(position);
            if (existing.type != Card.Type.KING) {
                Action action = new Action(card, Action.Type.MOVE, new Target(position));
                result.add(action);
            }
            if (existing.type == Card.Type.MAJOR && existing.servant <= 0 && card.type == Card.Type.PAWN) {
                Action action = new Action(card, Action.Type.SERVE, new Target(position));
                result.add(action);
            }
        }
        return result;
    }

    public Action getAvailableChaseAction(Card card, Army current) {
        for (Position position : card.position.adjacent()) {
            if (current.board.get(position).type == Card.Type.KING) {
                return new Action(card, Action.Type.CHASE, new Target(Target.Type.CARDS));
            }
        }
        return null;
    }

    public List<Action> getAvailableActions() {
        Player current = getPlayer(currentPlayer);
        Player other = getPlayer(currentPlayer.next());
        List<Action> result = new ArrayList<>();
        boolean ace = other.army.hasAce();
        for (Card card : current.army.all()) {
            result.addAll(getAvailablePlaceActions(card, current.deck, ace));
            if (card.type != Card.Type.EMPTY) {
                if (!hasMoved) {
                    if (card.type != Card.Type.KING) {
                        result.addAll(getAvailableMoveActions(card, current.army, ace));
                    }
                    else if (!ace) {
                        Action chase = getAvailableChaseAction(card, current.army);
                        if (chase != null) {
                            result.add(chase);
                        }
                    }
                }
                result.addAll(getAvailableAttackActions(card, current.army, other.army, ace));
            }
        }
        if (currentTurn.actionSets.isEmpty() && passes < 2 && lastPlayerCouldAct) {
            result.add(Action.PASS);
        }
        return result;
    }

    public enum ActionResult {
        CONTINUE,
        END_TURN,
        WIN,
        LOSE,
        STALEMATE
    }

    public ActionResult submitAction(Action action) {
        Player current = getPlayer(currentPlayer);
        Player other = getPlayer(currentPlayer.next());
        switch (action.type) {
            case SETUP -> {
                List<List<Integer>> powers = ListUtils.partition(action.target.powers, 4);
                black.army.loadPowers(powers.get(0));
                red.army.loadPowers(powers.get(1));
                for (Integer power : powers.get(0)) {
                    black.deck.cards.remove(power);
                }
                for (Integer power : powers.get(1)) {
                    red.deck.cards.remove(power);
                }
                black.deck.initHand();
                red.deck.initHand();
                return ActionResult.CONTINUE;
            }
            case PLACE -> {
                current.army.board.put(action.target.position, action.card);
                if (action.card.type == Card.Type.KING) {
                    hasPlacedKing = true;
                }
                Integer placePower = action.card.placePower();
                if (!current.deck.hand.remove(placePower)) {
                    current.deck.cards.remove(placePower);
                }
            }
            case MOVE -> {
                Card existing = current.army.board.get(action.target.position);
                Card newExisting = new Card(existing);
                Card newActing = new Card(action.card);
                current.army.board.put(action.target.position, newActing);
                current.army.board.put(action.card.position, newExisting);
                newExisting.position = action.card.position;
                newActing.position = action.target.position;
                hasMoved = true;
            }
            case SERVE -> {
                Card existing = current.army.board.get(action.target.position);
                existing.servant = action.card.power;
                current.army.board.put(action.card.position, action.card.position.empty());
                hasMoved = true;
            }
            case ATTACK -> {
                Position attackingPos = action.target.position.forward(current.army.isFallbackOpen()).back(other.army.isFallbackOpen());
                Card attacking = other.army.board.get(attackingPos);
                other.army.board.put(attackingPos, attackingPos.empty());
                if (action.card.shouldDiscard(attacking.totalPower(), other.army.hasAce())) {
                    current.army.board.put(action.card.position, action.card.position.empty());
                }
            }
            case CHASE -> {
                current.army.clear(Card.Type.KING);
                action.target = new Target(other.deck.draw(4));
                other.army.clear();
                other.army.loadPowers(action.target.powers);
                other.deck.hand = other.deck.draw(3);
            }
        }
        if (passes > 0) {
            passes = 0;
        }
        currentTurn.addAction(action);
        lastAction = action;
        turnEnded = action.type.endsTurn;
        boolean currentEmpty = current.isEmpty();
        boolean otherEmpty = other.isEmpty();
        if (currentEmpty || otherEmpty) {
            return otherEmpty && !currentEmpty
                    ? ActionResult.WIN
                    : !otherEmpty
                            ? ActionResult.LOSE
                            : ActionResult.STALEMATE;
        }
        return turnEnded ? ActionResult.END_TURN : ActionResult.CONTINUE;
    }

    public Player.Type getWinningPlayer(ActionResult result) {
        return result == ActionResult.WIN
                ? currentPlayer
                : currentPlayer.next();
    }

    public boolean canDraw() {
        Player player = getPlayer(currentPlayer);
        return currentTurn.actionSets.isEmpty() && player.deck.hand.size() < 3 && !player.deck.cards.isEmpty();
    }

    public void cycleTurn() {
        currentPlayer = currentPlayer.next();
        currentTurn = new Turn();
        hasMoved = false;
        hasPlacedKing = false;
        turnEnded = false;
    }

    public void endTurn() {
        turns.add(currentTurn);
        cycleTurn();
    }

    public void pass(boolean couldAct) {
        lastPlayerCouldAct = couldAct;
        currentTurn.addAction(Action.PASS);
        lastAction = Action.PASS;
        passes++;
        endTurn();
    }

    public void begin() {
        if (turns.isEmpty()) {
            currentTurn = new Turn();
            Action action = getSetupAction();
            submitAction(action);
            currentTurn.addAction(action);
            endTurn();
        }
    }

    public String getFileData(Map<String, String> headers) {
        List<String> headerStrings = headers.entrySet().stream()
                .map(pair -> "@" + pair.getKey() + " " + pair.getValue())
                .toList();
        String lines = String.join("\n", turns.stream().map(Turn::toString).toList());
        return String.join("\n", headerStrings) + "\n\n" + lines;
    }
}
