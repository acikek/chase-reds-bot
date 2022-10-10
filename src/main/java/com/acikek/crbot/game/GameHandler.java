package com.acikek.crbot.game;

import com.acikek.crbot.ChaseRedsBot;
import com.acikek.crbot.core.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class GameHandler extends ListenerAdapter {

    public static final Button END_TURN = Button.danger("game_end", "End Turn");
    public static final Button DRAW = Button.secondary("game_draw", "Draw");
    public static final Button CANCEL = Button.danger("game_cancel", "Cancel");

    public static Button getTypeButton(Action action) {
        return switch (action.type) {
            case PLACE -> Button.secondary("game_menu_place", "Place...");
            case MOVE -> Button.primary("game_menu_move", "Move...");
            case ATTACK -> Button.success("game_menu_attack", "Attack...");
            case CHASE -> Button.danger("game_action_" + action.toTurnString(), "Chase!");
            case PASS -> Button.danger("game_pass", "Pass");
            default -> null;
        };
    }

    public static String getPositionName(Position position) {
        return switch (position) {
            case LEFT -> "Left";
            case CENTER -> "Center";
            case RIGHT -> "Right";
            case FALLBACK -> "Fallback";
        };
    }

    public static String getRoyalName(String character) {
        return switch (character) {
            case "J" -> "Jack";
            case "Q" -> "Queen";
            case "K" -> "King";
            case "A" -> "Ace";
            default -> character;
        };
    }

    public static String cardToReadableString(Card card) {
        String powerString = getRoyalName(Card.getCharacter(card.power)) + (card.servant > 0 ? " with " + card.servant : "");
        return powerString + " at " + getPositionName(card.position);
    }

    public static void addButtons(MessageRequest<?> reply, List<Button> buttons, boolean cancel) {
        List<Button> newButtons = new ArrayList<>(buttons);
        if (cancel) {
            newButtons.add(CANCEL);
        }
        List<ActionRow> rows = ListUtils.partition(newButtons, 5).stream()
                .map(ActionRow::of)
                .toList();
        reply.setComponents(rows);
    }

    public static void addMenuButtons(MessageRequest<?> reply, Game game) {
        List<Action> availableActions = game.getAvailableActions();
        Set<Action.Type> set = new HashSet<>();
        List<Button> buttons = new ArrayList<>();
        if (game.canDraw()) {
            buttons.add(DRAW);
        }
        List<Button> typeButtons = availableActions.stream()
                .sorted()
                .filter(action -> action.type != Action.Type.SERVE)
                .filter(action -> set.add(action.type))
                .map(GameHandler::getTypeButton)
                .toList();
        buttons.addAll(typeButtons);
        if (!game.currentTurn.actionSets.isEmpty()) {
            buttons.add(END_TURN);
        }
        addButtons(reply, buttons, false);
    }

    public static List<Button> getStringButtons(String prefix, List<String> strings) {
        return strings.stream()
                .map(s -> Button.primary(prefix + "_" + s, s))
                .toList();
    }

    public static void addCardButtons(ReplyCallbackAction reply, String prefix, List<Card> cards) {
        List<Button> buttons = cards.stream()
                .map(card -> Button.primary(prefix + "_" + card.position, cardToReadableString(card)))
                .toList();
        addButtons(reply, buttons, true);
    }

    public static Button getActionButton(Action action, Function<Action, String> transform) {
        return Button.primary("game_action_" + action.toTurnString(), transform.apply(action));
    }

    public static void addActionButtons(ReplyCallbackAction reply, List<Action> actions, Function<Action, String> transform) {
        List<Button> buttons = actions.stream()
                .map(action -> getActionButton(action, transform))
                .toList();
        addButtons(reply, buttons, true);
    }

    public static void refreshBoard(IReplyCallback event, GameData data) {
        event.deferReply().queue();
        var reply = event.getHook().editOriginalAttachments(data.getBoard());
        addMenuButtons(reply, data.game);
        data.currentBoard = reply.complete();
        data.inMenu = false;
        data.pauseValid = false;
    }

    public void handlePlaceMenu(ButtonInteractionEvent event, String[] args, Game game, List<Action> actions) {
        if (args.length >= 3) {
            int power = Card.getPower(args[2]);
            List<Action> placeActions = actions.stream()
                    .distinct()
                    .filter(action -> action.card.placePower() == power)
                    .toList();
            var reply = event.reply("Where do you want to place it?");
            addActionButtons(reply, placeActions, action -> getPositionName(action.target.position));
            reply = reply.setEphemeral(true);
            reply.complete();
            return;
        }
        List<String> hand = game.getPlayer(game.currentPlayer).deck.hand.stream()
                .distinct()
                .filter(power -> actions.stream().anyMatch(action -> action.card.placePower() == power))
                .map(Card::getCharacter)
                .map(GameHandler::getRoyalName)
                .toList();
        var reply = event.reply("Choose a card from your hand.");
        reply = reply.setEphemeral(true);
        List<Button> buttons = new ArrayList<>(getStringButtons("game_menu_place", hand));
        addButtons(reply, buttons, true);
        reply.complete();
    }

    public void replyCardChoice(ButtonInteractionEvent event, List<Action> actions, String menu, String message) {
        List<Card> cards = actions.stream()
                .map(action -> action.card)
                .distinct()
                .toList();
        var reply = event.reply(message);
        reply = reply.setEphemeral(true);
        addCardButtons(reply, "game_menu_" + menu, cards);
        reply.complete();
    }

    public List<Action> getCardActions(String rankArg, String targetArg, Game game, List<Action> actions) {
        Position position = Position.fromRank(Integer.parseInt(rankArg));
        Card card = game.getPlayer(game.currentPlayer).army.board.get(position);
        Stream<Action> stream = actions.stream()
                .filter(action -> action.card == card);
        if (targetArg != null) {
            Position targetPos = Position.fromRank(Integer.parseInt(targetArg));
            stream = stream.filter(action -> action.target.position == targetPos);
        }
        return stream.toList();
    }

    public void handleMoveMenu(ButtonInteractionEvent event, String[] args, Game game, List<Action> actions) {
        if (args.length >= 4) {
            List<Action> cardActions = getCardActions(args[2], args[3], game, actions);
            Action swap = Action.filterByType(cardActions, List.of(Action.Type.MOVE)).get(0);
            Action serve = Action.filterByType(cardActions, List.of(Action.Type.SERVE)).get(0);
            List<Button> buttons = new ArrayList<>();
            buttons.add(getActionButton(swap, a -> "Swap"));
            buttons.add(getActionButton(serve, a -> "Serve"));
            var reply = event.reply("Would you like to **Swap** or **Serve**?");
            addButtons(reply, buttons, true);
            reply.setEphemeral(true).complete();
            return;
        }
        if (args.length == 3) {
            List<Action> cardActions = getCardActions(args[2], null, game, actions);
            List<Action> moves = Action.filterByType(cardActions, List.of(Action.Type.MOVE));
            List<Position> servePositions = Action.filterByType(cardActions, List.of(Action.Type.SERVE)).stream()
                    .map(action -> action.target.position)
                    .toList();
            List<Button> buttons = moves.stream()
                    .map(move -> servePositions.contains(move.target.position)
                            ? Button.primary("game_menu_move_" + move.card.position + "_" + move.target.position, move.target.position.toString())
                            : getActionButton(move, action -> getPositionName(action.target.position)))
                    .toList();
            var reply = event.reply("Where do you want to move it?");
            addButtons(reply, buttons, true);
            reply.setEphemeral(true).complete();
            return;
        }
        replyCardChoice(event, actions, "move", "Choose a card to move.");
    }

    public void handleAttackMenu(ButtonInteractionEvent event, String[] args, Game game, List<Action> actions) {
        if (args.length >= 3) {
            Function<Action, String> transform = action -> cardToReadableString(game.getPlayer(game.currentPlayer.next()).army.board.get(action.target.position));
            List<Action> cardActions = getCardActions(args[2], null, game, actions);
            var reply = event.reply("Where do you want to attack?");
            addActionButtons(reply, cardActions, transform);
            reply.setEphemeral(true).complete();
            return;
        }
        replyCardChoice(event, actions, "attack", "Choose a card to attack with.");
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        if (id == null) {
            return;
        }
        String[] args = id.split("_");
        if (!args[0].equals("game")) {
            return;
        }
        args = Arrays.copyOfRange(args, 1, args.length);
        GameData data = ChaseRedsBot.games.get(event.getUser());
        if (data == null) {
            event.reply("You don't belong to this game.").setEphemeral(true).queue();
            return;
        }
        if (!data.inMenu && data.currentBoard.getIdLong() != event.getMessageIdLong()) {
            event.reply("This action menu has expired.").setEphemeral(true).queue();
            return;
        }
        if (!data.checkTurn(event, event.getUser())) {
            return;
        }
        switch (args[0]) {
            case "cancel" -> {
                event.reply("Action cancelled.")
                        .setEphemeral(true)
                        .queue();
                data.setBoardDisabled(false);
                data.inMenu = false;
                return;
            }
            case "menu" -> {
                List<Action> availableActions = data.game.getAvailableActions();
                Action.Type type = Action.Type.valueOf(args[1].toUpperCase());
                List<Action.Type> types = new ArrayList<>(List.of(type));
                if (type == Action.Type.MOVE) {
                    types.add(Action.Type.SERVE);
                }
                List<Action> actions = Action.filterByType(availableActions, types);
                switch (type) {
                    case PLACE -> handlePlaceMenu(event, args, data.game, actions);
                    case MOVE -> handleMoveMenu(event, args, data.game, actions);
                    case ATTACK -> handleAttackMenu(event, args, data.game, actions);
                }
                data.setBoardDisabled(true);
                data.inMenu = true;
                return;
            }
            case "action" -> {
                Action action = Action.parseTurnString(args[1]);
                Game.ActionResult result = data.game.submitAction(action);
                if (result != Game.ActionResult.CONTINUE && result != Game.ActionResult.END_TURN) {
                    data.game.turns.add(data.game.currentTurn);
                    data.end(event, data.getPlayerData(data.game.getWinningPlayer(result)).user());
                    GameData.remove(data);
                    return;
                }
                if (result == Game.ActionResult.END_TURN || data.game.getAvailableActions().isEmpty()) {
                    data.game.endTurn();
                }
                else {
                    data.setBoardDisabled(false);
                }
            }
            case "draw" -> {
                Deck deck = data.game.getPlayer(data.game.currentPlayer).deck;
                deck.hand.add(deck.draw());
            }
            case "pass" -> data.game.pass(event.getMessage().getButtons().size() > 1);
            case "end" -> data.game.endTurn();
        }
        refreshBoard(event, data);
    }
}
