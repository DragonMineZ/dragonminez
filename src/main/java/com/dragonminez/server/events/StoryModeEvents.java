package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.*;
import com.dragonminez.common.quest.objectives.*;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

/**
 * Handles saga quest progression events (tick, kill, interact).
 * Uses {@link QuestRegistry} for saga/quest lookups and {@link PlayerQuestData} for progress.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class StoryModeEvents {

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
		if (event.player.tickCount % 20 != 0) return;
		if (!(event.player instanceof ServerPlayer player)) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			PlayerQuestData pqd = data.getPlayerQuestData();
			Map<String, Saga> allSagas = QuestRegistry.getAllSagas();

			for (Map.Entry<String, Saga> entry : allSagas.entrySet()) {
				String sagaId = entry.getKey();
				Saga saga = entry.getValue();
				if (!pqd.isSagaUnlocked(sagaId)) continue;

				List<Quest> sagaQuests = saga.getQuests();
				for (int questIndex = 0; questIndex < sagaQuests.size(); questIndex++) {
					Quest activeQuest = sagaQuests.get(questIndex);
					if (!shouldTrackSagaQuest(data, pqd, saga, activeQuest, questIndex)) continue;

					int questId = activeQuest.getId();
					String questKey = PlayerQuestData.sagaQuestKey(sagaId, questId);
					int currentObjIndex = -1;
					QuestObjective objective = null;

					for (int i = 0; i < activeQuest.getObjectives().size(); i++) {
						QuestObjective tempObj = activeQuest.getObjectives().get(i);
						int currentProgress = pqd.getObjectiveProgress(questKey, i);
						if (currentProgress < tempObj.getRequired()) {
							currentObjIndex = i;
							objective = tempObj;
							break;
						}
					}

					if (objective == null || currentObjIndex == -1) continue;
					boolean isLocationObjective = QuestLocationHelper.isLocationObjective(objective);

					if (isLocationObjective) {
						List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(player);
						boolean anyMemberInZone = false;
						for (ServerPlayer member : partyMembers) {
							if (QuestLocationHelper.isLocationConditionMet(member, objective)) {
								anyMemberInZone = true;
								break;
							}
						}
						int targetProgress = anyMemberInZone ? 1 : 0;
						updatePartyState(partyMembers, sagaId, questId, currentObjIndex, targetProgress);
					} else if (objective instanceof ItemObjective itemObjective) {
						int itemCount = countItems(player, itemObjective.getItemId());
						int savedProgress = pqd.getObjectiveProgress(questKey, currentObjIndex);
						if (itemCount != savedProgress) {
							int progressToSet = Math.min(itemCount, itemObjective.getRequired());
							updateIndividualProgress(player, sagaId, questId, currentObjIndex, progressToSet);
						}
					}
				}
			}
		});
	}

	@SubscribeEvent
	public static void onEntityKill(LivingDeathEvent event) {
		if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
		LivingEntity killedEntity = event.getEntity();
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(killer);
		for (ServerPlayer member : partyMembers) {
			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				Map<String, Saga> allSagas = QuestRegistry.getAllSagas();

				for (Map.Entry<String, Saga> entry : allSagas.entrySet()) {
					String sagaId = entry.getKey();
					Saga saga = entry.getValue();
					if (!pqd.isSagaUnlocked(sagaId)) continue;

					List<Quest> sagaQuests = saga.getQuests();
					for (int questIndex = 0; questIndex < sagaQuests.size(); questIndex++) {
						Quest activeQuest = sagaQuests.get(questIndex);
						if (!shouldTrackSagaQuest(data, pqd, saga, activeQuest, questIndex)) continue;

						int questId = activeQuest.getId();
						String questKey = PlayerQuestData.sagaQuestKey(sagaId, questId);

						for (int i = 0; i < activeQuest.getObjectives().size(); i++) {
							QuestObjective objective = activeQuest.getObjectives().get(i);
							int currentProgress = pqd.getObjectiveProgress(questKey, i);
							if (currentProgress >= objective.getRequired()) continue;

							if (objective instanceof KillObjective killObjective) {
								ResourceLocation targetId = ResourceLocation.parse(killObjective.getEntityId());
								EntityType<?> requiredType = BuiltInRegistries.ENTITY_TYPE.get(targetId);
								if (killedEntity.getType().equals(requiredType)) updateIndividualProgress(member, sagaId, questId, i, currentProgress + 1);
							}
						}
					}
				}
			});
		}
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getEntity() instanceof ServerPlayer interactor)) return;
		if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(interactor);

		for (ServerPlayer member : partyMembers) {
			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				Map<String, Saga> allSagas = QuestRegistry.getAllSagas();

				for (Map.Entry<String, Saga> entry : allSagas.entrySet()) {
					String sagaId = entry.getKey();
					Saga saga = entry.getValue();
					if (!pqd.isSagaUnlocked(sagaId)) continue;

					List<Quest> sagaQuests = saga.getQuests();
					for (int questIndex = 0; questIndex < sagaQuests.size(); questIndex++) {
						Quest activeQuest = sagaQuests.get(questIndex);
						if (!shouldTrackSagaQuest(data, pqd, saga, activeQuest, questIndex)) continue;

						int questId = activeQuest.getId();
						String questKey = PlayerQuestData.sagaQuestKey(sagaId, questId);

						for (int i = 0; i < activeQuest.getObjectives().size(); i++) {
							QuestObjective objective = activeQuest.getObjectives().get(i);
							int currentProgress = pqd.getObjectiveProgress(questKey, i);
							if (currentProgress >= objective.getRequired()) continue;
							if (objective instanceof InteractObjective interactObjective) {
								String targetStr = interactObjective.getEntityTypeId();
								EntityType<?> requiredType = targetStr != null ? BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(targetStr)) : null;
								if (requiredType == null || event.getTarget().getType().equals(requiredType)) updateIndividualProgress(member, sagaId, questId, i, currentProgress + 1);
							}
						}
					}
				}
			});
		}
	}


	private static int countItems(ServerPlayer player, String itemId) {
		try {
			Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
			int count = 0;
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.getItem() == item) count += stack.getCount();
			}
			return count;
		} catch (Exception e) {
			return 0;
		}
	}

	private static void updatePartyState(List<ServerPlayer> members, String sagaId, int questId, int objIndex, int newProgress) {
		for (ServerPlayer member : members) updateIndividualProgress(member, sagaId, questId, objIndex, newProgress);
	}

	private static void updateIndividualProgress(ServerPlayer player, String sagaId, int questId, int objIndex, int newProgress) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			PlayerQuestData pqd = data.getPlayerQuestData();
			Saga saga = QuestRegistry.getSaga(sagaId);
			if (saga == null) return;
			Quest quest = getSagaQuest(saga, questId);
			if (quest == null) return;

			String questKey = PlayerQuestData.sagaQuestKey(sagaId, questId);
			int current = pqd.getObjectiveProgress(questKey, objIndex);
			if (current != newProgress) {
				pqd.setObjectiveProgress(questKey, objIndex, newProgress);

				if (objIndex >= 0 && objIndex < quest.getObjectives().size()) {
					QuestObjective objective = quest.getObjectives().get(objIndex);
					if (current < objective.getRequired() && newProgress >= objective.getRequired()) {
						int clampedProgress = Math.min(newProgress, objective.getRequired());
						NetworkHandler.sendToPlayer(StoryToastS2C.objectiveComplete(questKey, objIndex, clampedProgress, objective.getRequired()), player);
					}
				}

				if (checkAndCompleteQuest(pqd, sagaId, questId, quest)) {
					if (questKey.equals(pqd.getTrackedQuestId())) pqd.setTrackedQuestId(null);
					NetworkHandler.sendToPlayer(StoryToastS2C.questComplete(questKey), player);
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			}
		});
	}

	private static boolean checkAndCompleteQuest(PlayerQuestData pqd, String sagaId, int questId, Quest quest) {
		String questKey = PlayerQuestData.sagaQuestKey(sagaId, questId);
		boolean allObjectivesComplete = true;
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			int progress = pqd.getObjectiveProgress(questKey, i);
			if (progress < objective.getRequired()) {
				allObjectivesComplete = false;
				break;
			}
		}

		if (allObjectivesComplete && !pqd.isQuestCompleted(questKey)) {
			pqd.completeQuest(questKey);
			return true;
		}

		return false;
	}

	private static Quest getSagaQuest(Saga saga, int questId) {
		for (Quest q : saga.getQuests()) {
			if (q.getId() == questId) return q;
		}
		return null;
	}

	private static boolean shouldTrackSagaQuest(StatsData data, PlayerQuestData pqd, Saga saga, Quest quest, int questIndex) {
		String sagaId = saga.getId();
		String questKey = PlayerQuestData.sagaQuestKey(sagaId, quest.getId());
		if (pqd.isQuestCompleted(questKey)) return false;
		return SagaBranchingHelper.isSagaQuestAvailable(quest, saga, questIndex, data);
	}
}

