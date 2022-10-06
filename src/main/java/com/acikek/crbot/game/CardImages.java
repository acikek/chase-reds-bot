package com.acikek.crbot.game;

import com.acikek.crbot.ChaseRedsBot;
import com.acikek.crbot.core.Card;
import com.acikek.crbot.core.Deck;
import com.acikek.crbot.core.Player;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class CardImages {

    public static final int CARD_WIDTH = 500;
    public static final int CARD_HEIGHT = 726;

    public static Map<Integer, Image> blackCards;
    public static Map<Integer, Image> redCards;

    public static Map<Integer, Image> getCardImages(String suffix) {
        Map<Integer, Image> images = new HashMap<>();
        for (int power : Deck.POWERS) {
            String name = "/images/" + (Card.getCharacter(power).toLowerCase() + suffix) + ".png";
            InputStream file = ChaseRedsBot.class.getResourceAsStream(name);
            if (file != null) {
                try {
                    images.put(power, ImageIO.read(file));
                }
                catch (IOException e) {
                    ChaseRedsBot.LOGGER.error("Failed to read card image " + name + "!");
                }
            }
        }
        return images;
    }

    public static Map<Integer, Image> getImagesForPlayer(Player.Type type) {
        return switch (type) {
            case BLACK -> blackCards;
            case RED -> redCards;
            default -> null;
        };
    }

    public static double getCardProportion(int width) {
        return (double) width / CARD_WIDTH;
    }

    public static int getCardHeight(int width) {
        return (int) (getCardProportion(width) * CARD_HEIGHT);
    }

    public static void drawCard(Graphics2D g2d, Map<Integer, Image> cards, Card card, int x, int y, int width, int height) {
        double proportion = getCardProportion(width);
        g2d.drawImage(cards.get(card.power), x, y, width, height, null);
        if (card.servant != 0) {
            int offset = (int) (proportion * 50);
            g2d.drawImage(cards.get(card.servant), x + offset, y - offset, width, height, null);
        }
    }

    public static byte[] composeImage(int width, int height, int cardWidth, BiConsumer<Graphics2D, Integer> function) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        function.accept(g2d, getCardHeight(cardWidth));
        g2d.dispose();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(result, "png", stream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stream.toByteArray();
    }

    public static byte[] composeImage(int width, int cardWidth, BiConsumer<Graphics2D, Integer> function) {
        return composeImage(width, getCardHeight(cardWidth), cardWidth, function);
    }

    public static byte[] composeCardRowImage(List<Integer> powers, Map<Integer, Image> cards) {
        List<Image> cardImages = powers.stream()
                .map(cards::get)
                .toList();
        return CardImages.composeImage(150 + (cardImages.size() - 1) * 50, 150, (g2d, height) -> {
            for (int i = 0; i < cardImages.size(); i++) {
                g2d.drawImage(cardImages.get(i), i * 50, 0, 150, height, null);
            }
        });
    }
}
