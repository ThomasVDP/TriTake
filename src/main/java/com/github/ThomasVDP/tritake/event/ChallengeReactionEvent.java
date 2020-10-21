package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ChallengeManager;
import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.PrivateChannel;
import reactor.core.publisher.Mono;

public class ChallengeReactionEvent implements IEvent<ReactionAddEvent>
{
    @Override
    public Class<?> getClassType()
    {
        return ReactionAddEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(ReactionAddEvent event)
    {
        return event.getMessage().flatMap(Message::getChannel).map(channel -> !(channel instanceof PrivateChannel))
                .filter(state -> event.getChannelId().equals(ServerManager.GetInstance().getServerToChannel().get(event.getGuildId().get())))
                .filterWhen(state -> event.getUser().map(user -> !user.isBot()))
                .filterWhen(state -> event.getMessage().flatMap(message -> message.getUserMentions()
                        .next().map(user -> ChallengeManager.GetInstance().hasChallengedPlayer(event.getGuildId().get(), user.getId(), event.getUserId()))))
                .switchIfEmpty(Mono.just(false));
        //return Mono.just(true);
    }

    @Override
    public Mono<Object> execute(ReactionAddEvent event)
    {
        return event.getChannel().flatMap(channel -> channel.createMessage("Reacted with " + event.getEmoji().toString()));
    }
}
