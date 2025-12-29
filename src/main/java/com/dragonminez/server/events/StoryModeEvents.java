package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestData;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.quest.objectives.*;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StoryModeEvents {

    @SubscribeEvent
    public static void killObjective(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity killedEntity = event.getEntity();

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            QuestData questData = stats.getQuestData();
            boolean progressUpdated = false;

            for (Map.Entry<String, Saga> entry : SagaManager.getAllSagas().entrySet()) {
                String sagaId = entry.getKey();
                Saga saga = entry.getValue();

                if (!questData.isSagaUnlocked(sagaId)) {
                    continue;
                }

                for (Quest quest : saga.getQuests()) {
                    if (questData.isQuestCompleted(sagaId, quest.getId())) {
                        continue;
                    }

                    for (int i = 0; i < quest.getObjectives().size(); i++) {
                        QuestObjective objective = quest.getObjectives().get(i);

                        if (objective instanceof KillObjective killObjective) {
                            EntityType<?> requiredType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(killObjective.getEntityId()));
                            if (killedEntity.getType().equals(requiredType)) {
                                int currentProgress = questData.getQuestObjectiveProgress(sagaId, quest.getId(), i);
                                int required = killObjective.getRequired();

                                if (currentProgress < required) {
                                    questData.setQuestObjectiveProgress(sagaId, quest.getId(), i, currentProgress + 1);
                                    progressUpdated = true;
                                }
                            }
                        }
                    }
                }
            }

            if (progressUpdated) {
                NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
            }
        });
    }

    @SubscribeEvent
    public static void interactObjective(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            QuestData questData = stats.getQuestData();
            boolean progressUpdated = false;

            for (Map.Entry<String, Saga> entry : SagaManager.getAllSagas().entrySet()) {
                String sagaId = entry.getKey();
                Saga saga = entry.getValue();

                if (!questData.isSagaUnlocked(sagaId)) {
                    continue;
                }

                for (Quest quest : saga.getQuests()) {
                    if (questData.isQuestCompleted(sagaId, quest.getId())) {
                        continue;
                    }

                    for (int i = 0; i < quest.getObjectives().size(); i++) {
                        QuestObjective objective = quest.getObjectives().get(i);

                        if (objective instanceof InteractObjective interactObjective) {
                            EntityType<?> requiredType = interactObjective.getEntityTypeId() != null ? BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(interactObjective.getEntityTypeId())) : null;
                            if (requiredType != null && event.getTarget().getType().equals(requiredType)) {
                                int currentProgress = questData.getQuestObjectiveProgress(sagaId, quest.getId(), i);
                                int required = interactObjective.getRequired();

                                if (currentProgress < required) {
                                    questData.setQuestObjectiveProgress(sagaId, quest.getId(), i, currentProgress + 1);
                                    progressUpdated = true;
                                }
                            }
                        }
                    }
                }
            }

            if (progressUpdated) {
                NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
            }
        });
    }

    private static int tickCounter = 0;
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        tickCounter++;
        if (tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        BlockPos playerPos = player.blockPosition();

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
            QuestData questData = stats.getQuestData();
            boolean progressUpdated = false;

            for (Map.Entry<String, Saga> entry : SagaManager.getAllSagas().entrySet()) {
                String sagaId = entry.getKey();
                Saga saga = entry.getValue();

                if (!questData.isSagaUnlocked(sagaId)) {
                    continue;
                }

                for (Quest quest : saga.getQuests()) {
                    if (questData.isQuestCompleted(sagaId, quest.getId())) {
                        continue;
                    }

                    for (int i = 0; i < quest.getObjectives().size(); i++) {
                        QuestObjective objective = quest.getObjectives().get(i);
                        int currentProgress = questData.getQuestObjectiveProgress(sagaId, quest.getId(), i);
                        int required = objective.getRequired();

                        if (currentProgress >= required) {
                            continue;
                        }

                        if (objective instanceof ItemObjective itemObjective) {
                            Item requiredItem = BuiltInRegistries.ITEM.get(new ResourceLocation(itemObjective.getItemId()));
                            int totalCount = 0;
                            for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
                                if (stack.is(requiredItem)) {
                                    totalCount += stack.getCount();
                                }
                            }
                            if (totalCount >= required && currentProgress < required) {
                                questData.setQuestObjectiveProgress(sagaId, quest.getId(), i, required);
                                progressUpdated = true;
                            } else if (totalCount > currentProgress && totalCount < required) {
                                questData.setQuestObjectiveProgress(sagaId, quest.getId(), i, totalCount);
                                progressUpdated = true;
                            }
                        } else if (objective instanceof BiomeObjective biomeObjective) {
                            ResourceKey<Biome> playerBiome = player.level().getBiome(playerPos).unwrapKey().orElse(null);
                            if (playerBiome != null && playerBiome.location().toString().contains(biomeObjective.getBiomeId())) {
                                questData.setQuestObjectiveProgress(sagaId, quest.getId(), i, required);
                                progressUpdated = true;
                            }
                        } else if (objective instanceof StructureObjective structureObjective) {
                            String structureId = structureObjective.getStructureId();
                            ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> structureKey =
                                ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(structureId));

                            var structureManager = player.serverLevel().structureManager();
                            var structureAt = structureManager.getStructureWithPieceAt(playerPos, structureKey);

                            if (structureAt.isValid()) {
                                questData.setQuestObjectiveProgress(sagaId, quest.getId(), i, required);
                                progressUpdated = true;
                            }
                        } else if (objective instanceof CoordsObjective coordsObjective) {
                            BlockPos targetPos = coordsObjective.getTargetPos();
                            double distance = playerPos.distSqr(targetPos);

                            if (distance <= 25) {
                                questData.setQuestObjectiveProgress(sagaId, quest.getId(), i, required);
                                progressUpdated = true;
                            }
                        }
                    }
                }
            }

            if (progressUpdated) {
                NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
            }
        });
    }
}
