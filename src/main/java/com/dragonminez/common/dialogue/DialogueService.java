package com.dragonminez.common.dialogue;

import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.OpenDialogueNodeS2C;
import com.dragonminez.common.network.S2C.OpenQuestNPCDialogueS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.QuestAvailabilityChecker;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DialogueService {

	private static final double MAX_NPC_DISTANCE_SQR = 10.0 * 10.0;
	private static final long CHOICE_MIN_INTERVAL_TICKS = 4L;
	private static final Map<UUID, ActiveNode> ACTIVE_NODES = new ConcurrentHashMap<>();

	private record ActiveNode(String npcId, int entityId, String nodeId) {
	}

	private DialogueService() {
	}

	/** Opens the NPC's dialogue tree for the player. Returns false when the NPC has no tree. */
	public static boolean openDialogue(ServerPlayer player, String npcId, int entityId) {
		DialogueTree tree = DialogueRegistry.getTree(npcId);
		if (tree == null) return false;
		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			sendNode(player, data, tree, tree.getStartNodeId(), entityId);
			return true;
		}).orElse(false);
	}

	public static void handleChoice(ServerPlayer player, String npcId, int entityId, String nodeId, int choiceIndex) {
		DialogueTree tree = DialogueRegistry.getTree(npcId);
		if (tree == null || !isNearMatchingNpc(player, npcId, entityId)) return;

		ActiveNode active = ACTIVE_NODES.get(player.getUUID());
		if (active == null || active.entityId() != entityId || !active.npcId().equals(tree.getNpcId()) || !active.nodeId().equals(nodeId)) return;
		if (!PacketRateLimiter.allow(player.getUUID(), "dialogue_choice", player.level().getGameTime(), CHOICE_MIN_INTERVAL_TICKS)) return;

		DialogueTree.DialogueNode node = tree.getNode(nodeId);
		if (node == null || choiceIndex < 0 || choiceIndex >= node.getChoices().size()) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			DialogueTree.DialogueChoice choice = node.getChoices().get(choiceIndex);
			if (!QuestAvailabilityChecker.arePrerequisitesMet(choice.getConditions(), data)) return;

			ACTIVE_NODES.remove(player.getUUID(), active);
			boolean openedQuests = false;
			for (DialogueAction action : choice.getActions()) {
				if (action.getType() == DialogueAction.Type.OPEN_QUESTS) {
					openQuestScreen(player, data, npcId, entityId);
					openedQuests = true;
				} else {
					action.execute(player);
				}
			}
			if (!choice.getActions().isEmpty()) {
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			}
			if (openedQuests) return;

			sendNode(player, data, tree, choice.getGotoNodeId(), entityId);
		});
	}

	private static void sendNode(ServerPlayer player, StatsData data, DialogueTree tree, String nodeId, int entityId) {
		DialogueTree.DialogueNode node = tree.getNode(nodeId);
		if (node == null) {
			ACTIVE_NODES.remove(player.getUUID());
			NetworkHandler.sendToPlayer(OpenDialogueNodeS2C.close(tree.getNpcId(), entityId), player);
			return;
		}

		List<String> choiceTexts = new ArrayList<>();
		List<Integer> choiceIndices = new ArrayList<>();
		List<DialogueTree.DialogueChoice> choices = node.getChoices();
		for (int i = 0; i < choices.size(); i++) {
			DialogueTree.DialogueChoice choice = choices.get(i);
			if (QuestAvailabilityChecker.arePrerequisitesMet(choice.getConditions(), data)) {
				choiceTexts.add(choice.getText());
				choiceIndices.add(i);
			}
		}

		ACTIVE_NODES.put(player.getUUID(), new ActiveNode(tree.getNpcId(), entityId, node.getId()));
		NetworkHandler.sendToPlayer(
				new OpenDialogueNodeS2C(tree.getNpcId(), entityId, node.getId(), node.getLine(), choiceTexts, choiceIndices),
				player);
	}

	public static void clear(UUID uuid) {
		ACTIVE_NODES.remove(uuid);
	}

	private static void openQuestScreen(ServerPlayer player, StatsData data, String npcId, int entityId) {
		QuestService.NPCQuestOptions options = QuestService.collectNpcQuestOptions(npcId, data);
		NetworkHandler.sendToPlayer(
				new OpenQuestNPCDialogueS2C(npcId, options.offerableQuestIds(),
						options.turnInQuestIds(), options.inProgressQuestIds(), true, entityId),
				player);
	}

	private static boolean isNearMatchingNpc(ServerPlayer player, String npcId, int entityId) {
		Entity entity = player.serverLevel().getEntity(entityId);
		if (!(entity instanceof MastersEntity master)) return false;
		if (!npcId.equalsIgnoreCase(master.getMasterName())) return false;
		return player.distanceToSqr(entity) <= MAX_NPC_DISTANCE_SQR;
	}
}
