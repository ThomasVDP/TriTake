package com.github.ThomasVDP.tritake;

import com.github.ThomasVDP.tritake.json.SnowflakeDeserializer;
import com.github.ThomasVDP.tritake.json.SnowflakeSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import discord4j.common.util.Snowflake;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BackupManager
{
    public static Gson gson = new GsonBuilder()
            .registerTypeAdapter(Snowflake.class, new SnowflakeSerializer())
            .registerTypeAdapter(Snowflake.class, new SnowflakeDeserializer())
            .enableComplexMapKeySerialization()
            .create();

    public BackupManager()
    {
        Schedulers.boundedElastic().schedule(() -> {
            Schedulers.boundedElastic().schedulePeriodically(() -> {
                backup("backup-long.json");
            }, 0, 1, TimeUnit.HOURS);
            Schedulers.boundedElastic().schedulePeriodically(() -> {
                backup("backup-short.json");
            }, 0, 10, TimeUnit.MINUTES);
        }, 10, TimeUnit.SECONDS);
    }

    public static void backup(String fileName)
    {
        try
        {
            FileWriter writer = new FileWriter(fileName);
            Tuple2<Map<Snowflake, Snowflake>, Map<Snowflake, List<Snowflake>>> data = Tuples.of(ServerManager.GetInstance().getServerToChannel(), ServerManager.GetInstance().getServerToRoles());
            writer.write(gson.toJson(data));
            writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void readBackupFile(String fileName)
    {
        File backupFile = new File(fileName);
        try {
            if (!backupFile.exists()) return;

            Type dataType = new TypeToken<Tuple2<Map<Snowflake, Snowflake>, Map<Snowflake, List<Snowflake>>>>(){}.getType();
            String fileContent = new String(Files.readAllBytes(backupFile.toPath()));
            Tuple2<Map<Snowflake, Snowflake>, Map<Snowflake, List<Snowflake>>> tuple = gson.fromJson(fileContent, dataType);
            ServerManager.GetInstance().setServerToChannel(tuple.getT1());
            ServerManager.GetInstance().setServerToRoles(tuple.getT2());
        }
        catch (IOException e) {
            //e.printStackTrace();
        }
    }
}
