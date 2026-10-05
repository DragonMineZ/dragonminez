package com.dragonminez.client.gui.dialogue;

import com.dragonminez.common.network.S2C.OpenQuestNPCDialogueS2C;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestService;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class DialogueQuest {
	enum Status { TURN_IN, OFFER, IN_PROGRESS, COOLDOWN, LOCKED }

	final String id;
	final Quest quest;
	private Status status;
	final Component lockReason;
	final long cooldownEndMs;

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
		List<DialogueQuest> list = new ArrayList<>(byId.values());
		list.sort(Comparator.comparingInt(quest -> quest.status.ordinal()));
		return list;
	}

	private static void add(Map<String, DialogueQuest> byId, List<String> ids, Status status) {
		for (String id : ids) {
			if (byId.containsKey(id)) continue;
			Quest quest = QuestRegistry.getClientQuest(id);
			if (quest != null) byId.put(id, new DialogueQuest(id, quest, status, null, 0L));
		}
	}
}
