package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.PrivateChannel;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class AssignRoleForChallengesEvent implements IEvent<MessageCreateEvent>
{
    @Override
    public Class<?> getClassType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(MessageCreateEvent event)
    {
        return Mono.just(event.getMessage().getContent().matches("^(?:tt|tritake)!addRole <@&\\d+>$"))
                .filter(state -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                .filterWhen(state -> event.getMessage().getChannel().map(channel -> !(channel instanceof PrivateChannel)))
                .filter(state -> event.getMessage().getChannelId().equals(ServerManager.GetInstance().getServerToChannel().get(event.getGuildId().get())))
                .switchIfEmpty(Mono.just(false));

    }

    @Override
    public Mono<Object> execute(MessageCreateEvent event)
    {
        return event.getMessage().getRoleMentions()
                .flatMap(role -> {
                    if (ServerManager.GetInstance().getServerToRoles().containsKey(event.getGuildId().get()))
                    {
                        if (ServerManager.GetInstance().getServerToRoles().get(event.getGuildId().get()).contains(role.getId()))
                        {
                            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("This role was already added to the whitelist!")).flatMap(o -> Mono.empty());
                        } else
                        {
                            ServerManager.GetInstance().getServerToRoles().get(event.getGuildId().get()).add(role.getId());
                        }
                    } else {
                        List<Snowflake> list = new ArrayList<>();
                        list.add(role.getId());
                        ServerManager.GetInstance().getServerToRoles().put(event.getGuildId().get(), list);
                    }
                    return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("Role " + role.getMention() + " was added to whitelist!")).flatMap(o -> Mono.empty());
                }).next();
    }
}
