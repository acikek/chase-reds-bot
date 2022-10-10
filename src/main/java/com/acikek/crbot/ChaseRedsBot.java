package com.acikek.crbot;

import com.acikek.crbot.command.CreativeCommands;
import com.acikek.crbot.command.GameCommands;
import com.acikek.crbot.command.GuideCommand;
import com.acikek.crbot.game.CardImages;
import com.acikek.crbot.game.GameData;
import com.acikek.crbot.game.GameHandler;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ChaseRedsBot extends ListenerAdapter implements EventListener {

    public static final Logger LOGGER = LoggerFactory.getLogger(ChaseRedsBot.class);
    public static final String TESTING_GUILD_ID = "992525569952587776";

    public static Guild testingGuild;
    public static Map<User, GameData> games = new HashMap<>();

    public static String playCommandMention;

    public static void main(String[] args) {
        LOGGER.info("Creating card images...");
        CardImages.blackCards = CardImages.getCardImages("b");
        CardImages.redCards = CardImages.getCardImages("r");
        LOGGER.info("Building bot...");
        JDABuilder.createDefault(args[0])
                .addEventListeners(new ChaseRedsBot(), new GameHandler(), new GuideCommand())
                .addEventListeners(
                        GameCommands.PLAY_COMMAND,
                        GameCommands.LEAVE_COMMAND,
                        GameCommands.PAUSE_COMMAND,
                        GameCommands.FREE_COMMAND,
                        GameCommands.REFRESH_COMMAND,
                        GameCommands.HAND_COMMAND,
                        CreativeCommands.SET_COMMAND,
                        CreativeCommands.CLEAR_COMMAND,
                        CreativeCommands.DECK_COMMAND
                )
                .build();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        testingGuild = event.getJDA().getGuildById(TESTING_GUILD_ID);
        if (testingGuild == null) {
            LOGGER.error("Testing guild '" + TESTING_GUILD_ID + "' not found!");
        }
        LOGGER.info("Updating all commands...");
        var commands = testingGuild.updateCommands().addCommands(
                GameCommands.PLAY_COMMAND_DATA,
                GameCommands.LEAVE_COMMAND_DATA,
                GameCommands.PAUSE_COMMAND_DATA,
                GameCommands.FREE_COMMAND_DATA,
                GameCommands.REFRESH_COMMAND_DATA,
                GameCommands.HAND_COMMAND_DATA,
                CreativeCommands.SET_COMMAND_DATA,
                CreativeCommands.CLEAR_COMMAND_DATA,
                CreativeCommands.DECK_COMMAND_DATA,
                GuideCommand.COMMAND_DATA
        ).complete();
        LOGGER.info("Retrieving 'play' command...");
        for (Command command : commands) {
            if (command.getName().equals("play")) {
                playCommandMention = "</play:" + command.getId() + ">";
                break;
            }
        }
        LOGGER.info("Ready!");
    }
}
