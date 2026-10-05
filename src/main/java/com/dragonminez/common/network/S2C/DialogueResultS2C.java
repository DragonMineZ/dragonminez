package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Getter
public class DialogueResultS2C {
	private final String npcId;
	private final String action;
	private final boolean success;

	public DialogueResultS2C(String npcId, String action, boolean success) {
		this.npcId = npcId == null ? "" : npcId;
		this.action = action == null ? "" : action;
		this.success = success;
	}

	public DialogueResultS2C(FriendlyByteBuf buf) {
		this.npcId = buf.readUtf(64);
		this.action = buf.readUtf(320);
		this.success = buf.readBoolean();
	}

	public static String npcAction(int actionId) {
		return "action:" + actionId;
	}

	public static String questStart(String questId) {
		return "quest_start:" + questId;
	}

	public static String questTurnIn(String questId) {
		return "quest_turn_in:" + questId;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(npcId, 64);
		buf.writeUtf(action, 320);
		buf.writeBoolean(success);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleDialogueResult(this)));
		context.setPacketHandled(true);
	}
}
