package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class ConfirmChannelEvent implements IEvent<MessageCreateEvent>
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
                .filter(state -> event.getMessage().getContent().matches("^(?:tt|tritake)!confirm$"))
                .filter(state -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                .filterWhen(state -> event.getMessage().getChannel().map(channel -> !(channel instanceof PrivateChannel)))
                .filterWhen(state -> event.getMessage().getAuthorAsMember()
                        .flatMap(Member::getBasePermissions)
                        .map(perm -> perm.and(PermissionSet.of(Permission.MANAGE_CHANNELS)))
                        .flatMap(set -> {
                            if (set.isEmpty()) {
                                return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("You don't have the right permissions to do that! " + event.getMessage().getAuthor().map(User::getMention).orElse(""))).map(o -> false);
                            }
                            return Mono.just(true);
                        })
                )
                .switchIfEmpty(Mono.just(false));
    }

    @Override
    public Publisher<Object> execute(MessageCreateEvent event)
    {
        return event.getMessage().getChannel()
                .flatMap(channel -> {
                    if (ServerManager.GetInstance().getChannelWaitingList().containsKey(channel.getId()))
                    {
                        ServerManager.GetInstance().getChannelWaitingList().get(channel.getId()).dispose();
                        ServerManager.GetInstance().getChannelWaitingList().remove(channel.getId());
                        ServerManager.GetInstance().getServerToChannel().put(event.getGuildId().get(), channel.getId());
                        return channel.createMessage("Changed bound channel to this channel!");
                    }
                    return Mono.empty();
                });
    }
}
