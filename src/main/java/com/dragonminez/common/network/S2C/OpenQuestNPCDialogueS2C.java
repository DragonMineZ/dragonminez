package com.dragonminez.common.network.S2C;

import com.dragonminez.client.gui.questnpc.QuestNPCDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sent from server to client when a player right-clicks a QuestNPCEntity.
 * Contains the NPC ID and lists of quest IDs the NPC can offer, accept turn-ins for, or has in progress.
 */
public class OpenQuestNPCDialogueS2C {
	private final String npcId;
	private final List<String> offerableQuestIds;
	private final List<String> turnInQuestIds;
	private final List<String> inProgressQuestIds;

	public OpenQuestNPCDialogueS2C(String npcId, List<String> offerableQuestIds,
									List<String> turnInQuestIds, List<String> inProgressQuestIds) {
		this.npcId = npcId;
		this.offerableQuestIds = offerableQuestIds;
		this.turnInQuestIds = turnInQuestIds;
		this.inProgressQuestIds = inProgressQuestIds;
	}

	public OpenQuestNPCDialogueS2C(FriendlyByteBuf buffer) {
		this.npcId = buffer.readUtf();
		int offerCount = buffer.readVarInt();
		this.offerableQuestIds = new ArrayList<>(offerCount);
		for (int i = 0; i < offerCount; i++) offerableQuestIds.add(buffer.readUtf());

		int turnInCount = buffer.readVarInt();
		this.turnInQuestIds = new ArrayList<>(turnInCount);
		for (int i = 0; i < turnInCount; i++) turnInQuestIds.add(buffer.readUtf());

		int progressCount = buffer.readVarInt();
		this.inProgressQuestIds = new ArrayList<>(progressCount);
		for (int i = 0; i < progressCount; i++) inProgressQuestIds.add(buffer.readUtf());
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(npcId);

		buffer.writeVarInt(offerableQuestIds.size());
		for (String id : offerableQuestIds) buffer.writeUtf(id);

		buffer.writeVarInt(turnInQuestIds.size());
		for (String id : turnInQuestIds) buffer.writeUtf(id);

		buffer.writeVarInt(inProgressQuestIds.size());
		for (String id : inProgressQuestIds) buffer.writeUtf(id);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleClient());
		context.setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	private void handleClient() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.setScreen(new QuestNPCDialogueScreen(npcId, offerableQuestIds, turnInQuestIds, inProgressQuestIds));
		}
	}
}

