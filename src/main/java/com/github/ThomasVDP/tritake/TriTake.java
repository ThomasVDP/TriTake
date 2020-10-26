package com.github.ThomasVDP.tritake;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import reactor.core.scheduler.Schedulers;

public class TriTake
{
    public static void main(String[] args)
    {
        if (args.length < 1) {
            System.out.println("Need at least one backup filename specified");
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> BackupManager.backup("shutdown-backup.json")));

        EventDispatcher customDispatcher = EventDispatcher.builder()
                .eventScheduler(Schedulers.immediate())
                .build();

        GatewayDiscordClient client = DiscordClientBuilder.create(System.getenv("TOKEN"))
                .build()
                .gateway()
                .setEventDispatcher(customDispatcher)
                .login()
                .block();

        ServerManager serverManager = new ServerManager(client);
        ChallengeManager challengeManager = new ChallengeManager();
        GameManager gameManager = new GameManager();
        BackupManager backupManager = new BackupManager();
        backupManager.readBackupFile(args[0]);

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                });

        serverManager.handleEvents();

        client.onDisconnect().block();
    }
}
