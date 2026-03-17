package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.*;
import com.dragonminez.common.quest.objectives.*;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles side-quest progression events (tick, kill, interact).
 * Uses {@link QuestRegistry} for quest lookups and {@link PlayerQuestData} for progress.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class SideQuestEvents {

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (!ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled()) return;
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
		if (event.player.tickCount % 20 != 0) return;
		if (!(event.player instanceof ServerPlayer player)) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			PlayerQuestData pqd = data.getPlayerQuestData();
			Set<String> acceptedIds = pqd.getAcceptedQuestIds();
			if (acceptedIds.isEmpty()) return;

			Set<String> tickRelevant = new HashSet<>();
			for (QuestObjective.ObjectiveType type : new QuestObjective.ObjectiveType[]{
					QuestObjective.ObjectiveType.BIOME,
					QuestObjective.ObjectiveType.STRUCTURE,
					QuestObjective.ObjectiveType.COORDS,
					QuestObjective.ObjectiveType.ITEM
			}) {
				tickRelevant.addAll(QuestRegistry.getQuestIdsByObjectiveType(type));
			}

			for (String questId : acceptedIds) {
				if (pqd.isQuestCompleted(questId)) continue;
				if (!tickRelevant.contains(questId)) continue;

				Quest sideQuest = QuestRegistry.getQuest(questId);
				if (sideQuest == null) continue;

				List<QuestObjective> objectives = sideQuest.getObjectives();
				for (int i = 0; i < objectives.size(); i++) {
					QuestObjective objective = objectives.get(i);
					int currentProgress = pqd.getObjectiveProgress(questId, i);
					if (currentProgress >= objective.getRequired()) continue;

					// Sequential mode: only process the first uncompleted objective
					if (!sideQuest.isParallelObjectives()) {
						processTickObjective(player, pqd, questId, objective, i);
						break;
					}

					// Parallel mode: process all uncompleted objectives
					processTickObjective(player, pqd, questId, objective, i);
				}

				checkAndComplete(player, pqd, questId);
			}
		});
	}

	private static void processTickObjective(ServerPlayer player, PlayerQuestData pqd, String questId, QuestObjective objective, int objIndex) {
		boolean isLocation = (objective instanceof BiomeObjective) || (objective instanceof StructureObjective) || (objective instanceof CoordsObjective);

		if (isLocation) {
			List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(player);
			boolean anyInZone = false;
			for (ServerPlayer member : partyMembers) {
				if (checkLocationCondition(member, objective)) {
					anyInZone = true;
					break;
				}
			}
			int targetProgress = anyInZone ? 1 : 0;
			updatePartyProgress(partyMembers, pqd, questId, objIndex, targetProgress, player);
		} else if (objective instanceof ItemObjective itemObj) {
			int itemCount = countItems(player, itemObj.getItemId());
			int savedProgress = pqd.getObjectiveProgress(questId, objIndex);
			if (itemCount != savedProgress) {
				int progressToSet = Math.min(itemCount, itemObj.getRequired());
				updateProgress(player, pqd, questId, objIndex, progressToSet);
			}
		}
	}

	@SubscribeEvent
	public static void onEntityKill(LivingDeathEvent event) {
		if (!ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled()) return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

		LivingEntity killedEntity = event.getEntity();
		List<String> killQuestIds = QuestRegistry.getQuestIdsByObjectiveType(QuestObjective.ObjectiveType.KILL);
		if (killQuestIds.isEmpty()) return;

		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(killer);

		for (ServerPlayer member : partyMembers) {
			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				Set<String> acceptedIds = pqd.getAcceptedQuestIds();

				for (String questId : killQuestIds) {
					if (!acceptedIds.contains(questId)) continue;
					if (pqd.isQuestCompleted(questId)) continue;

					Quest sideQuest = QuestRegistry.getQuest(questId);
					if (sideQuest == null) continue;

					List<QuestObjective> objectives = sideQuest.getObjectives();
					for (int i = 0; i < objectives.size(); i++) {
						QuestObjective objective = objectives.get(i);
						int currentProgress = pqd.getObjectiveProgress(questId, i);
						if (currentProgress >= objective.getRequired()) continue;

						// Sequential mode: skip if an earlier objective is uncompleted
						if (!sideQuest.isParallelObjectives() && !isFirstUncompleted(pqd, questId, objectives, i)) continue;

						if (objective instanceof KillObjective killObj) {
							ResourceLocation targetId = ResourceLocation.parse(killObj.getEntityId());
							EntityType<?> requiredType = BuiltInRegistries.ENTITY_TYPE.get(targetId);
							if (killedEntity.getType().equals(requiredType)) {
								updateProgress(member, pqd, questId, i, currentProgress + 1);
							}
						}
					}

					checkAndComplete(member, pqd, questId);
				}
			});
		}
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled()) return;
		if (!(event.getEntity() instanceof ServerPlayer interactor)) return;
		if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

		// Resolve the npcId if the target is a quest NPC or a masters entity
		String interactedNpcId = null;
		if (event.getTarget() instanceof QuestNPCEntity questNpc) {
			interactedNpcId = questNpc.getNpcId();
		} else if (event.getTarget() instanceof MastersEntity master) {
			interactedNpcId = master.getMasterName();
		}

		List<String> interactQuestIds = QuestRegistry.getQuestIdsByObjectiveType(QuestObjective.ObjectiveType.INTERACT);
		List<String> talkToQuestIds = QuestRegistry.getQuestIdsByObjectiveType(QuestObjective.ObjectiveType.TALK_TO);

		boolean hasInteract = !interactQuestIds.isEmpty();
		boolean hasTalkTo = !talkToQuestIds.isEmpty() && interactedNpcId != null;
		if (!hasInteract && !hasTalkTo) return;

		// Merge quest IDs to check
		Set<String> relevantQuestIds = new HashSet<>();
		relevantQuestIds.addAll(interactQuestIds);
		if (hasTalkTo) relevantQuestIds.addAll(talkToQuestIds);

		String finalNpcId = interactedNpcId;
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(interactor);

		for (ServerPlayer member : partyMembers) {
			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				Set<String> acceptedIds = pqd.getAcceptedQuestIds();

				for (String questId : relevantQuestIds) {
					if (!acceptedIds.contains(questId)) continue;
					if (pqd.isQuestCompleted(questId)) continue;

					Quest sideQuest = QuestRegistry.getQuest(questId);
					if (sideQuest == null) continue;

					List<QuestObjective> objectives = sideQuest.getObjectives();
					for (int i = 0; i < objectives.size(); i++) {
						QuestObjective objective = objectives.get(i);
						int currentProgress = pqd.getObjectiveProgress(questId, i);
						if (currentProgress >= objective.getRequired()) continue;

						if (!sideQuest.isParallelObjectives() && !isFirstUncompleted(pqd, questId, objectives, i)) continue;

						if (objective instanceof InteractObjective interactObj) {
							String targetStr = interactObj.getEntityTypeId();
							EntityType<?> requiredType = targetStr != null ? BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(targetStr)) : null;
							if (requiredType == null || event.getTarget().getType().equals(requiredType)) {
								updateProgress(member, pqd, questId, i, currentProgress + 1);
							}
						} else if (objective instanceof TalkToObjective talkObj && finalNpcId != null) {
							if (finalNpcId.equals(talkObj.getNpcId())) {
								updateProgress(member, pqd, questId, i, currentProgress + 1);
							}
						}
					}

					checkAndComplete(member, pqd, questId);
				}
			});
		}
	}

	// ---- Helper methods ----

	/**
	 * Returns true if the objective at the given index is the first uncompleted one in the list.
	 * Used for sequential mode to ensure objectives are completed in order.
	 */
	private static boolean isFirstUncompleted(PlayerQuestData pqd, String questId, List<QuestObjective> objectives, int targetIndex) {
		for (int i = 0; i < targetIndex; i++) {
			int progress = pqd.getObjectiveProgress(questId, i);
			if (progress < objectives.get(i).getRequired()) {
				return false;
			}
		}
		return true;
	}

	private static boolean checkLocationCondition(ServerPlayer player, QuestObjective objective) {
		BlockPos pos = player.blockPosition();
		ServerLevel level = player.serverLevel();

		if (objective instanceof BiomeObjective biomeObj) {
			try {
				String target = biomeObj.getBiomeId();
				Holder<Biome> biomeHolder = level.getBiome(pos);
				if (target.startsWith("#")) {
					ResourceLocation tagRL = ResourceLocation.parse(target.substring(1));
					TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, tagRL);
					return biomeHolder.is(tagKey);
				} else {
					ResourceLocation biomeRL = ResourceLocation.parse(target.contains(":") ? target : "minecraft:" + target);
					return biomeHolder.is(biomeRL);
				}
			} catch (Exception e) {
				return false;
			}
		} else if (objective instanceof StructureObjective structObj) {
			try {
				String target = structObj.getStructureId();
				ResourceLocation structRL = ResourceLocation.parse(target.contains(":") ? target : "minecraft:" + target);
				ResourceKey<Structure> structKey = ResourceKey.create(Registries.STRUCTURE, structRL);
				return level.structureManager().getStructureWithPieceAt(pos, structKey).isValid();
			} catch (Exception e) {
				return false;
			}
		} else if (objective instanceof CoordsObjective coordsObj) {
			double distSq = pos.distSqr(coordsObj.getTargetPos());
			double radiusSq = (double) coordsObj.getRadius() * coordsObj.getRadius();
			return distSq <= radiusSq;
		}

		return false;
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

	private static void updatePartyProgress(List<ServerPlayer> members, PlayerQuestData pqd, String questId, int objIndex, int newProgress, ServerPlayer sourcePlayer) {
		for (ServerPlayer member : members) {
			updateProgress(member, pqd, questId, objIndex, newProgress);
		}
	}

	private static void updateProgress(ServerPlayer player, PlayerQuestData pqd, String questId, int objIndex, int newProgress) {
		int current = pqd.getObjectiveProgress(questId, objIndex);
		if (current != newProgress) {
			pqd.setObjectiveProgress(questId, objIndex, newProgress);
			Quest quest = QuestRegistry.getQuest(questId);
			if (quest != null && objIndex >= 0 && objIndex < quest.getObjectives().size()) {
				QuestObjective objective = quest.getObjectives().get(objIndex);
				if (current < objective.getRequired() && newProgress >= objective.getRequired()) {
					int clampedProgress = Math.min(newProgress, objective.getRequired());
					NetworkHandler.sendToPlayer(StoryToastS2C.objectiveComplete(questId, objIndex, clampedProgress, objective.getRequired()), player);
				}
			}
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		}
	}

	private static void checkAndComplete(ServerPlayer player, PlayerQuestData pqd, String questId) {
		if (pqd.isQuestCompleted(questId)) return;

		Quest sideQuest = QuestRegistry.getQuest(questId);
		if (sideQuest == null) return;

		for (int i = 0; i < sideQuest.getObjectives().size(); i++) {
			QuestObjective objective = sideQuest.getObjectives().get(i);
			int progress = pqd.getObjectiveProgress(questId, i);
			if (progress < objective.getRequired()) return;
		}

		pqd.completeQuest(questId);
		if (questId.equals(pqd.getTrackedQuestId())) pqd.setTrackedQuestId(null);
		NetworkHandler.sendToPlayer(StoryToastS2C.questComplete(questId), player);
		NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
	}
}

