package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.PrivateChannel;
import reactor.core.publisher.Mono;

public class RemoveRoleForChallengesEvent implements IEvent<MessageCreateEvent>
{
    @Override
    public Class<?> getClassType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(MessageCreateEvent event)
    {
        return Mono.just(event.getMessage().getContent().matches("^(?:tt|tritake)!removeRole <@&\\d+>$"))
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
                            ServerManager.GetInstance().getServerToRoles().get(event.getGuildId().get()).remove(role.getId());
                            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("Role " + role.getMention() + " was removed from the whitelist!"));
                        }
                    }
                    return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("This role is not on the whitelist!"));
                }).flatMap(o -> Mono.empty()).next();
    }
}
