package com.dragonminez.common.network.S2C;

import com.dragonminez.client.gui.quest.DialogueScreen;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class OpenDialogueNodeS2C {

	private final String npcId;
	private final int entityId;
	private final String nodeId;
	private final String line;
	private final List<String> choiceTexts;
	private final List<Integer> choiceIndices;

	public OpenDialogueNodeS2C(String npcId, int entityId, String nodeId, String line,
							   List<String> choiceTexts, List<Integer> choiceIndices) {
		this.npcId = npcId == null ? "" : npcId;
		this.entityId = entityId;
		this.nodeId = nodeId == null ? "" : nodeId;
		this.line = line == null ? "" : line;
		this.choiceTexts = choiceTexts != null ? choiceTexts : new ArrayList<>();
		this.choiceIndices = choiceIndices != null ? choiceIndices : new ArrayList<>();
	}

	public static OpenDialogueNodeS2C close(String npcId, int entityId) {
		return new OpenDialogueNodeS2C(npcId, entityId, "", "", List.of(), List.of());
	}

	public boolean isClose() {
		return nodeId.isEmpty();
	}

	public static void encode(OpenDialogueNodeS2C msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.npcId);
		buf.writeInt(msg.entityId);
		buf.writeUtf(msg.nodeId);
		buf.writeUtf(msg.line);
		buf.writeVarInt(msg.choiceTexts.size());
		for (int i = 0; i < msg.choiceTexts.size(); i++) {
			buf.writeUtf(msg.choiceTexts.get(i));
			buf.writeVarInt(msg.choiceIndices.get(i));
		}
	}

	public static OpenDialogueNodeS2C decode(FriendlyByteBuf buf) {
		String npcId = buf.readUtf();
		int entityId = buf.readInt();
		String nodeId = buf.readUtf();
		String line = buf.readUtf();
		int count = buf.readVarInt();
		List<String> texts = new ArrayList<>(count);
		List<Integer> indices = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			texts.add(buf.readUtf());
			indices.add(buf.readVarInt());
		}
		return new OpenDialogueNodeS2C(npcId, entityId, nodeId, line, texts, indices);
	}

	public static void handle(OpenDialogueNodeS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> DialogueScreen.handleNodePacket(msg)));
		ctx.get().setPacketHandled(true);
	}
}
