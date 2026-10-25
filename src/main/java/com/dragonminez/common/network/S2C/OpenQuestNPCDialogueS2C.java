package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.common.quest.QuestService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenQuestNPCDialogueS2C {
	private final String npcId;
	private final List<String> offerableQuestIds;
	private final List<String> turnInQuestIds;
	private final List<String> inProgressQuestIds;
	private final List<QuestService.LockedNpcQuest> lockedQuests;
	private final boolean masterNpc;
	private final boolean npcDisliked;
	private final int entityId;

	public OpenQuestNPCDialogueS2C(String npcId, QuestService.NPCQuestOptions options, boolean masterNpc, boolean npcDisliked, int entityId) {
		this.npcId = npcId;
		this.offerableQuestIds = options.offerableQuestIds();
		this.turnInQuestIds = options.turnInQuestIds();
		this.inProgressQuestIds = options.inProgressQuestIds();
		this.lockedQuests = options.lockedQuests();
		this.masterNpc = masterNpc;
		this.npcDisliked = npcDisliked;
		this.entityId = entityId;
	}

	public OpenQuestNPCDialogueS2C(FriendlyByteBuf buffer) {
		this.npcId = buffer.readUtf();
		this.offerableQuestIds = readIds(buffer);
		this.turnInQuestIds = readIds(buffer);
		this.inProgressQuestIds = readIds(buffer);
		int lockedCount = buffer.readVarInt();
		this.lockedQuests = new ArrayList<>(lockedCount);
		for (int i = 0; i < lockedCount; i++) {
			String questId = buffer.readUtf();
			Component reason = buffer.readJsonWithCodec(ComponentSerialization.CODEC);
			long cooldownSeconds = buffer.readVarLong();
			lockedQuests.add(new QuestService.LockedNpcQuest(questId, reason, cooldownSeconds));
		}
		this.masterNpc = buffer.readBoolean();
		this.npcDisliked = buffer.readBoolean();
		this.entityId = buffer.readVarInt();
	}

	private static List<String> readIds(FriendlyByteBuf buffer) {
		int count = buffer.readVarInt();
		List<String> ids = new ArrayList<>(count);
		for (int i = 0; i < count; i++) ids.add(buffer.readUtf());
		return ids;
	}

	private static void writeIds(FriendlyByteBuf buffer, List<String> ids) {
		buffer.writeVarInt(ids.size());
		for (String id : ids) buffer.writeUtf(id);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(npcId);
		writeIds(buffer, offerableQuestIds);
		writeIds(buffer, turnInQuestIds);
		writeIds(buffer, inProgressQuestIds);
		buffer.writeVarInt(lockedQuests.size());
		for (QuestService.LockedNpcQuest locked : lockedQuests) {
			buffer.writeUtf(locked.questId());
			buffer.writeJsonWithCodec(ComponentSerialization.CODEC, locked.reason());
			buffer.writeVarLong(locked.cooldownSeconds());
		}
		buffer.writeBoolean(masterNpc);
		buffer.writeBoolean(npcDisliked);
		buffer.writeVarInt(entityId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenQuestNpcDialoguePacket(this)));
		context.setPacketHandled(true);
	}

	public String getNpcId() {
		return this.npcId;
	}

	public List<String> getOfferableQuestIds() {
		return this.offerableQuestIds;
	}

	public List<String> getTurnInQuestIds() {
		return this.turnInQuestIds;
	}

	public List<String> getInProgressQuestIds() {
		return this.inProgressQuestIds;
	}

	public List<QuestService.LockedNpcQuest> getLockedQuests() {
		return this.lockedQuests;
	}

	public boolean isMasterNpc() {
		return this.masterNpc;
	}

	public boolean isNpcDisliked() {
		return this.npcDisliked;
	}

	public int getEntityId() {
		return this.entityId;
	}
}
