package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class SetStoryDifficultyC2S {
	private static final long TOGGLE_COOLDOWN_MS = 500L;

	private static final Map<UUID, Long> LAST_TOGGLE = new HashMap<>();

	private final Difficulty difficulty;

	public SetStoryDifficultyC2S(Difficulty difficulty) {
		this.difficulty = difficulty != null ? difficulty : Difficulty.NORMAL;
	}

	public SetStoryDifficultyC2S(FriendlyByteBuf buffer) {
		this.difficulty = Difficulty.fromOrdinal(buffer.readVarInt());
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(difficulty.ordinal());
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			if (PartyManager.isInParty(player) && !PartyManager.isPartyLeader(player)) {
				return;
			}

			long now = System.currentTimeMillis();
			Long last = LAST_TOGGLE.get(player.getUUID());
			if (last != null && now - last < TOGGLE_COOLDOWN_MS) {
				return;
			}
			LAST_TOGGLE.put(player.getUUID(), now);

			ServerPlayer controller = PartyManager.resolveQuestController(player);
			if (controller == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, controller).ifPresent(data -> {
				PlayerQuestData pqd = data.getPlayerQuestData();
				boolean alreadyChosen = pqd.isDifficultyChosen();
				boolean sameDifficulty = pqd.getDifficulty() == difficulty;
				if (alreadyChosen && sameDifficulty) {
					return;
				}
				pqd.setDifficulty(difficulty);
				pqd.setDifficultyChosen(true);
				PartyManager.syncPartyQuestState(controller);
			});
		});
		context.setPacketHandled(true);
	}
}
