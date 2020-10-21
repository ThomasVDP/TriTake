package com.github.ThomasVDP.tritake;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeManager
{
    // tuple<from, to, hasAccepted>
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
        if (this.challenges.containsKey(guild))
        {
            this.challenges.get(guild).add(Tuples.of(from, to, false));
        } else {
            List<Tuple3<Snowflake, Snowflake, Boolean>> list = new ArrayList<>();
            list.add(Tuples.of(from, to, false));
            this.challenges.put(guild, list);
        }
    }

    public Mono<Boolean> isChallengePossible(Message messageIn)
    {
        return Mono.just(true)
                //can't challenge bot!
                .filterWhen(state -> messageIn.getUserMentions().next()
                        .flatMap(mentioned -> mentioned.isBot()
                                ? messageIn.getChannel().flatMap(channel -> channel.createMessage("You  cannot challenge a bot! " + messageIn.getAuthor().map(user -> user.getMention()).orElse(""))).map(o -> false)
                                : Mono.just(true)))
                //can't challenge yourself!
                //.filterWhen(state -> messageIn.getUserMentions()
                //        .flatMap(mentioned -> messageIn.getAuthor().map(user -> Mono.just(user.getId().equals(mentioned.getId()))
                //        .flatMap(value -> value ? messageIn.getChannel().flatMap(channel -> channel.createMessage("You cannot challenge yourself! " + mentioned.getMention())).map(o -> false) : Mono.just(true))).orElse(Mono.just(false))).next())
                //can't challenge when challenged someone!
                .filterWhen(state -> messageIn.getAuthor().map(user -> hasStartedChallenge(messageIn.getGuildId().get(), user.getId())
                        ? messageIn.getChannel().flatMap(channel -> channel.createMessage("You cannot challenge two people at the same time! " + user.getMention())).map(o -> false)
                        : Mono.just(true)).orElse(Mono.just(false)))
                //can't challenge when accepted
                .filterWhen(state -> messageIn.getAuthor().map(user -> hasAcceptedChallenge(messageIn.getGuildId().get(), user.getId())
                        ? messageIn.getChannel().flatMap(channel -> channel.createMessage("You have already accepted a challenge! " + user.getMention())).map(o -> false)
                        : Mono.just(true)).orElse(Mono.just(false)))
                //can't challenge when no role (unless no roles specified)
                .filterWhen(state -> messageIn.getAuthorAsMember()
                        .filter(member -> ServerManager.GetInstance().getServerToRoles().containsKey(messageIn.getGuildId().get()) && !ServerManager.GetInstance().getServerToRoles().get(messageIn.getGuildId().get()).isEmpty())
                        .flatMap(member -> Flux.fromIterable(ServerManager.GetInstance().getServerToRoles().get(messageIn.getGuildId().get()))
                                .flatMap(roleId -> member.getRoles().any(role -> role.getId().equals(roleId)))
                                .any(bool -> bool))
                        .doOnNext(hasRole -> {
                            if (!hasRole) {
                                messageIn.getChannel().flatMap(channel -> channel.createMessage("You cannot challenge someone without one of the necessary role(s)! " + messageIn.getAuthor().map(User::getMention).orElse(""))).subscribe();
                            }
                        })
                        .switchIfEmpty(Mono.just(true)))
                //can't challenge someone when they have no role (unless no roles specified)
                .filterWhen(state -> messageIn.getUserMentions().next()
                        .flatMap(mentioned -> mentioned.asMember(messageIn.getGuildId().get()))
                        .filter(member -> ServerManager.GetInstance().getServerToRoles().containsKey(messageIn.getGuildId().get()) && !ServerManager.GetInstance().getServerToRoles().get(messageIn.getGuildId().get()).isEmpty())
                        .flatMap(member -> Flux.fromIterable(ServerManager.GetInstance().getServerToRoles().get(messageIn.getGuildId().get()))
                                .flatMap(roleId -> member.getRoles().any(role -> role.getId().equals(roleId))) //.map(role -> role.getId().equals(roleId)).filter(containsRole -> containsRole).switchIfEmpty(Flux.just(false)).next())
                                .any(bool -> bool))
                        .doOnNext(hasRole -> {
                            if (!hasRole) {
                                messageIn.getChannel().flatMap(channel -> channel.createMessage("You cannot challenge this person! " + messageIn.getAuthor().map(User::getMention).orElse(""))).subscribe();
                            }
                        })
                        .switchIfEmpty(Mono.just(true)))
                .switchIfEmpty(Mono.just(false));

    }

    public boolean hasStartedChallenge(Snowflake guild, Snowflake member)
    {
        return challenges.containsKey(guild) && challenges.get(guild).stream().anyMatch(tuple -> tuple.getT1().equals(member));
    }

    public boolean hasAcceptedChallenge(Snowflake guild, Snowflake member)
    {
        return challenges.containsKey(guild) && challenges.get(guild).stream().anyMatch(tuple -> tuple.getT2().equals(member) && tuple.getT3());
    }

    public boolean hasChallengedPlayer(Snowflake guild, Snowflake from, Snowflake to)
    {
        return challenges.containsKey(guild) && challenges.get(guild).stream().anyMatch(tuple -> tuple.getT1().equals(from) && tuple.getT2().equals(to) && !tuple.getT3());
    }

    public static ChallengeManager GetInstance()
    {
        return instance;
    }
}
