package com.github.ThomasVDP.tritake;

import com.github.ThomasVDP.tritake.event.*;
import com.github.ThomasVDP.tritake.event.game.GameMoveEvent;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerManager
{
    private Map<Snowflake, Snowflake> serverToChannel = new HashMap<>();
    private Map<Snowflake, List<Snowflake>> serverToRoles = new HashMap<>();
    private final Map<Snowflake, Disposable> channelWaitingList = new HashMap<>();

    private final List<IEvent<? extends Event>> eventListeners = new ArrayList<>();

    private final GatewayDiscordClient client;
    private static ServerManager instance;

    public ServerManager(GatewayDiscordClient client)
    {
        if (instance == this)
        {
            System.exit(1);
        }
        instance = this;

        this.client = client;

        eventListeners.add(new LeaveGuildEvent());
        eventListeners.add(new HelpCommandEvent());

        eventListeners.add(new BindToChannelEvent());
        eventListeners.add(new ConfirmChannelEvent());
        eventListeners.add(new AssignRoleForChallengesEvent());
        eventListeners.add(new RemoveRoleForChallengesEvent());

        eventListeners.add(new ChallengePlayerEvent());
        eventListeners.add(new CancelChallengeEvent());

        eventListeners.add(new ChallengeReactionEvent());

        eventListeners.add(new GameMoveEvent());
    }

    @SuppressWarnings("unchecked")
    public void handleEvents()
    {
        this.client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Flux.fromIterable(eventListeners)
                        .filter(listener -> listener.getClassType().equals(MessageCreateEvent.class))
                        .filterWhen(listener -> ((IEvent<MessageCreateEvent>)listener).canExecute(event))
                        .flatMap(listener -> ((IEvent<MessageCreateEvent>)listener).execute(event))
                        .next())
                .subscribe();

        this.client.getEventDispatcher().on(ReactionAddEvent.class)
                .flatMap(event -> Flux.fromIterable(eventListeners)
                        .filter(listener -> listener.getClassType().equals(ReactionAddEvent.class))
                        .filterWhen(listener -> ((IEvent<ReactionAddEvent>)listener).canExecute(event))
                        .flatMap(listener -> ((IEvent<ReactionAddEvent>)listener).execute(event))
                ).subscribe();

        this.client.getEventDispatcher().on(GuildDeleteEvent.class)
                .flatMap(event -> Flux.fromIterable(eventListeners)
                        .filter(listener -> listener.getClassType().equals(GuildDeleteEvent.class))
                        .filterWhen(listener -> ((IEvent<GuildDeleteEvent>)listener).canExecute(event))
                        .flatMap(listener -> ((IEvent<GuildDeleteEvent>)listener).execute(event))
                ).subscribe();
    }

    public static ServerManager GetInstance()
    {
        return instance;
    }

    public GatewayDiscordClient getClient()
    {
        return this.client;
    }

    public Map<Snowflake, Snowflake> getServerToChannel()
    {
        return this.serverToChannel;
    }
    public void setServerToChannel(Map<Snowflake, Snowflake> map)
    {
        this.serverToChannel = map;
    }

    public Map<Snowflake, List<Snowflake>> getServerToRoles()
    {
        return this.serverToRoles;
    }
    public void setServerToRoles(Map<Snowflake, List<Snowflake>> map)
    {
        this.serverToRoles = map;
    }

    public Map<Snowflake, Disposable> getChannelWaitingList()
    {
        return this.channelWaitingList;
    }
}
