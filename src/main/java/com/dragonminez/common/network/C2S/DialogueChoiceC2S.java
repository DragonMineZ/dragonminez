package com.dragonminez.common.network.C2S;

import com.dragonminez.common.dialogue.DialogueService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The player's pick on a dialogue node; fully re-validated server-side by DialogueService. */
public class DialogueChoiceC2S {

	private final String npcId;
	private final int entityId;
	private final String nodeId;
	private final int choiceIndex;

	public DialogueChoiceC2S(String npcId, int entityId, String nodeId, int choiceIndex) {
		this.npcId = npcId == null ? "" : npcId;
		this.entityId = entityId;
		this.nodeId = nodeId == null ? "" : nodeId;
		this.choiceIndex = choiceIndex;
	}

	public static void encode(DialogueChoiceC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.npcId);
		buf.writeInt(msg.entityId);
		buf.writeUtf(msg.nodeId);
		buf.writeVarInt(msg.choiceIndex);
	}

	public static DialogueChoiceC2S decode(FriendlyByteBuf buf) {
		return new DialogueChoiceC2S(buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readVarInt());
	}

	public static void handle(DialogueChoiceC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null) return;
			DialogueService.handleChoice(sender, msg.npcId, msg.entityId, msg.nodeId, msg.choiceIndex);
		});
		ctx.get().setPacketHandled(true);
	}
}
