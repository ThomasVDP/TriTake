package com.github.ThomasVDP.tritake.event.game;

import com.github.ThomasVDP.tritake.ChallengeManager;
import com.github.ThomasVDP.tritake.GameManager;
import com.github.ThomasVDP.tritake.ServerManager;
import com.github.ThomasVDP.tritake.event.IEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.PrivateChannel;
import reactor.core.publisher.Mono;

public class GameMoveEvent implements IEvent<MessageCreateEvent>
{
    @Override
    public Class<?> getClassType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(MessageCreateEvent event)
    {
        return Mono.just(event.getMessage().getContent().matches("^(?:tt|tritake)!move \\d \\d$"))
                .filter(state -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                .filterWhen(state -> event.getMessage().getChannel().map(channel -> !(channel instanceof PrivateChannel)))
                .filter(state -> event.getMessage().getChannelId().equals(ServerManager.GetInstance().getServerToChannel().get(event.getGuildId().get())))
                .switchIfEmpty(Mono.just(false));
    }

    @Override
    public Mono<Object> execute(MessageCreateEvent event)
    {
        return event.getMessage().getAuthorAsMember()
                .flatMap(member -> !ChallengeManager.GetInstance().hasStartedChallenge(member.getGuildId(), member.getId())
                        ? event.getMessage().delete()
                        : Mono.fromRunnable(() -> GameManager.GetInstance().executeMove(member, event))
                );
    }
}
