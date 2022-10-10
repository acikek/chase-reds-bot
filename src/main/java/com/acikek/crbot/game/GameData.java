package com.acikek.crbot.game;

import com.acikek.crbot.ChaseRedsBot;
import com.acikek.crbot.core.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class GameData {

    public static final int CARD_WIDTH = 200;
    public static final int CARD_HEIGHT = CardImages.getCardHeight(CARD_WIDTH);
    public static final int CARD_BORDER = 40;
    public static final int ARMY_BORDER = 100;
    public static final int BOARD_WIDTH_PADDING = 600;
    public static final int BOARD_WIDTH = CARD_WIDTH * 3 + CARD_BORDER * 2 + BOARD_WIDTH_PADDING;
    public static final int BOARD_HEIGHT = CARD_HEIGHT * 4 + CARD_BORDER * 2 + ARMY_BORDER;

    public static final Font FONT = new Font("Arial", Font.BOLD, 70);
    public static final DateTimeFormatter FILE_FORMAT =  DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    public static final DateTimeFormatter HEADER_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public static final String[] QUOTES = {
            "At the end of the game the kind and the pawn go back in the same box.",
            "Never allow the fear of striking out keep you from playing the game.",
            "As a rule, the more mistakes there are in a game, the more memorable it remains, because you have suffered and worried over each mistake at the board.",
            "The world's a puzzle; no need to make sense out of it.",
            "Baseball, board games, playing Jeopardy, I hate to lose.",
            "No one ever won a game by resigning.",
            "They say that life's a game, and then they take the board away.",
            "The goal is to win, but it is the goal that is important, not the winning.",
            "You have to learn the rules of the game. And then you have to play better than anyone else.",
            "The way a man plays a game shows some of his character. The way he loses shows all of it.",
    };

    public record PlayerData(User user, Image avatar) {}

    public PlayerData black;
    public PlayerData red;

    public Game game;
    public boolean creative;

    public Message currentBoard;
    public boolean inMenu;
    public boolean pauseValid;

    public GameData(User user, User opponent, Game game, boolean creative) {
        try {
            boolean flipped = game.currentPlayer == Player.Type.RED;
            User black = flipped ? opponent : user;
            User red = flipped ? user : opponent;
            this.black = new PlayerData(black, getCircleAvatar(ImageIO.read(black.getEffectiveAvatar().download().get()), Color.BLACK));
            this.red = new PlayerData(red, getCircleAvatar(ImageIO.read(red.getEffectiveAvatar().download().get()), Color.RED));
        } catch (Exception e) {
            ChaseRedsBot.LOGGER.error("Avatars failed to download and convert!", e);
        }
        this.game = game;
        this.creative = creative;
    }

    public static Image getCircleAvatar(Image image, Color outlineColor) {
        int width = image.getWidth(null);
        BufferedImage bufferedImage = new BufferedImage(width + 6, width + 6, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setClip(new Ellipse2D.Float(3, 3, width, width));
        g2d.drawImage(image, 3, 3, width, width, null);
        g2d.setClip(null);
        g2d.setColor(outlineColor);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(3, 3, width, width);
        g2d.dispose();
        return bufferedImage;
    }

    public static void setButtonsDisabled(Message message, boolean disabled) {
        List<LayoutComponent> disabledButtons = message.getComponents().stream()
                .map(layout -> layout.withDisabled(disabled))
                .toList();
        message.editMessageComponents(disabledButtons).queue();
    }

    public void setBoardDisabled(boolean disabled) {
        setButtonsDisabled(currentBoard, disabled);
    }

    public User getOtherUser(User user) {
        return user.getIdLong() == black.user.getIdLong()
                ? red.user
                : black.user;
    }

    public PlayerData getPlayerData(Player.Type type) {
        return switch (type) {
            case BLACK -> black;
            case RED -> red;
            default -> null;
        };
    }

    public PlayerData getCurrentPlayerData() {
        return getPlayerData(game.currentPlayer);
    }

    public Player getPlayer(User user) {
        if (black.user.getIdLong() == red.user.getIdLong()) {
            return game.getPlayer(game.currentPlayer);
        }
        Player.Type type = user.getIdLong() == black.user.getIdLong()
                ? Player.Type.BLACK
                : Player.Type.RED;
        return game.getPlayer(type);
    }

    public boolean checkTurn(IReplyCallback event, User user) {
        if (getCurrentPlayerData().user.getIdLong() != user.getIdLong()) {
            event.reply("It's the other player's turn...")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    public boolean checkCreative(IReplyCallback event) {
        if (!creative) {
            event.reply("You can only use this command in a Creative game.")
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    public static int[] getCardCoordinates(Position position, boolean current) {
        int[] result = new int[2];
        if (position.isCenterFile()) {
            result[0] = CARD_WIDTH + CARD_BORDER;
        }
        else {
            boolean left = (current ? position : position.relative()) == Position.LEFT;
            result[0] = left ? 0 : CARD_WIDTH * 2 + CARD_BORDER * 2;
        }
        int y = position == Position.FALLBACK ? 0 : (CARD_HEIGHT + CARD_BORDER);
        if (current) {
            y = (BOARD_HEIGHT - CARD_HEIGHT) - y;
        }
        result[1] = y;
        return result;
    }

    public void drawPlayerArmy(Player.Type player, Graphics2D g2d, int height) {
        for (Card card : game.getPlayer(player).army.all()) {
            if (card.type == Card.Type.EMPTY) {
                continue;
            }
            int[] coordinates = getCardCoordinates(card.position, game.currentPlayer == player);
            CardImages.drawCard(g2d, CardImages.getImagesForPlayer(player), card, coordinates[0], coordinates[1], CARD_WIDTH, height);
        }
    }

    public static void drawOutlineText(Graphics2D g2d, String string, int x, int y) {
        g2d.setColor(Color.WHITE);
        g2d.drawString(string, x, y);
        FontRenderContext frc = g2d.getFontRenderContext();
        GlyphVector vector = FONT.createGlyphVector(frc, string);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));
        g2d.translate(x, y);
        g2d.draw(vector.getOutline());
        g2d.translate(-x, -y);
    }

    public void drawGameText(Graphics2D g2d) {
        drawOutlineText(g2d, game.currentPlayer + "'s turn", 800, 400);
        drawOutlineText(g2d, "Turn #" + game.turns.size(), 800, 500);
        if (game.lastAction != null) {
            String lastAction = game.lastAction == Action.PASS ? "Passed" : game.lastAction.toTurnString();
            drawOutlineText(g2d, lastAction, 800, 600);
        }
    }

    public void drawAvatar(Graphics2D g2d, Player.Type player, int x, int y) {
        g2d.drawImage(getPlayerData(player).avatar(), x, y, 250, 250, null);
        drawOutlineText(g2d, game.getPlayer(player).deck.hand.size() + "H", x + 290, y + 90);
        drawOutlineText(g2d, game.getPlayer(player).deck.cards.size() + "D", x + 290, y + 210);
    }

    public void drawAvatars(Graphics2D g2d) {
        int x = BOARD_WIDTH - BOARD_WIDTH_PADDING + 115;
        drawAvatar(g2d, game.currentPlayer, x, BOARD_HEIGHT - 600);
        drawAvatar(g2d, game.currentPlayer.next(), x, BOARD_HEIGHT - 300);
    }

    public FileUpload getBoard() {
        byte[] image = CardImages.composeImage(BOARD_WIDTH, BOARD_HEIGHT, CARD_WIDTH, (g2d, height) -> {
            drawPlayerArmy(game.currentPlayer, g2d, height);
            drawPlayerArmy(game.currentPlayer.next(), g2d, height);
            g2d.setFont(FONT);
            drawGameText(g2d);
            drawAvatars(g2d);
        });
        return FileUpload.fromData(image, "board.png");
    }

    public static boolean validateFile(IReplyCallback event, List<String> lines) {
        System.out.println(lines);
        for (int i = 0; i < lines.size(); i ++) {
            try {
                Turn.parse(lines.get(i));
            } catch (Exception e) {
                event.reply("Failed to validate game file. Invalid turn #" + (i + 1) + ": `" + lines.get(i) + "`").setEphemeral(true).queue();
                return false;
            }
        }
        return true;
    }

    public static void begin(IReplyCallback event, User user, User opponent, boolean buildup, boolean creative, String fileData) {
        List<Turn> turns = null;
        if (fileData != null) {
            List<String> lines = Game.getTurnLines(fileData);
            if (!validateFile(event, lines)) {
                return;
            }
            turns = lines.stream()
                    .map(Turn::parse)
                    .toList();
        }
        event.deferReply().queue();
        Game game = new Game(turns, buildup);
        GameData data = new GameData(user, opponent != null ? opponent : user, game, creative);
        game.begin();
        var reply = event.getHook().editOriginalAttachments(data.getBoard());
        GameHandler.addMenuButtons(reply, game);
        data.currentBoard = reply.complete();
        ChaseRedsBot.games.put(user, data);
        ChaseRedsBot.games.put(opponent, data);
    }

    public String getEndMessage(User winningPlayer) {
        return winningPlayer == null
                ? "**STALEMATE!** The game ends in a tie!"
                : "**" + winningPlayer.getName().toUpperCase() + " WINS!**";
    }

    public String getFilename(LocalDateTime now) {
        return black.user.getName() + "_" + red.user.getName() + "_" + FILE_FORMAT.format(now) + ".chase";
    }

    public String getFileData(LocalDateTime now) {
        Map<String, String> headers = Map.of(
                "rec", "Chase Reds Bot",
                "black", black.user.getName(),
                "red", red.user.getName(),
                "date", HEADER_FORMAT.format(now)
        );
        return game.getFileData(headers);
    }

    public static String getFileHashString(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return "**HASH: `" + Base64.getEncoder().encodeToString(hash) + "`**";
        }
        catch (NoSuchAlgorithmException e) {
            return "**HASH: UNKNOWN**";
        }
    }

    public void end(IReplyCallback event, User winningPlayer) {
        String message = getEndMessage(winningPlayer);
        LocalDateTime now = LocalDateTime.now();
        event.reply(message + "\n*" + QUOTES[game.random.nextInt(QUOTES.length)] + "*")
                .addFiles(FileUpload.fromData(getFileData(now).getBytes(StandardCharsets.UTF_8), getFilename(now)))
                .queue();
    }

    public static void remove(GameData data) {
        ChaseRedsBot.games.remove(data.black.user());
        ChaseRedsBot.games.remove(data.red.user());
    }
}
