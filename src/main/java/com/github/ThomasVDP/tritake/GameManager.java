package com.github.ThomasVDP.tritake;

import com.github.ThomasVDP.tritake.game.GameLayout;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GameManager
{
    private final Map<Snowflake, List<Tuple4<Snowflake, Snowflake, Message, GameLayout>>> games = new HashMap<>();

    private static GameManager instance;

    public GameManager()
    {
        if (instance == this)
        {
            System.exit(1);
        }
        instance = this;
    }

    public static GameManager GetInstance()
    {
        return instance;
    }

    public void CreateGame(Mono<MessageChannel> channel, Member from, Member to)
    {
        this.CreateGame(channel, from, to, 5);
    }

    public void CreateGame(Mono<MessageChannel> messageChannel, Member from, Member to, int size)
    {
        ChallengeManager.GetInstance().acceptChallenge(from, to);

        GameLayout gameLayout = new GameLayout(size);

        messageChannel.flatMap(channel -> channel.createMessage(to.getMention() + " accepted " + from.getMention() + "'s challenge!\nThe game is on!")).subscribe();
        messageChannel.flatMap(channel -> channel.createEmbed(embedCreateSpec -> embedCreateSpec.setTitle(from.getDisplayName() + " vs " + to.getDisplayName())
                        .setColor(Color.DISCORD_WHITE)
                        .setDescription("A TriTake game...")
                        .setFooter(gameLayout.hasWon().getT3() ? "(It's " + from.getDisplayName() + "'s turn)" : "(It's " + to.getDisplayName() + "'s turn)", null)
                        .addField("Board:", gameLayout.getBoardLayoutForMessage(), false)
        )).doOnSuccess(msg -> {
                //create the game
                Tuple4<Snowflake, Snowflake, Message, GameLayout> obj = Tuples.of(from.getId(), to.getId(), msg, gameLayout);
                if (this.games.containsKey(from.getGuildId()))
                {
                    this.games.get(from.getGuildId()).add(obj);
                } else {
                    List<Tuple4<Snowflake, Snowflake, Message, GameLayout>> list = new ArrayList<>();
                    list.add(obj);
                    this.games.put(from.getGuildId(), list);
                }
        }).subscribe();

        List<Tuple4<Snowflake, Snowflake, Boolean, Disposable>> challenges = ChallengeManager.GetInstance().getChallenges(from.getGuildId());
        for (int i = 0; i < challenges.size(); ++i)
        {
            Tuple4<Snowflake, Snowflake, Boolean, Disposable> element = challenges.get(i);
            if (element.getT1().equals(from.getId())) {
                challenges.set(i, element.mapT4(o ->
                    Schedulers.boundedElastic().schedule(() -> {
                        GameManager.GetInstance().removeGame(from.getGuildId(), from.getId(), to.getId());
                        ChallengeManager.GetInstance().getChallenges(from.getGuildId()).removeIf(tuple -> tuple.getT1().equals(from.getId()));
                        messageChannel.flatMap(channel -> channel.createMessage("The game has ended due to inactivity! " + from.getMention() + " " + to.getMention())).subscribe();
                    }, 5, TimeUnit.MINUTES)
                ));
            }
        }
    }

    public Tuple4<Snowflake, Snowflake, Message, GameLayout> getGameFor(Member member)
    {
        return this.games.get(member.getGuildId()).stream().filter(tuple -> tuple.getT1().equals(member.getId()) || tuple.getT2().equals(member.getId())).findFirst().get();
    }

    public void executeMove(Member member, MessageCreateEvent event)
    {
        Tuple4<Snowflake, Snowflake, Message, GameLayout> gameData = this.getGameFor(member);

        if (gameData.getT4().getWhosTurn() == gameData.getT1().equals(member.getId()))
        {
            String[] parts = event.getMessage().getContent().split(" ");
            if (gameData.getT4().move(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])))
            {
                (!gameData.getT1().equals(member.getId())
                        ? ServerManager.GetInstance().getClient().getUserById(gameData.getT1()).flatMap(user -> user.asMember(member.getGuildId()))
                        : ServerManager.GetInstance().getClient().getUserById(gameData.getT2()).flatMap(user -> user.asMember(member.getGuildId()))
                ).flatMap(user -> gameData.getT3().edit(messageEditSpec -> messageEditSpec.setEmbed(embedCreateSpec ->
                        embedCreateSpec.setTitle((gameData.getT1().equals(member.getId()) ? member.getDisplayName() : user.getDisplayName()) + " vs " + (gameData.getT1().equals(member.getId()) ? user.getDisplayName() : member.getDisplayName()))
                                .setColor(Color.DISCORD_WHITE)
                                .setDescription("A TriTake game...")
                                .setFooter("(It's " + user.getDisplayName() + "'s turn)", null)
                                .addField("Board:", gameData.getT4().getBoardLayoutForMessage(), false)))).subscribe();
            } else {
                event.getMessage().getChannel().flatMap(channel -> channel.createMessage("Invalid move!")
                        .flatMap(message -> Mono.fromRunnable(() -> Schedulers.boundedElastic().schedule(() -> message.delete().subscribe(), 3, TimeUnit.SECONDS)))).subscribe();
            }
        } else {
            event.getMessage().getChannel().flatMap(channel -> channel.createMessage("It is not your turn! " + event.getMember().map(User::getMention).orElse(""))
                    .flatMap(message -> Mono.fromRunnable(() -> Schedulers.boundedElastic().schedule(() -> message.delete().subscribe(), 3, TimeUnit.SECONDS)))).subscribe();
        }

        checkWinStatus(member, event);
        event.getMessage().delete().subscribe();
    }

    private void checkWinStatus(Member member, MessageCreateEvent event)
    {
        Tuple4<Snowflake, Snowflake, Message, GameLayout> gameData = this.games.get(member.getGuildId()).stream().filter(tuple -> tuple.getT1().equals(member.getId()) || tuple.getT2().equals(member.getId())).findFirst().get();
        Tuple3<Boolean, Boolean, Boolean> winStatus = gameData.getT4().hasWon();
        if (winStatus.getT1() || winStatus.getT2())
        {
            ChallengeManager.GetInstance().getChallenges(member.getGuildId()).stream().filter(tuple -> tuple.getT1().equals(gameData.getT1())).forEach(tuple -> tuple.getT4().dispose());
            ChallengeManager.GetInstance().getChallenges(member.getGuildId()).removeIf(tuple -> tuple.getT1().equals(gameData.getT1()));
            this.removeGame(member.getGuildId(), gameData.getT1(), gameData.getT2());
            event.getMessage().getChannel().flatMap(channel -> ServerManager.GetInstance().getClient().getUserById(gameData.getT1())
                    .flatMap(userFrom -> ServerManager.GetInstance().getClient().getUserById(gameData.getT2())
                            .flatMap(userTo -> !winStatus.getT3() && !winStatus.getT2()
                                    ? channel.createMessage("Unfortunately, " + userTo.getMention() + " couldn't make their dream come true and lost from " + userFrom.getMention() + "!")
                                    : channel.createMessage("Yayy! " + userTo.getMention() + " seems to have known this would happen and totally destroyed " + userFrom.getMention() + "!")))).subscribe();
        } else {
            List<Tuple4<Snowflake, Snowflake, Boolean, Disposable>> challenges = ChallengeManager.GetInstance().getChallenges(member.getGuildId());
            for (int i = 0; i < challenges.size(); ++i)
            {
                Tuple4<Snowflake, Snowflake, Boolean, Disposable> element = challenges.get(i);
                if (element.getT1().equals(member.getId()) || element.getT2().equals(member.getId()))
                {
                    element.getT4().dispose();
                    challenges.set(i, element.mapT4(o ->
                            Schedulers.boundedElastic().schedule(() -> {
                                GameManager.GetInstance().removeGame(member.getGuildId(), element.getT1(), element.getT2());
                                ChallengeManager.GetInstance().getChallenges(member.getGuildId()).removeIf(tuple -> tuple.getT1().equals(element.getT1()));
                                event.getMessage().getChannel().flatMap(channel -> ServerManager.GetInstance().getClient().getUserById(element.getT1())
                                        .flatMap(userFrom -> ServerManager.GetInstance().getClient().getUserById(element.getT2())
                                                .flatMap(userTo -> channel.createMessage("The game has ended due to inactivity! " + userFrom.getMention() + " " + userTo.getMention())))).subscribe();
                            }, 5, TimeUnit.MINUTES)
                    ));
                }
            }
        }
    }

    public void removeGame(Snowflake guildId, Snowflake from, Snowflake to)
    {
        this.games.get(guildId).removeIf(tuple -> tuple.getT1().equals(from) && tuple.getT2().equals(to));
    }

    public Map<Snowflake, List<Tuple4<Snowflake, Snowflake, Message, GameLayout>>> getGames()
    {
        return this.games;
    }
}
