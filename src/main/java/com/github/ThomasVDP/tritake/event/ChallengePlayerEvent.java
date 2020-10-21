package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ChallengeManager;
import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import reactor.core.publisher.Mono;


public class ChallengePlayerEvent implements IEvent<MessageCreateEvent>
{
    @Override
    public Class<?> getClassType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(MessageCreateEvent event)
    {
        System.out.println(event.getMessage().getContent());
        return Mono.just(event.getMessage().getContent().matches("^(?:tt|tritake)!challenge <@!?\\d+?>$"))
                .filter(state -> event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false))
                .filterWhen(state -> event.getMessage().getChannel().map(channel -> !(channel instanceof PrivateChannel)))
                .filter(state -> event.getMessage().getChannelId().equals(ServerManager.GetInstance().getServerToChannel().get(event.getGuildId().get())))
                .switchIfEmpty(Mono.just(false));
    }

    @Override
    public Mono<Object> execute(MessageCreateEvent event)
    {
        return Mono.just(event)
                .map(MessageCreateEvent::getMessage)
                .filterWhen(ChallengeManager.GetInstance()::isChallengePossible)
                .flatMapMany(Message::getUserMentions)
                .flatMap(user -> event.getMessage().getChannel()
                        .flatMap(channel -> channel.createMessage(event.getMessage().getAuthor().map(User::getMention).orElse("") + " challenged you for a TriTake game! " + user.getMention())
                                .flatMap(message -> Mono.fromRunnable(() -> ChallengeManager.GetInstance().CreateNewChallenge(event.getGuildId().get(), event.getMessage().getAuthor().map(User::getId).get(), user.getId())).zipWith(
                                        message.addReaction(ReactionEmoji.of(null, "\u2705", false)))
                                        .zipWith(message.addReaction(ReactionEmoji.of(null, "\u274C", false)))))
                ).flatMap(o -> Mono.empty()).next();
    }
}
