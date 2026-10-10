package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestAvailabilityChecker;
import com.dragonminez.common.quest.QuestDataMigrations;
import com.dragonminez.common.quest.QuestLocationHelper;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestParty;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.quest.objectives.CheckpointRaceObjective;
import com.dragonminez.common.quest.objectives.DeliverObjective;
import com.dragonminez.common.quest.objectives.EscortObjective;
import com.dragonminez.common.quest.objectives.InteractObjective;
import com.dragonminez.common.quest.objectives.SparObjective;
import com.dragonminez.common.quest.objectives.SurviveWavesObjective;
import com.dragonminez.common.quest.objectives.ItemObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.DragonSummonObjective;
import com.dragonminez.common.quest.objectives.SkillObjective;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class QuestEvents {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
			return;
		}
		if (event.player.tickCount % 20 != 0) {
			return;
		}
		if (!(event.player instanceof ServerPlayer player) || StorageManager.isLoadPending(player)) {
			return;
		}

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			boolean timingChanged = primeStartRequirementTimers(player, data);
			processTickObjectives(player, data);
			handleQuestTimeLimits(player, data);
			if (timingChanged) {
				NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
			}
		});
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && !StorageManager.isLoadPending(player)) {
			onPlayerDataReady(player);
		}
	}

	public static void onPlayerDataReady(ServerPlayer player) {
		PartyManager.reconcile(player);
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (QuestDataMigrations.run(player, data.getPlayerQuestData())) {
				NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
			}
		});
	}

	@SubscribeEvent
	public static void onSparHurt(LivingHurtEvent event) {
		LivingEntity target = event.getEntity();
		if (target.level().isClientSide) return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
		if (!target.getPersistentData().contains(QuestService.QUEST_KEY_TAG)) return;
		if (target.isRemoved()) {
			event.setCanceled(true);
			return;
		}

		String questKey = target.getPersistentData().getString(QuestService.QUEST_KEY_TAG);
		int objectiveIndex = target.getPersistentData().getInt(QuestService.QUEST_OBJECTIVE_INDEX_TAG);
		Quest quest = QuestRegistry.getQuest(questKey);
		if (quest == null || objectiveIndex < 0 || objectiveIndex >= quest.getObjectives().size()) return;
		if (!(quest.getObjectives().get(objectiveIndex) instanceof SparObjective)) return;

		List<ServerPlayer> attackerParty = PartyManager.getAllPartyMembers(attacker);
		String owner = target.getPersistentData().getString(QuestService.QUEST_OWNER_TAG);
		boolean ownParty = false;
		for (ServerPlayer member : attackerParty) {
			if (member.getStringUUID().equals(owner)) {
				ownParty = true;
				break;
			}
		}
		if (!ownParty) {
			event.setCanceled(true);
			return;
		}

		if (target.getHealth() - event.getAmount() > target.getMaxHealth() * 0.15f) return;

		event.setCanceled(true);
		forEachSharedQuest(attacker, (member, qk, q, pqd) ->
				processKillObjectives(member, pqd, qk, q, target, attackerParty));
		attacker.displayClientMessage(Component.translatable("message.dragonminez.quest.spar_won",
				target.getDisplayName()), true);
		target.discard();
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			QuestFieldSessions.clearAll(player);
			QuestService.clearPlayer(player.getUUID());
			com.dragonminez.common.network.C2S.SetTrackedQuestC2S.clear(player.getUUID());
			com.dragonminez.server.world.tournament.Tournament.Manager.onPlayerLogout(player);
		}
	}

	@SubscribeEvent
	public static void onEntityKill(LivingDeathEvent event) {
		QuestFieldSessions.onEntityDeath(event.getEntity());

		if (event.getEntity() instanceof ServerPlayer deadPlayer) {
			handlePlayerQuestFailure(deadPlayer);
			com.dragonminez.server.world.tournament.Tournament.Manager.onPlayerDeath(deadPlayer);
		}

		if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
			return;
		}

		creditQuestKill(killer, event.getEntity());
	}

	public static void creditQuestKill(ServerPlayer killer, LivingEntity killedEntity) {
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(killer);
		forEachSharedQuest(killer, (member, questKey, quest, pqd) ->
				processKillObjectives(member, pqd, questKey, quest, killedEntity, partyMembers));
	}

	private static void handleQuestTimeLimits(ServerPlayer player, StatsData data) {
		PlayerQuestData pqd = data.getPlayerQuestData();
		Set<String> acceptedQuestIds = new LinkedHashSet<>(pqd.getAcceptedQuestIds());
		if (acceptedQuestIds.isEmpty()) {
			return;
		}

		long gameTime = player.serverLevel().getGameTime();
		Set<String> failedQuestIds = new LinkedHashSet<>();
		for (String questKey : acceptedQuestIds) {
			Quest quest = QuestRegistry.getQuest(questKey);
			if (quest == null || !quest.hasTimeLimit()) {
				continue;
			}
			long acceptedGameTime = pqd.getQuestAcceptedGameTime(questKey);
			if (acceptedGameTime < 0 || gameTime - acceptedGameTime < quest.getTimeLimitSeconds() * 20L) {
				continue;
			}

			if (failQuestFor(player, pqd, questKey, quest, DMZEvent.QuestFailEvent.FailureReason.TIME_EXPIRED)) {
				failedQuestIds.add(questKey);
			}
		}

		if (failedQuestIds.isEmpty()) {
			return;
		}

		for (String questKey : failedQuestIds) {
			NetworkHandler.sendToPlayer(StoryToastS2C.questFailed(questKey), player);
		}
		QuestService.syncQuestState(player);
	}

	public static void handlePlayerQuestFailure(ServerPlayer deadPlayer) {
		PlayerQuestData deadData = QuestParty.questData(deadPlayer);
		if (deadData == null) {
			return;
		}

		for (String questKey : new LinkedHashSet<>(deadData.getAcceptedQuestIds())) {
			Quest quest = QuestRegistry.getQuest(questKey);
			if (quest == null || !hasKillObjectives(quest)) {
				continue;
			}

			List<ServerPlayer> group = QuestParty.allParticipants(deadPlayer, questKey);
			boolean wiped = true;
			for (ServerPlayer member : group) {
				if (member != deadPlayer && !member.isDeadOrDying()) {
					wiped = false;
					break;
				}
			}
			if (!wiped) {
				continue;
			}

			for (ServerPlayer member : group) {
				PlayerQuestData memberData = member == deadPlayer ? deadData : QuestParty.questData(member);
				if (memberData == null || !memberData.isQuestAccepted(questKey)) continue;
				if (failQuestFor(member, memberData, questKey, quest, DMZEvent.QuestFailEvent.FailureReason.PLAYER_DEATH)) {
					NetworkHandler.sendToPlayer(StoryToastS2C.questFailed(questKey), member);
					QuestService.syncQuestState(member);
				}
			}
		}
	}

	static boolean failQuestFor(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest,
								DMZEvent.QuestFailEvent.FailureReason reason) {
		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		DMZEvent.QuestFailEvent failEvent = new DMZEvent.QuestFailEvent(
				player,
				questKey,
				resolved != null ? resolved.saga() : null,
				quest,
				QuestParty.allParticipants(player, questKey),
				reason
		);
		if (MinecraftForge.EVENT_BUS.post(failEvent)) {
			return false;
		}

		pqd.failQuest(questKey);
		QuestFieldSessions.clearQuest(player, questKey);
		return true;
	}

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getEntity() instanceof ServerPlayer interactor)) {
			return;
		}
		if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || StorageManager.isLoadPending(interactor)) {
			return;
		}

		String interactedNpcId = null;
		if (event.getTarget() instanceof QuestNPCEntity questNpc) {
			interactedNpcId = questNpc.getNpcId();
		} else if (event.getTarget() instanceof MastersEntity master) {
			interactedNpcId = master.getMasterName();
		}

		String finalNpcId = interactedNpcId;
		if (finalNpcId != null) {
			StatsProvider.get(StatsCapability.INSTANCE, interactor).ifPresent(data ->
					processAcceptedQuests(interactor, data, (questKey, quest, pqd) ->
							processDeliveries(interactor, pqd, questKey, quest, finalNpcId)));
		}

		forEachSharedQuest(interactor, (member, questKey, quest, pqd) ->
				processInteractObjectives(member, pqd, questKey, quest, event, finalNpcId));
	}

	@SubscribeEvent
	public static void onDragonSummoned(DMZEvent.DragonSummonedEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer summoner)) {
			return;
		}

		forEachSharedQuest(summoner, (member, questKey, quest, pqd) ->
				processDragonSummonObjectives(member, pqd, questKey, quest, event));
	}

	private static boolean primeStartRequirementTimers(ServerPlayer player, StatsData data) {
		PlayerQuestData pqd = data.getPlayerQuestData();
		boolean timingChanged = false;

		for (Map.Entry<String, Quest> entry : QuestRegistry.getAllQuests().entrySet()) {
			String questKey = entry.getKey();
			Quest quest = entry.getValue();
			if (!QuestAvailabilityChecker.hasTimedStartRequirement(quest)
					|| !isQuestTypeEnabled(quest)
					|| pqd.getQuestStatus(questKey) != PlayerQuestData.QuestStatus.NOT_STARTED
					|| pqd.getStartRequirementTiming(questKey) != null) {
				continue;
			}
			if (!isQuestAvailableForTracking(questKey, quest, data)) {
				continue;
			}
			timingChanged |= QuestAvailabilityChecker.primeStartRequirementTiming(quest, questKey, player, data);
		}

		return timingChanged;
	}

	private static void processTickObjectives(ServerPlayer player, StatsData data) {
		processAcceptedQuests(player, data, (questKey, quest, pqd) -> {
			for (int i = 0; i < quest.getObjectives().size(); i++) {
				QuestObjective objective = quest.getObjectives().get(i);
				int required = quest.getObjectiveRequired(pqd, questKey, i);
				int currentProgress = pqd.getObjectiveProgress(questKey, i);

				if (objective instanceof ItemObjective itemObjective) {
					int held = Math.min(QuestParty.countItemById(player, itemObjective.getItemId()), required);
					if (held != currentProgress) {
						updateProgress(player, pqd, questKey, quest, i, held);
					}
					if (held >= required) continue;
					if (!quest.isParallelObjectives()) break;
					continue;
				}

				if (currentProgress >= required || QuestService.isTurnInTalk(quest, objective)) {
					continue;
				}

				if (QuestLocationHelper.isLocationObjective(objective)) {
					boolean inZone = QuestLocationHelper.isLocationConditionMet(player, objective);
					if (!inZone) {
						for (ServerPlayer participant : QuestParty.participants(player, questKey)) {
							if (participant != player && QuestLocationHelper.isLocationConditionMet(participant, objective)) {
								inZone = true;
								break;
							}
						}
					}
					if (inZone) {
						updateProgress(player, pqd, questKey, quest, i, required);
					}
				} else if (objective instanceof SkillObjective skillObjective) {
					int skillLevel = data.getSkills().getSkillLevel(skillObjective.getSkill());
					if (skillLevel != currentProgress) {
						int progressToSet = Math.min(skillLevel, required);
						updateProgress(player, pqd, questKey, quest, i, progressToSet);
					}
				} else if (objective instanceof CheckpointRaceObjective raceObjective) {
					BlockPos checkpoint = raceObjective.getCheckpoint(currentProgress);
					if (checkpoint != null && isInObjectiveDimension(player, pqd, questKey, raceObjective.getDimension())
							&& player.distanceToSqr(checkpoint.getX() + 0.5, checkpoint.getY() + 0.5, checkpoint.getZ() + 0.5)
							<= (double) raceObjective.getRadius() * raceObjective.getRadius()) {
						updateProgress(player, pqd, questKey, quest, i, currentProgress + 1);
						player.displayClientMessage(Component.translatable(
								"message.dragonminez.quest.checkpoint", currentProgress + 1, required), true);
					}
				} else if (objective instanceof SurviveWavesObjective wavesObjective) {
					QuestFieldSessions.tickWaves(player, pqd, questKey, quest, i, wavesObjective);
				} else if (objective instanceof EscortObjective escortObjective) {
					QuestFieldSessions.tickEscort(player, pqd, questKey, quest, i, escortObjective);
				}

				if (!quest.isParallelObjectives()) {
					break;
				}
			}

			checkAndComplete(player, pqd, questKey, quest);
		});
	}

	static boolean isInObjectiveDimension(ServerPlayer player, PlayerQuestData pqd, String questKey, String objectiveDimension) {
		String dimension = objectiveDimension != null && !objectiveDimension.isBlank()
				? objectiveDimension
				: pqd.getAcceptedDimension(questKey);
		if (dimension == null || dimension.isBlank()) {
			return true;
		}
		ResourceLocation id = ResourceLocation.tryParse(dimension);
		return id != null && player.level().dimension().location().equals(id);
	}

	private static void processKillObjectives(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest,
											LivingEntity killedEntity, List<ServerPlayer> partyMembers) {
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			int currentProgress = pqd.getObjectiveProgress(questKey, i);
			if (currentProgress >= quest.getObjectiveRequired(pqd, questKey, i)) {
				continue;
			}
			if (!(objective instanceof KillObjective killObjective)) {
				continue;
			}
			if (!isKillObjectiveUnlocked(pqd, questKey, quest, i)) {
				continue;
			}
			if (matchesKillObjective(killedEntity, questKey, i, killObjective, partyMembers)) {
				updateProgress(player, pqd, questKey, quest, i, currentProgress + 1);
			}
		}

		checkAndComplete(player, pqd, questKey, quest);
	}

	private static void processDeliveries(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest, String npcId) {
		if (QuestService.requiresTurnInAction(quest) && npcId.equalsIgnoreCase(quest.getTurnIn())) {
			return;
		}
		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		if (resolved == null) {
			return;
		}

		boolean delivered = false;
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			if (!(quest.getObjectives().get(i) instanceof DeliverObjective deliver) || !deliver.matchesNpc(npcId)) {
				continue;
			}
			if (pqd.getObjectiveProgress(questKey, i) >= quest.getObjectiveRequired(pqd, questKey, i)) {
				continue;
			}
			if (!quest.isParallelObjectives() && !isFirstUncompleted(pqd, questKey, quest, i)) {
				continue;
			}
			Component blocker = QuestService.deliverOnInteract(player, pqd, resolved, npcId, i);
			if (blocker != null) {
				player.sendSystemMessage(blocker.copy().withStyle(net.minecraft.ChatFormatting.YELLOW));
			} else {
				delivered = true;
			}
		}

		if (delivered) {
			for (ServerPlayer participant : QuestParty.participants(player, questKey)) {
				PlayerQuestData participantData = participant == player ? pqd : QuestParty.questData(participant);
				if (participantData == null) continue;
				checkAndComplete(participant, participantData, questKey, quest);
				QuestService.syncQuestState(participant);
			}
		}
	}

	private static void processInteractObjectives(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest,
												PlayerInteractEvent.EntityInteract event, String interactedNpcId) {
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			int currentProgress = pqd.getObjectiveProgress(questKey, i);
			if (currentProgress >= quest.getObjectiveRequired(pqd, questKey, i)) {
				continue;
			}
			if (!quest.isParallelObjectives() && !isFirstUncompleted(pqd, questKey, quest, i)) {
				continue;
			}

			if (objective instanceof InteractObjective interactObjective) {
				if (matchesInteractTarget(interactObjective, event)) {
					updateProgress(player, pqd, questKey, quest, i, currentProgress + 1);
				}
			} else if (objective instanceof TalkToObjective talkToObjective
					&& interactedNpcId != null
					&& !QuestService.isTurnInTalk(quest, objective)
					&& interactedNpcId.equalsIgnoreCase(talkToObjective.getNpcId())) {
				updateProgress(player, pqd, questKey, quest, i, currentProgress + 1);
			}
		}

		checkAndComplete(player, pqd, questKey, quest);
	}

	private static boolean matchesInteractTarget(InteractObjective objective, PlayerInteractEvent.EntityInteract event) {
		String targetStr = objective.getEntityTypeId();
		if (targetStr != null) {
			ResourceLocation id = ResourceLocation.tryParse(targetStr);
			if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return false;
			EntityType<?> requiredType = BuiltInRegistries.ENTITY_TYPE.get(id);
			if (!event.getTarget().getType().equals(requiredType)) return false;
		}
		String requiredName = objective.getEntityName();
		return requiredName == null || requiredName.isBlank()
				|| event.getTarget().getName().getString().equals(requiredName);
	}

	private static void processDragonSummonObjectives(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest,
													 DMZEvent.DragonSummonedEvent event) {
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			int currentProgress = pqd.getObjectiveProgress(questKey, i);
			if (currentProgress >= quest.getObjectiveRequired(pqd, questKey, i)) {
				continue;
			}
			if (!quest.isParallelObjectives() && !isFirstUncompleted(pqd, questKey, quest, i)) {
				continue;
			}

			if (objective instanceof DragonSummonObjective summonObjective
					&& summonObjective.matches(event.getDragonId(), event.getBallSetId())) {
				updateProgress(player, pqd, questKey, quest, i, currentProgress + 1);
			}
		}

		checkAndComplete(player, pqd, questKey, quest);
	}

	private static void processAcceptedQuests(ServerPlayer player, StatsData data, AcceptedQuestProcessor processor) {
		if (StorageManager.isLoadPending(player)) {
			return;
		}
		PlayerQuestData pqd = data.getPlayerQuestData();
		Set<String> acceptedIds = pqd.getAcceptedQuestIds();
		for (String questKey : acceptedIds) {
			Quest quest = QuestRegistry.getQuest(questKey);
			if (quest == null || pqd.isQuestCompleted(questKey) || !isQuestTypeEnabled(quest)) {
				continue;
			}
			processor.process(questKey, quest, pqd);
		}
	}

	private static boolean isQuestAvailableForTracking(String questKey, Quest quest, StatsData data) {
		if (!quest.isSagaQuest()) {
			return QuestAvailabilityChecker.isAvailable(quest, data);
		}

		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		if (resolved == null || resolved.saga() == null) {
			return false;
		}

		int questIndex = resolved.saga().getQuests().indexOf(quest);
		return questIndex >= 0 && QuestAvailabilityChecker.isSagaQuestAvailable(quest, resolved.saga(), questIndex, data);
	}

	private static boolean isQuestTypeEnabled(Quest quest) {
		if (quest == null) {
			return false;
		}

		if (quest.isSagaQuest()) {
			return ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled();
		}
		if (quest.isSideQuest()) {
			return ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled();
		}
		return true;
	}

	private static boolean hasKillObjectives(Quest quest) {
		for (QuestObjective objective : quest.getObjectives()) {
			if (objective instanceof KillObjective
					|| objective instanceof SurviveWavesObjective
					|| objective instanceof EscortObjective) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesKillObjective(LivingEntity killedEntity, String questKey, int objectiveIndex,
												KillObjective killObjective, List<ServerPlayer> partyMembers) {
		try {
			boolean typeMatches = killObjective.matches(killedEntity.getType());
			boolean hasQuestTags = hasQuestSpawnTags(killedEntity);
			boolean questTagsMatch = hasQuestTags && matchesQuestSpawnTags(killedEntity, questKey, objectiveIndex, partyMembers);
			return acceptsKillMatch(typeMatches, questTagsMatch, killObjective.getCountMode(), hasQuestTags);
		} catch (Exception e) {
			return false;
		}
	}

	static boolean acceptsKillMatch(boolean entityTypeMatches, boolean questSpawnTagsMatch,
									KillObjective.CountMode countMode, boolean hasQuestSpawnTags) {
		if (questSpawnTagsMatch) {
			return true;
		}
		return entityTypeMatches
				&& countMode == KillObjective.CountMode.ANY_MATCHING
				&& !hasQuestSpawnTags;
	}

	private static boolean hasQuestSpawnTags(LivingEntity killedEntity) {
		return killedEntity.getPersistentData().contains(QuestService.QUEST_KEY_TAG)
				|| killedEntity.getPersistentData().contains(QuestService.QUEST_OBJECTIVE_INDEX_TAG)
				|| killedEntity.getPersistentData().contains(QuestService.QUEST_OWNER_TAG);
	}

	private static boolean matchesQuestSpawnTags(LivingEntity killedEntity, String questKey, int objectiveIndex,
												List<ServerPlayer> partyMembers) {
		if (!killedEntity.getPersistentData().contains(QuestService.QUEST_KEY_TAG)
				|| !killedEntity.getPersistentData().contains(QuestService.QUEST_OBJECTIVE_INDEX_TAG)
				|| !killedEntity.getPersistentData().contains(QuestService.QUEST_OWNER_TAG)) {
			return false;
		}
		if (!questKey.equals(killedEntity.getPersistentData().getString(QuestService.QUEST_KEY_TAG))
				|| objectiveIndex != killedEntity.getPersistentData().getInt(QuestService.QUEST_OBJECTIVE_INDEX_TAG)) {
			return false;
		}

		String ownerUuid = killedEntity.getPersistentData().getString(QuestService.QUEST_OWNER_TAG);
		for (ServerPlayer member : partyMembers) {
			if (member.getStringUUID().equals(ownerUuid)) {
				return true;
			}
		}
		return false;
	}

	static boolean isKillObjectiveUnlocked(PlayerQuestData pqd, String questKey, Quest quest, int objectiveIndex) {
		if (quest == null || objectiveIndex < 0 || objectiveIndex >= quest.getObjectives().size()) {
			return false;
		}
		if (!(quest.getObjectives().get(objectiveIndex) instanceof KillObjective)) {
			return false;
		}
		if (quest.isParallelObjectives()) {
			return true;
		}

		int killBlockStart = objectiveIndex;
		while (killBlockStart > 0 && quest.getObjectives().get(killBlockStart - 1) instanceof KillObjective) {
			killBlockStart--;
		}
		return isFirstUncompleted(pqd, questKey, quest, killBlockStart);
	}

	static boolean isFirstUncompleted(PlayerQuestData pqd, String questKey, Quest quest, int targetIndex) {
		for (int i = 0; i < targetIndex; i++) {
			if (QuestService.isTurnInTalk(quest, quest.getObjectives().get(i))) {
				continue;
			}
			int progress = pqd.getObjectiveProgress(questKey, i);
			if (progress < quest.getObjectiveRequired(pqd, questKey, i)) {
				return false;
			}
		}
		return true;
	}

	static void updateProgress(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest,
										   int objectiveIndex, int newProgress) {
		int current = pqd.getObjectiveProgress(questKey, objectiveIndex);
		if (current == newProgress) {
			return;
		}

		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		int required = objectiveIndex >= 0 && objectiveIndex < quest.getObjectives().size()
				? quest.getObjectiveRequired(pqd, questKey, objectiveIndex)
				: 0;
		DMZEvent.QuestObjectiveProgressEvent progressEvent = new DMZEvent.QuestObjectiveProgressEvent(
				player,
				questKey,
				resolved != null ? resolved.saga() : null,
				quest,
				QuestParty.allParticipants(player, questKey),
				objectiveIndex,
				current,
				newProgress,
				required
		);
		if (MinecraftForge.EVENT_BUS.post(progressEvent)) {
			return;
		}
		newProgress = Math.max(0, progressEvent.getNewProgress());
		if (current == newProgress) {
			return;
		}

		pqd.setObjectiveProgress(questKey, objectiveIndex, newProgress);

		if (objectiveIndex >= 0 && objectiveIndex < quest.getObjectives().size()) {
			if (current < required && newProgress >= required) {
				int clampedProgress = Math.min(newProgress, required);
				NetworkHandler.sendToPlayer(
						StoryToastS2C.objectiveComplete(questKey, objectiveIndex, clampedProgress, required),
						player
				);
			}
		}

		NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
	}

	static void checkAndComplete(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest) {
		if (!pqd.isQuestAccepted(questKey) || QuestService.requiresTurnInAction(quest)) {
			return;
		}

		for (int i = 0; i < quest.getObjectives().size(); i++) {
			int progress = pqd.getObjectiveProgress(questKey, i);
			if (progress < quest.getObjectiveRequired(pqd, questKey, i)) {
				return;
			}
		}

		QuestService.ResolvedQuest resolved = QuestService.resolveQuest(questKey);
		DMZEvent.QuestCompletedEvent completeEvent = new DMZEvent.QuestCompletedEvent(
				player,
				questKey,
				resolved != null ? resolved.saga() : null,
				quest,
				QuestParty.allParticipants(player, questKey)
		);
		if (MinecraftForge.EVENT_BUS.post(completeEvent)) {
			return;
		}

		pqd.completeQuest(questKey);
		QuestFieldSessions.clearQuest(player, questKey);
		if (questKey.equals(pqd.getTrackedQuestId())) {
			pqd.setTrackedQuestId(null);
		}
		NetworkHandler.sendToPlayer(StoryToastS2C.questComplete(questKey), player);
		QuestService.syncQuestState(player);
	}

	private static void forEachSharedQuest(ServerPlayer actor, SharedQuestProcessor processor) {
		PlayerQuestData actorData = QuestParty.questData(actor);
		if (actorData == null) {
			return;
		}
		Set<String> actorQuests = actorData.getAcceptedQuestIds();
		for (ServerPlayer member : QuestParty.nearbyMembers(actor)) {
			StatsProvider.get(StatsCapability.INSTANCE, member).ifPresent(data ->
					processAcceptedQuests(member, data, (questKey, quest, pqd) -> {
						if (member == actor || actorQuests.contains(questKey)) {
							processor.process(member, questKey, quest, pqd);
						}
					}));
		}
	}

	@FunctionalInterface
	private interface SharedQuestProcessor {
		void process(ServerPlayer member, String questKey, Quest quest, PlayerQuestData pqd);
	}

	@FunctionalInterface
	private interface AcceptedQuestProcessor {
		void process(String questKey, Quest quest, PlayerQuestData pqd);
	}
}
