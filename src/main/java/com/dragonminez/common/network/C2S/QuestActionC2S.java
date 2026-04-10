package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.QuestService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QuestActionC2S {
	public enum ActionType {
		START,
		TURN_IN
	}

	private final ActionType actionType;
	private final String questId;
	private final boolean isHardMode;
	private final String npcId;

	public QuestActionC2S(ActionType actionType, String questId, boolean isHardMode, String npcId) {
		this.actionType = actionType;
		this.questId = questId;
		this.isHardMode = isHardMode;
		this.npcId = npcId != null ? npcId : "";
	}

	public QuestActionC2S(FriendlyByteBuf buffer) {
		this.actionType = buffer.readEnum(ActionType.class);
		this.questId = buffer.readUtf();
		this.isHardMode = buffer.readBoolean();
		this.npcId = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeEnum(actionType);
		buffer.writeUtf(questId);
		buffer.writeBoolean(isHardMode);
		buffer.writeUtf(npcId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) {
				return;
			}

			Component failure = switch (actionType) {
				case START -> QuestService.startQuest(player, questId, isHardMode);
				case TURN_IN -> QuestService.turnInQuest(player, questId, npcId);
			};

			if (failure != null) {
				player.displayClientMessage(failure.copy().withStyle(ChatFormatting.RED), true);
			}
		});
		context.setPacketHandled(true);
	}
}
