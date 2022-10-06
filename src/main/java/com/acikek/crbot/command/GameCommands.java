package com.acikek.crbot.command;

import com.acikek.crbot.ChaseRedsBot;
import com.acikek.crbot.core.Game;
import com.acikek.crbot.game.CardImages;
import com.acikek.crbot.game.GameData;
import com.acikek.crbot.game.GameHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class GameCommands {

    public static final CommandData PLAY_COMMAND_DATA = Commands.slash("play", "Play a game of Chase Reds")
            .addOption(OptionType.USER, "opponent", "Your opponent", false)
            .addOption(OptionType.ATTACHMENT, "resume", "The game file to resume", false)
            .addOption(OptionType.BOOLEAN, "creative", "Enable creative mode", false);


    public static final CommandData LEAVE_COMMAND_DATA = Commands.slash("leave", "Forfeit and leave the game");
    public static final CommandData PAUSE_COMMAND_DATA = Commands.slash("pause", "Pause the game and generate the resumable file");

    public static final CommandData FREE_COMMAND_DATA = Commands.slash("free", "Cancel the current action menu and free the board");
    public static final CommandData REFRESH_COMMAND_DATA = Commands.slash("refresh", "Refresh the board message");
    public static final CommandData HAND_COMMAND_DATA = Commands.slash("hand", "View your current hand");

    public static GameData checkGame(IReplyCallback event, User user) {
        GameData data = ChaseRedsBot.games.get(user);
        if (data == null) {
            event.reply("You're not part of a game.").setEphemeral(true).queue();
        }
        return data;
    }

    public static boolean checkPlaying(IReplyCallback event, User user) {
        if (ChaseRedsBot.games.containsKey(user)) {
            event.reply("You're already playing a game.").setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    public static String getAttachmentData(Message.Attachment attachment) {
        try {
            return new String(attachment.getProxy().download().get().readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            return null;
        }
    }

    public static final ListenerAdapter PLAY_COMMAND = new ListenerAdapter() {

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("play")) {
                return;
            }
            if (!checkPlaying(event, event.getUser())) {
                return;
            }
            User opponent = event.getOption("opponent", OptionMapping::getAsUser);
            if (opponent != null && ChaseRedsBot.games.containsKey(opponent)) {
                event.reply("This opponent is already playing a game.").setEphemeral(true).queue();
            }
            Message.Attachment resume = event.getOption("resume", OptionMapping::getAsAttachment);
            String resumeData = getAttachmentData(resume);
            boolean creative = event.getOption("creative", false, OptionMapping::getAsBoolean);
            boolean sameUser = opponent == null || opponent.getIdLong() == event.getUser().getIdLong();
            if (!creative && sameUser) {
                event.reply("You can't play this opponent.").setEphemeral(true).queue();
                return;
            }
            if (sameUser) {
                GameData.begin(event, event.getUser(), opponent, true, resumeData);
                return;
            }
            if (resumeData != null && !GameData.validateFile(event, Game.getTurnLines(resumeData))) {
                return;
            }
            String challenge = opponent.getAsMention() + ", **" + event.getUser().getName() + "** has challenged you to"
                    + (resumeData != null ? " **resume** " : "")
                    + "a game of **Chase Reds!**";
            // TODO: Add buildup mode
            // TODO: As more modes are added, make this dynamic
            String settings = creative ? "(Creative)" : "";
            String note = "> *This request is not stored. Ignore this message to deny the challenge.*";
            String buttonId = "accept_" + opponent.getId() + "_" + event.getUser().getId() + "_" + creative;
            String hash = resumeData != null ? "\n" + GameData.getFileHashString(resumeData) : "";
            var reply = event
                    .reply(challenge + " " + settings + hash +  "\n" + note)
                    .addActionRow(Button.success(buttonId, "Accept"));
            if (resume != null && resumeData != null) {
                reply.addFiles(FileUpload.fromData(resumeData.getBytes(StandardCharsets.UTF_8), resume.getFileName()));
            }
            reply.queue();
        }

        @Override
        public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
            String id = event.getButton().getId();
            if (id == null) {
                return;
            }
            String[] args = id.split("_");
            if (!args[0].equals("accept")) {
                return;
            }
            if (!checkPlaying(event, event.getUser())) {
                return;
            }
            if (!args[1].equals(event.getUser().getId())) {
                event.reply("This request wasn't for you.").setEphemeral(true).queue();
                return;
            }
            User user = event.getJDA().retrieveUserById(args[2]).complete();
            List<Message.Attachment> attachments = event.getMessage().getAttachments();
            String fileData = attachments.isEmpty() ? null : getAttachmentData(attachments.get(0));
            GameData.begin(event, event.getUser(), user, args[3].equals("true"), fileData);
        }
    };

    public static final ListenerAdapter LEAVE_COMMAND = new ListenerAdapter() {

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("leave")) {
                return;
            }
            GameData data = checkGame(event, event.getUser());
            if (data == null) {
                return;
            }
            data.end(event, Game.ActionResult.LOSE);
            GameData.remove(data);
        }
    };

    public static final ListenerAdapter PAUSE_COMMAND = new ListenerAdapter() {

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("pause")) {
                return;
            }
            GameData data = checkGame(event, event.getUser());
            if (data == null || !data.checkTurn(event, event.getUser())) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            String fileData = data.getFileData(now);
            String hash = GameData.getFileHashString(fileData);
            // TODO: link command with a snowflake
            event.reply("Game paused! Use " + ChaseRedsBot.playCommandMention + " and link this attachment to continue.\n" + hash)
                    .addFiles(FileUpload.fromData(fileData.getBytes(StandardCharsets.UTF_8), data.getFilename(now)))
                    .queue();
            GameData.remove(data);
        }
    };

    public static final ListenerAdapter FREE_COMMAND = new ListenerAdapter() {

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("free")) {
                return;
            }
            GameData data = checkGame(event, event.getUser());
            if (data == null || !data.checkTurn(event, event.getUser())) {
                return;
            }
            if (!data.inMenu) {
                event.reply("You're not in an action menu.").setEphemeral(true).queue();
                return;
            }
            event.reply("Menu freed.").setEphemeral(true).queue();
            data.setBoardDisabled(false);
            data.inMenu = false;
        }
    };

    public static final ListenerAdapter REFRESH_COMMAND = new ListenerAdapter() {

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("refresh")) {
                return;
            }
            GameData data = checkGame(event, event.getUser());
            if (data == null) {
                return;
            }
            GameHandler.refreshBoard(event, data);
        }
    };

    public static final ListenerAdapter HAND_COMMAND = new ListenerAdapter() {

        @Override
        public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
            if (!event.getName().equals("hand")) {
                return;
            }
            GameData data = checkGame(event, event.getUser());
            if (data == null) {
                return;
            }
            Map<Integer, Image> imageMap = CardImages.getImagesForPlayer(data.game.currentPlayer);
            List<Image> cardImages = data.getPlayer(event.getUser()).deck.hand.stream()
                    .map(imageMap::get)
                    .toList();
            byte[] image = CardImages.composeImage(150 + (cardImages.size() - 1) * 50, 150, (g2d, height) -> {
                for (int i = 0; i < cardImages.size(); i++) {
                    g2d.drawImage(cardImages.get(i), i * 50, 0, 150, height, null);
                }
            });
            FileUpload fileUpload = FileUpload.fromData(image, "deck.png");
            event.replyFiles(fileUpload).setEphemeral(true).queue();
        }
    };
}