package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ChallengeManager;
import com.github.ThomasVDP.tritake.GameManager;
import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.reaction.ReactionEmoji;
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
                .filterWhen(state -> event.getUser().map(user -> ChallengeManager.GetInstance().getChallengerFor(event.getGuildId().get(), user.getId()).isPresent()))
                .switchIfEmpty(Mono.just(false));
    }

    @Override
    public Mono<Object> execute(ReactionAddEvent event)
    {
        return Mono.just(event)
                .filter(o -> !ChallengeManager.GetInstance().hasAcceptedChallenge(event.getGuildId().get(), o.getUserId()))
                .filterWhen(this::handleReaction)
                .flatMap(o -> o.getMessage()
                    .flatMap(message -> o.getUser()
                        .flatMap(user -> user.asMember(event.getGuildId().get())
                                .flatMap(memberTo -> ServerManager.GetInstance().getClient().getMemberById(event.getGuildId().get(), ChallengeManager.GetInstance().getChallengerFor(event.getGuildId().get(), memberTo.getId()).get())
                                    .flatMap(memberFrom -> Mono.fromRunnable(() -> {
                                        int sizeIndex = message.getContent().indexOf("(") + 1;
                                        //System.out.println(sizeIndex + ": " + message.getContent().substring(sizeIndex, sizeIndex + 1));
                                        GameManager.GetInstance().CreateGame(event.getChannel(), memberFrom, memberTo, Integer.parseInt(message.getContent().substring(sizeIndex, sizeIndex + 1)));
                                    }))))));
    }

    private Mono<Boolean> handleReaction(ReactionAddEvent event)
    {
        return Mono.just(true)
                .flatMap(state -> event.getEmoji().asUnicodeEmoji().flatMap(ReactionEmoji::asUnicodeEmoji).filter(s -> s.getRaw().equals("\u274C"))
                        .map(s -> event.getChannel()
                                .flatMap(channel -> event.getUser()
                                        .flatMap(userTo -> ServerManager.GetInstance().getClient().getUserById(ChallengeManager.GetInstance().getChallengerFor(event.getGuildId().get(), userTo.getId()).get())
                                                .flatMap(userFrom -> userFrom.asMember(event.getGuildId().get()))
                                                .flatMap(memberFrom -> event.getMessage().flatMap(message -> {
                                                    ChallengeManager.GetInstance().getChallenges(event.getGuildId().get()).stream().filter(tuple -> tuple.getT1().equals(memberFrom.getId()) && !tuple.getT3()).findFirst().get().getT4().dispose();
                                                    ChallengeManager.GetInstance().getChallenges(event.getGuildId().get()).removeIf(tuple -> tuple.getT1().equals(memberFrom.getId()) && !tuple.getT3());
                                                    return channel.createMessage(userTo.getMention() + " declined the challenge. " + memberFrom.getMention());
                                                }))))
                                .map(o -> true)).orElse(Mono.just(true)))
                .map(state -> event.getEmoji().asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw).map(s -> s.equals("\u2705")).orElse(false));

    }
}
