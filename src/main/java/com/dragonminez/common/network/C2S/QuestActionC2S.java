package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.QuestActionFeedbackS2C;
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
		RESUMMON,
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

			try {
				Component failure = switch (actionType) {
					case START -> QuestService.startQuest(player, questId, isHardMode);
					case RESUMMON -> QuestService.resummonQuest(player, questId);
					case TURN_IN -> QuestService.turnInQuest(player, questId, npcId);
				};

				if (failure != null) {
					NetworkHandler.sendToPlayer(new QuestActionFeedbackS2C(
							failure.copy().withStyle(ChatFormatting.RED)), player);
				}
			} catch (Exception exception) {
				LogUtil.error(Env.SERVER, "Failed to handle quest action " + actionType + " for quest '"
						+ questId + "' requested by " + player.getGameProfile().getName(), exception);
				NetworkHandler.sendToPlayer(new QuestActionFeedbackS2C(
						Component.translatable("message.dragonminez.quest.start.unavailable")
								.withStyle(ChatFormatting.RED)), player);
			}
		});
		context.setPacketHandled(true);
	}
}
