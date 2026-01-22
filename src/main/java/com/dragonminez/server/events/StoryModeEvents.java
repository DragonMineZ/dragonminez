package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.*;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StoryModeEvents {

	private static void updatePartyProgress(ServerPlayer sourcePlayer, String sagaId, Quest quest, int objIndex, int newProgress) {
		List<ServerPlayer> members = PartyManager.getAllPartyMembers(sourcePlayer);

		for (ServerPlayer member : members) {
			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(stats -> {
				QuestData questData = stats.getQuestData();
				if (questData.isSagaUnlocked(sagaId) && !questData.isQuestCompleted(sagaId, quest.getId())) {
					int currentProgress = questData.getQuestObjectiveProgress(sagaId, quest.getId(), objIndex);
					if (currentProgress < newProgress) {
						questData.setQuestObjectiveProgress(sagaId, quest.getId(), objIndex, newProgress);
						NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(member), member);

						boolean isQuestNowComplete = true;
						for (int i = 0; i < quest.getObjectives().size(); i++) {
							if (questData.getQuestObjectiveProgress(sagaId, quest.getId(), i) < quest.getObjectives().get(i).getRequired()) {
								isQuestNowComplete = false;
								break;
							}
						}

						if (isQuestNowComplete) {
							questData.completeQuest(sagaId, quest.getId());
							MinecraftForge.EVENT_BUS.post(new DMZEvent.QuestCompleteEvent(member, SagaManager.getSaga(sagaId), quest, members));
						}
					}
				}
			});
		}
	}

	@SubscribeEvent
	public static void killObjective(LivingDeathEvent event) {
		if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
		LivingEntity killedEntity = event.getEntity();
		for (Map.Entry<String, Saga> entry : SagaManager.getAllSagas().entrySet()) {
			String sagaId = entry.getKey();
			Saga saga = entry.getValue();

			for (Quest quest : saga.getQuests()) {
				for (int i = 0; i < quest.getObjectives().size(); i++) {
					QuestObjective objective = quest.getObjectives().get(i);

					if (objective instanceof KillObjective killObjective) {
						EntityType<?> requiredType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(killObjective.getEntityId()));
						if (killedEntity.getType().equals(requiredType)) {
							int finalI = i;
							StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
								QuestData qd = stats.getQuestData();
								if (qd.isSagaUnlocked(sagaId) && !qd.isQuestCompleted(sagaId, quest.getId())) {
									int current = qd.getQuestObjectiveProgress(sagaId, quest.getId(), finalI);
									int required = killObjective.getRequired();

									if (current < required) {
										updatePartyProgress(player, sagaId, quest, finalI, current + 1);
									}
								}
							});
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void interactObjective(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		for (Map.Entry<String, Saga> entry : SagaManager.getAllSagas().entrySet()) {
			String sagaId = entry.getKey();
			Saga saga = entry.getValue();

			for (Quest quest : saga.getQuests()) {
				for (int i = 0; i < quest.getObjectives().size(); i++) {
					QuestObjective objective = quest.getObjectives().get(i);

					if (objective instanceof InteractObjective interactObjective) {
						EntityType<?> requiredType = interactObjective.getEntityTypeId() != null ? BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(interactObjective.getEntityTypeId())) : null;

						if (requiredType != null && event.getTarget().getType().equals(requiredType)) {
							int finalI = i;
							StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
								QuestData qd = stats.getQuestData();
								if (qd.isSagaUnlocked(sagaId) && !qd.isQuestCompleted(sagaId, quest.getId())) {
									int current = qd.getQuestObjectiveProgress(sagaId, quest.getId(), finalI);
									if (current < interactObjective.getRequired()) {
										updatePartyProgress(player, sagaId, quest, finalI, current + 1);
									}
								}
							});
						}
					}
				}
			}
		}
	}

	private static int tickCounter = 0;

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
		if (!(event.player instanceof ServerPlayer player)) return;

		if (player.tickCount % 20 != 0) return;

		BlockPos playerPos = player.blockPosition();

		for (Map.Entry<String, Saga> entry : SagaManager.getAllSagas().entrySet()) {
			String sagaId = entry.getKey();
			Saga saga = entry.getValue();

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				QuestData qd = stats.getQuestData();
				if (!qd.isSagaUnlocked(sagaId)) return;

				for (Quest quest : saga.getQuests()) {
					if (qd.isQuestCompleted(sagaId, quest.getId())) continue;

					for (int i = 0; i < quest.getObjectives().size(); i++) {
						QuestObjective objective = quest.getObjectives().get(i);

						if (qd.getQuestObjectiveProgress(sagaId, quest.getId(), i) >= objective.getRequired()) continue;

						boolean conditionMet = false;
						int progressToSet = objective.getRequired();

						if (objective instanceof ItemObjective itemObjective) {
							try {
								Item requiredItem = BuiltInRegistries.ITEM.get(new ResourceLocation(itemObjective.getItemId()));
								int totalCount = 0;
								for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
									if (stack.is(requiredItem)) totalCount += stack.getCount();
								}
								if (totalCount >= objective.getRequired()) {
									conditionMet = true;
								} else if (totalCount > qd.getQuestObjectiveProgress(sagaId, quest.getId(), i)) {
									conditionMet = true;
									progressToSet = totalCount;
								}
							} catch (Exception ignored) {}
						}

						else if (objective instanceof BiomeObjective biomeObjective) {
							try {
								ResourceKey<Biome> playerBiomeKey = player.level().getBiome(playerPos).unwrapKey().orElse(null);
								if (playerBiomeKey != null) {
									ResourceLocation targetBiome = new ResourceLocation(biomeObjective.getBiomeId());
									if (playerBiomeKey.location().equals(targetBiome)) {
										conditionMet = true;
									}
								}
							} catch (Exception ignored) {}
						}

						else if (objective instanceof StructureObjective structureObjective) {
							try {
								ResourceLocation structLoc = new ResourceLocation(structureObjective.getStructureId());
								ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> structureKey =
										ResourceKey.create(Registries.STRUCTURE, structLoc);

								var structureManager = player.serverLevel().structureManager();
								if (structureManager.getStructureWithPieceAt(playerPos, structureKey).isValid()) {
									conditionMet = true;
								}
							} catch (Exception ignored) {}
						}

						else if (objective instanceof CoordsObjective coordsObjective) {
							if (playerPos.distSqr(coordsObjective.getTargetPos()) <= (coordsObjective.getRadius() * coordsObjective.getRadius())) {
								conditionMet = true;
							}
						}

						if (conditionMet) {
							updatePartyProgress(player, sagaId, quest, i, progressToSet);
						}
					}
					break;
				}
			});
		}
	}
}