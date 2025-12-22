package com.dragonminez.common.stats;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.SyncServerConfigS2C;
import com.dragonminez.common.quest.QuestData;
import com.dragonminez.common.quest.SagaManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class StatsCapability {
    public static final Capability<StatsData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(StatsData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            if (!player.getCapability(INSTANCE).isPresent()) {
                event.addCapability(StatsProvider.ID, new StatsProvider(player));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player player = event.getEntity();
        Player original = event.getOriginal();

        original.reviveCaps();

        StatsProvider.get(INSTANCE, player).ifPresent(newData ->
                StatsProvider.get(INSTANCE, original).ifPresent(newData::copyFrom)
        );

        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            Map<String, Map<String, FormConfig>> allForms = new HashMap<>();
            ConfigManager.getLoadedRaces().forEach(raceName -> {
                allForms.put(raceName, ConfigManager.getAllFormsForRace(raceName));
            });

            NetworkHandler.sendToPlayer(
                new SyncServerConfigS2C(
                    ConfigManager.getAllRaceStats(),
                    ConfigManager.getAllRaceCharacters(),
                    allForms,
                    ConfigManager.getServerConfig(),
                    ConfigManager.getSkillsConfig()
                ),
                serverPlayer
            );

            SagaManager.loadSagas(serverPlayer.getServer());

        }
        event.getEntity().refreshDimensions();
    }

    private static boolean hasInitializedPlayer = false;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            if (event.player instanceof ServerPlayer serverPlayer && !hasInitializedPlayer) {
                hasInitializedPlayer = true;

                StatsProvider.get(INSTANCE, serverPlayer).ifPresent(data -> {
                    QuestData questData = data.getQuestData();

                    boolean saiyanUnlocked = questData.isSagaUnlocked("saiyan_saga");
                    boolean quest1Completed = questData.isQuestCompleted("saiyan_saga", 1);

                    if (!questData.isSagaUnlocked("saiyan_saga")) {
                        questData.unlockSaga("saiyan_saga");
                    }

                    NetworkHandler.sendToPlayer(new StatsSyncS2C(serverPlayer), serverPlayer);
                });
            }

            StatsProvider.get(INSTANCE, event.player).ifPresent(StatsData::tick);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        StatsProvider.get(INSTANCE, event.getEntity()).ifPresent(data -> {
            data.getResources().setCurrentEnergy(data.getMaxEnergy());
            data.getResources().setCurrentStamina(data.getMaxStamina());
            data.getStatus().setAlive(true);
        });
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            StatsProvider.get(INSTANCE, serverPlayer).ifPresent(data -> {
                NetworkHandler.sendToPlayer(new StatsSyncS2C(serverPlayer), serverPlayer);
            });
        }
    }
}

