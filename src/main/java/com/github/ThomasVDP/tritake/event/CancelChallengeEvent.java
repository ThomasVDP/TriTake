package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ChallengeManager;
import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.PrivateChannel;
import reactor.core.publisher.Mono;

public class CancelChallengeEvent implements IEvent<MessageCreateEvent>
{
    @Override
    public Class<?> getClassType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(MessageCreateEvent event)
    {
        return Mono.just(event.getMessage().getContent().matches("^(?:tt|tritake)!cancel$"))
                .filter(state -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                .filterWhen(state -> event.getMessage().getChannel().map(channel -> !(channel instanceof PrivateChannel)))
                .filter(state -> event.getMessage().getChannelId().equals(ServerManager.GetInstance().getServerToChannel().get(event.getGuildId().get())))
                .switchIfEmpty(Mono.just(false));
    }

    @Override
    public Mono<Object> execute(MessageCreateEvent event)
    {
        return event.getMessage().getAuthorAsMember()
                .flatMap(member -> ChallengeManager.GetInstance().cancelChallenge(event.getMessage(), event.getGuildId().get(), member, false))
                .flatMap(o -> Mono.empty());
    }
}
