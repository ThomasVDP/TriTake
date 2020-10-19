package com.github.ThomasVDP.tritake;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeManager
{
    private final Map<Snowflake, List<Tuple3<Snowflake, Snowflake, Boolean>>> challenges = new HashMap<>();

    private static ChallengeManager instance;

    public ChallengeManager()
    {
        if (instance == this)
        {
            System.exit(1);
        }
        instance = this;
    }

    public void CreateNewChallenge(Snowflake guild, Snowflake from, Snowflake to)
    {

    }

    public Mono<Boolean> isChallengePossible(Message messageIn)
    {
        return Mono.just(true)
                .filterWhen(state -> messageIn.getUserMentions()
                        .flatMap(mentioned -> mentioned.isBot()
                                ? messageIn.getChannel().flatMap(channel -> channel.createMessage("You  cannot challenge a bot! " + messageIn.getAuthor().map(user -> user.getMention()).orElse(""))).map(o -> false)
                                : Mono.just(true)))
                //.filterWhen(state -> messageIn.getUserMentions()
                //        .flatMap(mentioned -> messageIn.getAuthor().map(user -> Mono.just(user.getId().equals(mentioned.getId()))
                //        .flatMap(value -> value ? messageIn.getChannel().flatMap(channel -> channel.createMessage("You cannot challenge yourself! " + mentioned.getMention())).map(o -> false) : Mono.just(true))).orElse(Mono.just(false))).next())
                .switchIfEmpty(Mono.just(false));

    }

    public boolean hasChallenged(Snowflake guild, Snowflake member)
    {
        return challenges.containsKey(guild) && challenges.get(guild).stream().anyMatch(tuple -> tuple.getT1().equals(member));
    }

    public boolean hasAcceptedChallenge(Snowflake guild, Snowflake member)
    {
        return challenges.containsKey(guild) && challenges.get(guild).stream().anyMatch(tuple -> tuple.getT2().equals(member) && tuple.getT3());
    }

    public static ChallengeManager GetInstance()
    {
        return instance;
    }
}
