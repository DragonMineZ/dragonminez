package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
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
	private final boolean masterNpc;
	private final int entityId;

	public OpenQuestNPCDialogueS2C(String npcId, List<String> offerableQuestIds,
									List<String> turnInQuestIds, List<String> inProgressQuestIds) {
		this(npcId, offerableQuestIds, turnInQuestIds, inProgressQuestIds, false, -1);
	}

	public OpenQuestNPCDialogueS2C(String npcId, List<String> offerableQuestIds,
									List<String> turnInQuestIds, List<String> inProgressQuestIds,
									boolean masterNpc, int entityId) {
		this.npcId = npcId;
		this.offerableQuestIds = offerableQuestIds;
		this.turnInQuestIds = turnInQuestIds;
		this.inProgressQuestIds = inProgressQuestIds;
		this.masterNpc = masterNpc;
		this.entityId = entityId;
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

		this.masterNpc = buffer.readBoolean();
		this.entityId = buffer.readVarInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(npcId);

		buffer.writeVarInt(offerableQuestIds.size());
		for (String id : offerableQuestIds) buffer.writeUtf(id);

		buffer.writeVarInt(turnInQuestIds.size());
		for (String id : turnInQuestIds) buffer.writeUtf(id);

		buffer.writeVarInt(inProgressQuestIds.size());
		for (String id : inProgressQuestIds) buffer.writeUtf(id);

		buffer.writeBoolean(masterNpc);
		buffer.writeVarInt(entityId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleOpenQuestNpcDialoguePacket(
						npcId, offerableQuestIds, turnInQuestIds, inProgressQuestIds, masterNpc, entityId)));
		context.setPacketHandled(true);
	}
}

