package com.github.ThomasVDP.tritake;

import com.github.ThomasVDP.tritake.event.BindToChannelEvent;
import com.github.ThomasVDP.tritake.event.ChallengePlayerEvent;
import com.github.ThomasVDP.tritake.event.IEvent;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerManager
{
    private final Map<Snowflake, Snowflake> serverToChannel = new HashMap<>();

    private final List<IEvent> eventListeners = new ArrayList<>();

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
        eventListeners.add(new ChallengePlayerEvent());
    }

    public void handleEvents()
    {
        /*client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(MessageCreateEvent::getMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(message -> message.getContent().equalsIgnoreCase("!ping"))
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("pong!"))
                .subscribe();*/

        this.client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Flux.fromIterable(eventListeners)
                        .filter(listener -> listener.getClassType().equals(MessageCreateEvent.class))
                        .filter(listener -> listener.canExecute(event))
                        .flatMap(listener -> listener.execute(event))
                        .next())
                .subscribe();
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
}
