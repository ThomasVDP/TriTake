package com.github.ThomasVDP.tritake;

import com.github.ThomasVDP.tritake.event.*;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerManager
{
    private final Map<Snowflake, Snowflake> serverToChannel = new HashMap<>();
    private final Map<Snowflake, List<Snowflake>> serverToRoles = new HashMap<>();

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

        eventListeners.add(new BindToChannelEvent());
        eventListeners.add(new AssignRoleForChallengesEvent());
        eventListeners.add(new RemoveRoleForChallengesEvent());
        eventListeners.add(new ChallengePlayerEvent());

        eventListeners.add(new ChallengeReactionEvent());
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

    public Map<Snowflake, List<Snowflake>> getServerToRoles()
    {
        return this.serverToRoles;
    }
}
