package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ChallengeManager;
import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public class ChallengePlayerEvent implements IEvent<MessageCreateEvent>
{
    @Override
    public String getName()
    {
        return "tt!challenge";
    }

    @Override
    public Class<?> getClassType()
    {
        return MessageCreateEvent.class;
    }

    @Override
    public boolean canExecute(MessageCreateEvent event)
    {
        System.out.println(event.getMessage().getContent());
        return event.getMessage().getContent().matches("^(?:tt|tritake)!challenge <@!?\\d+?>$")
                && event.getMessage().getAuthor().map(user -> !user.isBot()).orElse(false)
                && event.getMessage().getChannel().map(channel -> !(channel instanceof PrivateChannel)).block()
                && event.getMessage().getChannelId().equals(ServerManager.GetInstance().getServerToChannel().get(event.getGuildId().get()));
    }

    @Override
    public Mono<Tuple2<Tuple2<Void, Void>, Object>> execute(MessageCreateEvent event)
    {
        return Mono.just(event)
                .map(MessageCreateEvent::getMessage)
                .filterWhen(ChallengeManager.GetInstance()::isChallengePossible)
                .flatMapMany(Message::getUserMentions)
                .flatMap(user -> event.getMessage().getChannel()
                        .flatMap(channel -> channel.createMessage(event.getMessage().getAuthor().map(User::getMention).orElse("") + " challenged you for a TriTake game! " + user.getMention())
                                .flatMap(message -> message.addReaction(ReactionEmoji.of(null, "\u2705", false)).zipWith(
                                        message.addReaction(ReactionEmoji.of(null, "\u274C", false)))
                                        .zipWith(Mono.fromRunnable(() -> ChallengeManager.GetInstance().CreateNewChallenge(event.getGuildId().get(), event.getMessage().getAuthor().map(User::getId).get(), user.getId())))))
                ).next();

                /*.flatMap(Message::getChannel)
                //.flatMap(channel -> channel.createMessage(event.getMessage().getAuthor().map(User::getMention).orElse("") + " challenged you for a TriTake game! " + event.getMessage().getUser))

                .flatMap(message -> message.getUserMentions()
                    .flatMap(user -> user.isBot()
                            ? message.getChannel().flatMap(messageChannel -> messageChannel.createMessage("You cannot challenge a bot! " + message.getAuthor().map(User::getMention).orElse("")))
                            :  /*user.getId().equals(message.getAuthor().map(User::getId).get())
                                    ? message.getChannel().flatMap(channel -> channel.createMessage("You cannot challenge yourself! " + user.getMention()))
                                    : message.getChannel().flatMap(channel -> channel.createMessage(message.getAuthor().map(User::getMention).orElse("") + " challenged you for a TriTake game! " + user.getMention())
                                            .map(newMessage -> Flux.just(newMessage.addReaction(ReactionEmoji.of(null, "\u2705", false)).subscribe(),
                                                    newMessage.addReaction(ReactionEmoji.of(null, "\u274C", false)).subscribe()))))*/
                //.next());

        /*return Mono.just(event)
                .map(MessageCreateEvent::getMessage)
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Works!"));*/
    }
}
