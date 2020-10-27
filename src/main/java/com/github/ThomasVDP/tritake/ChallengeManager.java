package com.github.ThomasVDP.tritake;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChallengeManager
{
    // tuple<from, to, hasAccepted>
    private final Map<Snowflake, List<Tuple4<Snowflake, Snowflake, Boolean, Disposable>>> challenges = new HashMap<>();

    private static ChallengeManager instance;

    public ChallengeManager()
    {
        if (instance == this)
        {
            System.exit(1);
        }
        instance = this;
    }

    public void CreateNewChallenge(Snowflake guild, Snowflake from, Snowflake to, Message message)
    {
        Disposable scheduledTask =  Schedulers.boundedElastic().schedule(() -> {
            this.challenges.get(guild).removeIf(tuple -> tuple.getT1().equals(from) && !tuple.getT3());
            ServerManager.GetInstance().getClient().getMemberById(guild, from)
                    .flatMap(memberFrom ->  message.getChannel().flatMap(channel -> channel.createMessage("Your challenge was canceled because it ran out of time! " + memberFrom.getMention()))).flatMap(o -> message.delete()).subscribe();
        }, 3, TimeUnit.MINUTES);

        if (this.challenges.containsKey(guild))
        {
            this.challenges.get(guild).add(Tuples.of(from, to, false, scheduledTask));
        } else {
            List<Tuple4<Snowflake, Snowflake, Boolean, Disposable>> list = new ArrayList<>();
            list.add(Tuples.of(from, to, false, scheduledTask));
            this.challenges.put(guild, list);
        }
    }

    public Mono<Object> cancelChallenge(Message message, Snowflake guildId, Member member, boolean autoCancel)
    {
        List<Tuple4<Snowflake, Snowflake, Boolean, Disposable>> serverChallenges = this.challenges.get(guildId);
        if (this.challenges.containsKey(guildId) && serverChallenges.stream().anyMatch(tuple -> tuple.getT1().equals(member.getId()) && !tuple.getT3()))
        {
            if (!autoCancel) {
                serverChallenges.stream().filter(tuple -> tuple.getT1().equals(member.getId()) && !tuple.getT3()).forEach(tuple -> {
                    tuple.getT4().dispose();;
                });
            }
            //serverChallenges.stream().filter(tuple -> tuple.getT1().equals(member.getId()) && !tuple.getT3()).peek(tuple -> tuple.getT4().dispose()).peek(serverChallenges::remove).collect(Collectors.toList());
            serverChallenges.removeIf(tuple -> tuple.getT1().equals(member.getId()) && !tuple.getT3());
            return message.getChannel().flatMap(channel -> channel.createMessage("Your challenge was canceled! " + member.getMention()));
        } else if (this.challenges.containsKey(guildId) && serverChallenges.stream().anyMatch(tuple -> tuple.getT1().equals(member.getId()) && tuple.getT3()))
        {
            //cancel match
            Snowflake from = member.getId();
            Snowflake to = this.challenges.get(guildId).stream().filter(tuple -> tuple.getT1().equals(from)).findFirst().get().getT2();
            this.challenges.get(guildId).stream().filter(tuple -> tuple.getT1().equals(from)).forEach(tuple -> tuple.getT4().dispose());
            this.challenges.get(guildId).removeIf((tuple -> tuple.getT1().equals(from)));
            GameManager.GetInstance().removeGame(guildId, from, to);
            return message.getChannel().flatMap(channel -> ServerManager.GetInstance().getClient().getUserById(from)
                    .flatMap(userFrom -> ServerManager.GetInstance().getClient().getUserById(to)
                            .flatMap(userTo -> channel.createMessage(userFrom.getMention() + " couldn't stand the thought any longer that " + userTo.getMention() + " was gonna beat them and thus left the game!"))));
        } else if (this.challenges.containsKey(guildId) && serverChallenges.stream().anyMatch(tuple -> tuple.getT2().equals(member.getId()) && tuple.getT3()))
        {
            //cancel match
            Snowflake from = this.challenges.get(guildId).stream().filter(tuple -> tuple.getT2().equals(member.getId())).findFirst().get().getT1();
            Snowflake to = member.getId();
            this.challenges.get(guildId).stream().filter(tuple -> tuple.getT1().equals(from)).forEach(tuple -> tuple.getT4().dispose());
            this.challenges.get(guildId).removeIf(tuple -> tuple.getT1().equals(from));
            GameManager.GetInstance().removeGame(guildId, from, to);
            return message.getChannel().flatMap(channel -> ServerManager.GetInstance().getClient().getUserById(from)
                    .flatMap(userFrom -> ServerManager.GetInstance().getClient().getUserById(to)
                            .flatMap(userTo -> channel.createMessage(userTo.getMention() + " thought they could beat " + userFrom.getMention() + " easily but decided to call it off!"))));
        } else {
            return message.getChannel().flatMap(channel -> channel.createMessage("There is nothing to cancel! " + member.getMention()));
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
                .filterWhen(state -> messageIn.getUserMentions()
                        .flatMap(mentioned -> messageIn.getAuthor().map(user -> Mono.just(user.getId().equals(mentioned.getId()))
                        .flatMap(value -> value ? messageIn.getChannel().flatMap(channel -> channel.createMessage("You cannot challenge yourself! " + mentioned.getMention())).map(o -> false) : Mono.just(true))).orElse(Mono.just(false))).next())
                //can't challenge when challenged someone!
                .filterWhen(state -> messageIn.getAuthor().map(user -> hasChallengedPlayer(messageIn.getGuildId().get(), user.getId(), false)
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

    public void acceptChallenge(Member from, Member to)
    {
        Snowflake guildId = from.getGuildId();
        for (int i = 0; i < this.challenges.get(guildId).size(); ++i)
        {
            Tuple4<Snowflake, Snowflake, Boolean, Disposable> element = this.challenges.get(guildId).get(i);
            if (element.getT1().equals(from.getId()) && element.getT2().equals(to.getId())) {
                element.getT4().dispose();
                this.challenges.get(guildId).set(i, element.mapT3(o -> true));
            }
        }
    }

    public boolean hasStartedChallenge(Snowflake guild, Snowflake member)
    {
        return challenges.containsKey(guild) && challenges.get(guild).stream().anyMatch(tuple -> (tuple.getT1().equals(member) || tuple.getT2().equals(member)) && tuple.getT3());
    }

    public boolean hasAcceptedChallenge(Snowflake guild, Snowflake member)
    {
        return challenges.containsKey(guild) && challenges.get(guild).stream().anyMatch(tuple -> tuple.getT2().equals(member) && tuple.getT3());
    }

    public Optional<Snowflake> getChallengerFor(Snowflake guild, Snowflake to)
    {
        return challenges.get(guild).stream().filter(tuple -> tuple.getT2().equals(to)).findFirst().map(Tuple4::getT1);
    }

    public boolean hasChallengedPlayer(Snowflake guild, Snowflake from, boolean checkGameStatus)
    {
        return challenges.containsKey(guild) && challenges.get(guild).stream().anyMatch(tuple -> tuple.getT1().equals(from) && (!checkGameStatus || !tuple.getT3()));
    }

    public List<Tuple4<Snowflake, Snowflake, Boolean, Disposable>> getChallenges(Snowflake guildId)
    {
        return this.challenges.get(guildId);
    }

    public Map<Snowflake, List<Tuple4<Snowflake, Snowflake, Boolean, Disposable>>> getChallengeList()
    {
        return this.challenges;
    }

    public static ChallengeManager GetInstance()
    {
        return instance;
    }
}
