package com.dragonminez.common.network.S2C;

import com.dragonminez.common.config.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;

public class SyncServerConfigS2C {

    private static final Gson GSON = new GsonBuilder().create();
    private final String generalServerJson;
    private final String skillsJson;
    private final String formsJson;
    private final String raceStatsJson;
    private final String raceCharacterJson;

    public SyncServerConfigS2C(GeneralServerConfig serverConfig, SkillsConfig skillsConfig, Map<String, Map<String, FormConfig>> formsConfigs, Map<String, RaceStatsConfig> statsConfigs, Map<String, RaceCharacterConfig> characterConfigs) {
        this.generalServerJson = GSON.toJson(serverConfig);
        this.skillsJson = GSON.toJson(skillsConfig);
        this.formsJson = GSON.toJson(formsConfigs);
        this.raceStatsJson = GSON.toJson(statsConfigs);
        this.raceCharacterJson = GSON.toJson(characterConfigs);
    }

    public SyncServerConfigS2C(FriendlyByteBuf buf) {
        this.generalServerJson = buf.readUtf(1048576);
        this.skillsJson = buf.readUtf(1048576);
        this.formsJson = buf.readUtf(1048576);
        this.raceStatsJson = buf.readUtf(1048576);
        this.raceCharacterJson = buf.readUtf(1048576);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(generalServerJson, 1048576);
        buf.writeUtf(skillsJson,1048576 );
        buf.writeUtf(formsJson, 1048576);
        buf.writeUtf(raceStatsJson, 1048576);
        buf.writeUtf(raceCharacterJson, 1048576);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                GeneralServerConfig serverConfig = GSON.fromJson(generalServerJson, GeneralServerConfig.class);
                SkillsConfig skillsConfig = GSON.fromJson(skillsJson, SkillsConfig.class);

                Type formsType = new TypeToken<Map<String, Map<String, FormConfig>>>() {}.getType();
                Map<String, Map<String, FormConfig>> formsConfigs = GSON.fromJson(formsJson, formsType);

                Type statsType = new TypeToken<Map<String, RaceStatsConfig>>() {}.getType();
                Map<String, RaceStatsConfig> statsConfigs = GSON.fromJson(raceStatsJson, statsType);

                Type characterType = new TypeToken<Map<String, RaceCharacterConfig>>() {}.getType();
                Map<String, RaceCharacterConfig> characterConfigs = GSON.fromJson(raceCharacterJson, characterType);

                ConfigManager.applySyncedServerConfig(serverConfig, skillsConfig, formsConfigs, statsConfigs, characterConfigs);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
