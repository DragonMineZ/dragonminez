package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.quest.sidequest.SideQuest;
import com.dragonminez.common.quest.sidequest.SideQuestData;
import com.dragonminez.common.quest.sidequest.SideQuestManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sent from client to server when a player turns in a quest at an NPC.
 * Validates that all non-TALK_TO objectives are complete, then completes any TALK_TO objectives
 * matching the npcId, and finishes the quest with automatic reward granting.
 */
public class TurnInSideQuestC2S {
	private final String sideQuestId;
	private final String npcId;

	public TurnInSideQuestC2S(String sideQuestId, String npcId) {
		this.sideQuestId = sideQuestId;
		this.npcId = npcId;
	}

	public TurnInSideQuestC2S(FriendlyByteBuf buffer) {
		this.sideQuestId = buffer.readUtf();
		this.npcId = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(sideQuestId);
		buffer.writeUtf(npcId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			SideQuest sideQuest = SideQuestManager.getSideQuest(sideQuestId);
			if (sideQuest == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				SideQuestData sqData = data.getSideQuestData();

				// Must be accepted and not completed
				if (!sqData.isQuestAccepted(sideQuestId)) return;
				if (sqData.isQuestCompleted(sideQuestId)) return;

				// Validate the NPC matches the turn-in NPC
				if (sideQuest.getTurnIn() == null || !sideQuest.getTurnIn().equals(npcId)) return;

				// Check all non-TALK_TO objectives are complete
				List<QuestObjective> objectives = sideQuest.getObjectives();
				for (int i = 0; i < objectives.size(); i++) {
					QuestObjective obj = objectives.get(i);
					if (obj.getType() == QuestObjective.ObjectiveType.TALK_TO) continue;
					int progress = sqData.getObjectiveProgress(sideQuestId, i);
					if (progress < obj.getRequired()) return; // Not ready for turn-in
				}

				// Complete TALK_TO objectives matching this NPC
				for (int i = 0; i < objectives.size(); i++) {
					QuestObjective obj = objectives.get(i);
					if (obj instanceof TalkToObjective talkObj && talkObj.getNpcId().equals(npcId)) {
						sqData.setObjectiveProgress(sideQuestId, i, obj.getRequired());
					}
				}

				// Complete the quest
				sqData.completeQuest(sideQuestId);

				// Grant all rewards
				for (QuestReward reward : sideQuest.getRewards()) {
					reward.giveReward(player);
				}

				player.displayClientMessage(
						Component.translatable("command.dragonminez.story.sidequest.turned_in", sideQuestId), false);

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}
}

