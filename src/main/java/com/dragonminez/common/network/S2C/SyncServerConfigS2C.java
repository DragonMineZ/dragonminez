package com.dragonminez.common.network.S2C;

import com.dragonminez.common.config.*;
import com.dragonminez.common.network.CompressionUtil;
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
    private final byte[] generalServerBytes;
    private final byte[] skillsBytes;
    private final byte[] skillOfferingsBytes;
    private final byte[] formsBytes;
    private final byte[] raceStatsBytes;
    private final byte[] raceCharacterBytes;

    public SyncServerConfigS2C(GeneralServerConfig serverConfig, SkillsConfig skillsConfig, MasterSkillsOfferingConfig skillsOfferingConfig, Map<String, Map<String, FormConfig>> formsConfigs, Map<String, RaceStatsConfig> statsConfigs, Map<String, RaceCharacterConfig> characterConfigs) {
        this.generalServerBytes = CompressionUtil.compress(GSON.toJson(serverConfig));
        this.skillsBytes = CompressionUtil.compress(GSON.toJson(skillsConfig));
        this.skillOfferingsBytes = CompressionUtil.compress(GSON.toJson(skillsOfferingConfig));
        this.formsBytes = CompressionUtil.compress(GSON.toJson(formsConfigs));
        this.raceStatsBytes = CompressionUtil.compress(GSON.toJson(statsConfigs));
        this.raceCharacterBytes = CompressionUtil.compress(GSON.toJson(characterConfigs));
    }

    public SyncServerConfigS2C(FriendlyByteBuf buf) {
        this.generalServerBytes = buf.readByteArray();
        this.skillsBytes = buf.readByteArray();
        this.skillOfferingsBytes = buf.readByteArray();
        this.formsBytes = buf.readByteArray();
        this.raceStatsBytes = buf.readByteArray();
        this.raceCharacterBytes = buf.readByteArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(generalServerBytes);
        buf.writeByteArray(skillsBytes);
        buf.writeByteArray(skillOfferingsBytes);
        buf.writeByteArray(formsBytes);
        buf.writeByteArray(raceStatsBytes);
        buf.writeByteArray(raceCharacterBytes);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            String generalServerJson = CompressionUtil.decompress(generalServerBytes);
            String skillsJson = CompressionUtil.decompress(skillsBytes);
            String skillOfferingsJson = CompressionUtil.decompress(skillOfferingsBytes);
            String formsJson = CompressionUtil.decompress(formsBytes);
            String raceStatsJson = CompressionUtil.decompress(raceStatsBytes);
            String raceCharacterJson = CompressionUtil.decompress(raceCharacterBytes);

            GeneralServerConfig serverConfig = GSON.fromJson(generalServerJson, GeneralServerConfig.class);
            SkillsConfig skillsConfig = GSON.fromJson(skillsJson, SkillsConfig.class);
            MasterSkillsOfferingConfig skillsOfferingConfig = GSON.fromJson(skillOfferingsJson, MasterSkillsOfferingConfig.class);

            Type formsType = new TypeToken<Map<String, Map<String, FormConfig>>>() {}.getType();
            Map<String, Map<String, FormConfig>> formsConfigs = GSON.fromJson(formsJson, formsType);

            Type statsType = new TypeToken<Map<String, RaceStatsConfig>>() {}.getType();
            Map<String, RaceStatsConfig> statsConfigs = GSON.fromJson(raceStatsJson, statsType);

            Type characterType = new TypeToken<Map<String, RaceCharacterConfig>>() {}.getType();
            Map<String, RaceCharacterConfig> characterConfigs = GSON.fromJson(raceCharacterJson, characterType);

            ConfigManager.applySyncedServerConfig(serverConfig, skillsConfig, skillsOfferingConfig, formsConfigs, statsConfigs, characterConfigs);
        }));
        ctx.get().setPacketHandled(true);
    }
}