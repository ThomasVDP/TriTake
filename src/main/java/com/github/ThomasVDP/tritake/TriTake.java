package com.github.ThomasVDP.tritake;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;

public class TriTake
{
    public static void main(String[] args)
    {
        GatewayDiscordClient client = DiscordClientBuilder.create(System.getenv("TOKEN"))
                .build()
                .login()
                .block();

        ServerManager serverManager = new ServerManager(client);
        ChallengeManager challengeManager = new ChallengeManager();

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                });

        serverManager.handleEvents();

        client.onDisconnect().block();
    }
}
