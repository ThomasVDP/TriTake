package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.util.concurrent.TimeUnit;

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
        return Mono.just(true)
                .filter(state -> event.getMessage().getContent().matches("(?:tt|tritake)!bind"))
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
                        }))
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
                                    return message.getChannel().flatMap(channel -> channel.createMessage("Changing bound channel!\nConfirm by sending `tt!confirm` as your next message! (Within 3 minutes)")
                                            .flatMap(o -> Mono.fromRunnable(() -> ServerManager.GetInstance().getChannelWaitingList().put(channel.getId(), Schedulers.boundedElastic().schedule(() -> {
                                                ServerManager.GetInstance().getChannelWaitingList().remove(channel.getId());
                                                channel.createMessage("The time limit for changing the bound channel has run out!").subscribe();
                                            }, 3, TimeUnit.MINUTES)))));
                                }
                            } else {
                                ServerManager.GetInstance().getServerToChannel().put(guildIdTest.getT2(), message.getChannelId());
                                return message.getChannel().flatMap(channel -> channel.createMessage("Bound to this channel!"));
                            }
                        }));

    }
}
