package com.dragonminez.client.gui.dialogue;

import com.dragonminez.common.network.S2C.OpenQuestNPCDialogueS2C;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestService;
import com.dragonminez.common.quest.QuestUnlockOrder;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class DialogueQuest {
	enum Status { TURN_IN, OFFER, IN_PROGRESS, COOLDOWN, LOCKED, COMPLETED }

	final String id;
	final Quest quest;
	private Status status;
	Component lockReason;
	long cooldownEndMs;

	private DialogueQuest(String id, Quest quest, Status status, Component lockReason, long cooldownEndMs) {
		this.id = id;
		this.quest = quest;
		this.status = status;
		this.lockReason = lockReason;
		this.cooldownEndMs = cooldownEndMs;
	}

	Status status(long now) {
		return status == Status.COOLDOWN && now >= cooldownEndMs ? Status.OFFER : status;
	}

	void setStatus(Status status) {
		this.status = status;
	}

	boolean done(long now) {
		Status current = status(now);
		return current == Status.COOLDOWN || current == Status.COMPLETED;
	}

	void markTurnedIn(long now) {
		if (!quest.isRepeatable() || quest.getType() == Quest.QuestType.DAILY) {
			status = Status.COMPLETED;
		} else if (quest.getRepeatCooldownSeconds() <= 0) {
			status = Status.OFFER;
		} else {
			status = Status.COOLDOWN;
			cooldownEndMs = now + quest.getRepeatCooldownSeconds() * 1000L;
			lockReason = Component.translatable("gui.dragonminez.dialogue.quest.repeat_cooldown");
		}
	}

	static String optionId(String questId) {
		return "quest:" + questId;
	}

	static List<DialogueQuest> from(OpenQuestNPCDialogueS2C msg) {
		Map<String, DialogueQuest> byId = new LinkedHashMap<>();
		long now = Util.getMillis();
		add(byId, msg.getTurnInQuestIds(), Status.TURN_IN);
		add(byId, msg.getOfferableQuestIds(), Status.OFFER);
		add(byId, msg.getInProgressQuestIds(), Status.IN_PROGRESS);
		for (QuestService.LockedNpcQuest locked : msg.getLockedQuests()) {
			Quest quest = QuestRegistry.getClientQuest(locked.questId());
			if (quest == null || byId.containsKey(locked.questId())) continue;
			boolean cooldown = locked.cooldownSeconds() > 0L;
			byId.put(locked.questId(), new DialogueQuest(locked.questId(), quest, cooldown ? Status.COOLDOWN : Status.LOCKED,
					locked.reason(), cooldown ? now + locked.cooldownSeconds() * 1000L : 0L));
		}
		addCompleted(byId, msg.getNpcId());

		List<String> ids = new ArrayList<>(byId.keySet());
		ids.sort(QuestUnlockOrder.comparator(QuestRegistry.getClientQuests(), QuestRegistry.getClientSagas()));
		List<DialogueQuest> list = new ArrayList<>(ids.size());
		for (String id : ids) list.add(byId.get(id));
		return list;
	}

	private static void add(Map<String, DialogueQuest> byId, List<String> ids, Status status) {
		for (String id : ids) {
			if (byId.containsKey(id)) continue;
			Quest quest = QuestRegistry.getClientQuest(id);
			if (quest != null) byId.put(id, new DialogueQuest(id, quest, status, null, 0L));
		}
	}

	private static void addCompleted(Map<String, DialogueQuest> byId, String npcId) {
		StatsData stats = NpcDialogueContent.stats();
		if (stats == null || npcId == null || npcId.isBlank()) return;
		PlayerQuestData questData = stats.getPlayerQuestData();
		for (Map.Entry<String, Quest> entry : QuestRegistry.getClientQuests().entrySet()) {
			if (byId.containsKey(entry.getKey()) || !npcId.equals(entry.getValue().getQuestGiver())) continue;
			if (questData.isQuestCompleted(entry.getKey())) {
				byId.put(entry.getKey(), new DialogueQuest(entry.getKey(), entry.getValue(), Status.COMPLETED, null, 0L));
			}
		}
	}
}
