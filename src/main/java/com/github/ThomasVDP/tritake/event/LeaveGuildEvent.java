package com.github.ThomasVDP.tritake.event;

import com.github.ThomasVDP.tritake.ChallengeManager;
import com.github.ThomasVDP.tritake.GameManager;
import com.github.ThomasVDP.tritake.ServerManager;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import reactor.core.publisher.Mono;

public class LeaveGuildEvent implements IEvent<GuildDeleteEvent>
{
    @Override
    public Class<?> getClassType()
    {
        return GuildDeleteEvent.class;
    }

    @Override
    public Mono<Boolean> canExecute(GuildDeleteEvent event)
    {
        System.out.println("Test");
        return Mono.just(true)
                .filter(state -> !event.isUnavailable())
                .switchIfEmpty(Mono.just(false));
    }

    @Override
    public Mono<Object> execute(GuildDeleteEvent event)
    {
        return Mono.fromRunnable(() -> {
            ServerManager.GetInstance().getServerToChannel().remove(event.getGuildId());
            ServerManager.GetInstance().getServerToRoles().remove(event.getGuildId());
            ChallengeManager.GetInstance().getChallenges(event.getGuildId()).forEach(tuple -> tuple.getT4().dispose());
            ChallengeManager.GetInstance().getChallengeList().remove(event.getGuildId());
            GameManager.GetInstance().getGames().remove(event.getGuildId());
        });
    }
}
