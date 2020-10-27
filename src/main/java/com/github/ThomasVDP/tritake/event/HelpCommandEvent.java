package com.github.ThomasVDP.tritake.event;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class HelpCommandEvent implements IEvent<MessageCreateEvent>
{
    @Override
    public Class<?> getClassType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(MessageCreateEvent event)
    {
        return Mono.just(true)
                .filter(state -> event.getMessage().getContent().matches("^(?:tt|tritake)!help$"))
                .filter(state -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                .switchIfEmpty(Mono.just(false));
    }

    @Override
    public Mono<Object> execute(MessageCreateEvent event)
    {
        return event.getMessage().getAuthor()
                .map(Mono::just).orElse(Mono.empty())
                .flatMap(User::getPrivateChannel)
                .flatMap(channel -> channel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle("TriTake help message!")
                        .setColor(Color.DISCORD_WHITE)
                        .setFooter("(made by T_VDP)", null)
                        .setDescription("The official help message for TriTake!\n(if it wasn't obvious already :D)")
                        .addField("Goal", "The game is very **simple**!\n\n -- **Don't remove the last square!** --", false)
                        .addField("Idea", "Every turn you remove an amount of squares but you're limited to one row every turn. So choose careful because you lose if you take the last square!!", false)
                        .addField("Gameplay", "To start a game you `tt!challenge @user` a person and then wait for them to accept (or reject D: )\n" +
                                "Accepting is as easy as reacting with a checkmark (refusing works the same way)\n" +
                                "The game starts.\n" +
                                "You can both then control the game by messagine `tt!move <row> <amount>` where <row> and <amout> are replaced by your chosen row and amount.\n" +
                                "Whoever makes the other take away the last square wins!", false)
                        .addField("User Commands", "- **tt!help**: Shows this message!\n" +
                                "- **tt!challenge @user [size]**: Makes you challenge the user to a game consisting of `[size]` rows (between 5 and 9). Note that the size defaults to 5.\n" +
                                "- **tt!move <row> <amount>**: Makes you take away `<amount>` squares on the `<row>`th row.\n" +
                                "- **tt!cancel**: Makes you cancel your challenge/game.\n", false)
                        .addField("Admin Commands", "- **tt!bind**: Makes the bot listen to a certain channel only. (required to make the bot work!)\n" +
                                "- **tt!confirm**: Command needed to change the bound channel." +
                                "- **tt!removeRole @role**: Removes the `role` from the bot's whitelist.\n" +
                                "- **tt!addRole @role**: Adds the `role` to the bot's whitelist, you cannot challenge or get challenged when you don't have one of these roles.\n", false)
                )).flatMap(message -> event.getMessage().delete()).onErrorResume(err -> Mono.empty()).flatMap(o -> Mono.empty());
    }
}
