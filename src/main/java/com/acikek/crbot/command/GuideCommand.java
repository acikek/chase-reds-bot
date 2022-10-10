package com.acikek.crbot.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

public class GuideCommand extends ListenerAdapter {

    public static final CommandData COMMAND_DATA = Commands.slash("guide", "Open a guide covering Chase Reds and this bot")
            .addOptions(
                    new OptionData(OptionType.STRING, "page", "The guide topic", true)
                            .addChoice("Game", "game")
                            .addChoice("Abilities", "abilities")
                            .addChoice("Commands", "commands")
                            .addChoice("Modes", "modes")
            );

    public static final String GAME = """
            **Chase Reds** is a tabletop card game inspired by War and Chess. It was created by users **acikek** and **Jeb_Kerm**, with the former being this bot's creator.
                        
            Each player gets a color half of a regular 52-piece deck of cards. **Black goes first**, and when you challenge another user, you'll be red.
            A player has four cards placed down in their **Army** in a T formation, three cards in their **Hand**, and the rest in an invisible **Deck**.
            The goal is to eliminate all opposing cards!
                        
            During your turn, you have a few options. You can place down cards from your Hand onto your Army's empty spaces. You can move or swap cards in your army to adjacent spaces, but only once-per-turn. Additionally, if one of your cards opposes something weaker, you can kill it, ending your turn immediately.
                        
            Each card class has their own passive ability that influences the moves you can make. Since they're passive, you don't have to seek them out; you can simply choose the ability moves in the action menus. To learn about card abilities, see the *Abilities* section.
            """;

    public static final String ABILITIES = """
            2, 3, and 4 make up the **Pawn** class. They can **serve** 8, 9, and 10, either by moving onto them or placing on top of them. This combined card also has combined power, but it fully discards after *any* kill.
            5, 6, and 7 make up the **Minor** class. When in the fallback slot, they can range attack any opposing card without self-destructing if the powers are the same.
            8, 9, and 10 make up the **Major** class. Along with supporting servants, they can move to any space in the Army, including non-adjacent spaces.
            
            Each card in the **Royalty** class has its own ability.
            Jacks can kill any card regardless of the opposing power, but self-destruct rules still apply to cards that are above or equal to its power.
            Queens can attack diagonally as well as forwards.
            When two Kings are placed side-by-side, you can activate **Chase**, which clears the entire opposing Army and Hand, along with the chasing Kings. You can only place **one** King per turn, and Kings cannot be moved or swapped with.
            Aces block all opposing abilities when placed in the front line. This supersedes other Aces, so Aces can't be blocked by other Aces.
            """;

    public static final String COMMANDS = """
            There are various helpful and sometimes essential commands to know while playing a game of Chase Reds.
            > *To view creative mode commands, see the* Modes *section*.
            
            View your **/hand** often. The hand card count, as well as your deck's, are displayed to the right of your avatar on the board.
            
            All action menus are private and only visible to you. If you accidentally close one, you can use **/free** to re-enable the current board's buttons again.
            If you've taken an exceedingly long time to make a move, the board buttons might not function. This can be fixed with a **/refresh** of the board.
            
            You can **/pause** a game and save the file to play later. When resuming, the player who initially paused should run the **/play** command again.
            If you know you're going to lose, you can always **/leave**, which ends the game in your opponent's favor immediately.
            """;

    public static final String MODES = """
            You can enable optional modes with the **/play** command that impact gameplay significantly.
            
            In **Buildup Mode**, the decks are configured to have high-power cards like Aces, Jacks, Queens and Kings appear later in the game. Not only does this create more tension, but it often helps create fairer starting scenarios.
            
            In **Creative Mode**, you can battle yourself and you also have access to special commands. This mode is useful for testing strategies and possible moves.
            You can **/clear** an individual army or the entire board.
            You can **/set** a card on an Army. This won't count as an action, and it doesn't take any game context (deck, hand, existing cards) into account.
            You can view your **/deck** similarly to how you view your hand. If you're battling yourself, this will display the current player's deck.
            """;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("guide")) {
            return;
        }
        String page = event.getOption("page", OptionMapping::getAsString);
        if (page == null) {
            return;
        }
        String message = switch (page) {
            case "game" -> GAME;
            case "abilities" -> ABILITIES;
            case "commands" -> COMMANDS;
            case "modes" -> MODES;
            default -> null;
        };
        if (message == null) {
            return;
        }
        event.reply(message).setEphemeral(true).queue();
    }
}
