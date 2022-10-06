package com.acikek.crbot.command;

import com.acikek.crbot.core.Card;
import com.acikek.crbot.core.Deck;
import com.acikek.crbot.core.Player;
import com.acikek.crbot.core.Position;
import com.acikek.crbot.game.GameData;
import com.acikek.crbot.game.GameHandler;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreativeCommands {

    public static final CommandData SET_COMMAND_DATA = Commands.slash("set", "Set a card at a position in creative mode")
            .addOptions(
                    addPowerChoices(new OptionData(OptionType.INTEGER, "power", "The card's base power", true), Deck.POWERS, true),
                    new OptionData(OptionType.INTEGER, "position", "The card's position", true)
                            .addChoice("Left", 1)
                            .addChoice("Center", 2)
                            .addChoice("Right", 3)
                            .addChoice("Fallback", 4),
                    addPowerChoices(new OptionData(OptionType.INTEGER, "servant", "The card's servant addon power", false), List.of(2, 3, 4), false),
                    addArmyChoices(new OptionData(OptionType.STRING, "army", "The army to set to", false), false)
            );

    public static final CommandData CLEAR_COMMAND_DATA = Commands.slash("clear", "Clear an army in creative mode")
            .addOptions(addArmyChoices(new OptionData(OptionType.STRING, "army", "The army to clear", true), true));

    public static OptionData addPowerChoices(OptionData option, List<Integer> powers, boolean empty) {
        for (int power : powers) {
            option.addChoice(Card.getCharacter(power), power);
        }
        if (empty) {
            option.addChoice("Empty", -1);
        }
        return option;
    }

    public static OptionData addArmyChoices(OptionData option, boolean both) {
        option.addChoice("Black", "black")
                .addChoice("Red", "red");
        if (both) {
            option.addChoice("Both", "both");
        }
        return option;
    }

    public static Player.Type[] resolveArmyType(String value) {
        return switch (value) {
            case "black" -> new Player.Type[] { Player.Type.BLACK };
            case "red" -> new Player.Type[] { Player.Type.RED };
            case "both" -> new Player.Type[] { Player.Type.BLACK, Player.Type.RED };
            default -> null;
        };
    }

    public static final ListenerAdapter SET_COMMAND = new ListenerAdapter() {

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("set")) {
                return;
            }
            GameData data = GameCommands.checkGame(event, event.getUser());
            if (data == null || !data.checkCreative(event)) {
                return;
            }
            Integer power = event.getOption("power", OptionMapping::getAsInt);
            Integer servant = event.getOption("servant", OptionMapping::getAsInt);
            Integer rank = event.getOption("position", OptionMapping::getAsInt);
            String army = event.getOption("army", OptionMapping::getAsString);
            if (power == null || rank == null) {
                return;
            }
            Position position = Position.fromRank(rank);
            Card card = new Card(power, position);
            if (servant != null) {
                if (card.type != Card.Type.MAJOR) {
                    event.reply("Only Major cards can have servants.").setEphemeral(true).queue();
                    return;
                }
                card.servant = servant;
            }
            Player.Type player = army == null ? Player.Type.BLACK : resolveArmyType(army)[0];
            data.game.getPlayer(player).army.board.put(position, card);
            GameHandler.refreshBoard(event, data);
        }
    };

    public static final ListenerAdapter CLEAR_COMMAND = new ListenerAdapter() {

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("clear")) {
                return;
            }
            GameData data = GameCommands.checkGame(event, event.getUser());
            if (data == null || !data.checkCreative(event)) {
                return;
            }
            String army = event.getOption("army", OptionMapping::getAsString);
            if (army == null) {
                return;
            }
            for (Player.Type player : resolveArmyType(army)) {
                data.game.getPlayer(player).army.clear();
            }
            GameHandler.refreshBoard(event, data);
        }
    };
}
