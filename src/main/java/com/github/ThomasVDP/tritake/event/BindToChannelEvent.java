package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class BindToChannelEvent implements IEvent<MessageCreateEvent>
{
    @Override
    public Class<? extends Object> getClassType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(MessageCreateEvent event)
    {
        return Mono.just(event.getMessage().getContent().matches("(?:tt|tritake)!bind"))
                .filter(state -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                .switchIfEmpty(Mono.just(false));
    }

    @Override
    public Mono<Object> execute(MessageCreateEvent event)
    {
        return Mono.just(event)
                .map(MessageCreateEvent::getMessage)
                .flatMap(message -> message.getGuild()
                        .flatMap(guild -> Mono.just(Tuples.of(ServerManager.GetInstance().getServerToChannel().containsKey(guild.getId()), guild.getId())))
                        .flatMap(guildIdTest -> {
                            if (guildIdTest.getT1()) {
                                if (ServerManager.GetInstance().getServerToChannel().get(guildIdTest.getT2()).equals(message.getChannelId()))
                                {
                                    return message.getChannel().flatMap(channel -> channel.createMessage("Already bound to this channel!"));
                                } else {
                                    return message.getChannel().flatMap(channel -> channel.createMessage("Changing bound channel!\nConfirm by sending `tt!confirm` as your next message! (Within 3 minutes)"));
                                }
                            } else {
                                ServerManager.GetInstance().getServerToChannel().put(guildIdTest.getT2(), message.getChannelId());
                                return message.getChannel().flatMap(channel -> channel.createMessage("Bound to this channel!"));
                            }
                        }));

    }
}
