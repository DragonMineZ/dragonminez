package com.dragonminez.common.network.C2S;

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

public class ToggleStoryHardModeC2S {

	// Delay to prevent packet explosion
	private static final long TOGGLE_COOLDOWN_MS = 500L;

	/** Last accepted toggle timestamp per player. Mutated only on the server main thread. */
	private static final Map<UUID, Long> LAST_TOGGLE = new HashMap<>();

	private final boolean enabled;

	public ToggleStoryHardModeC2S(boolean enabled) {
		this.enabled = enabled;
	}

	public ToggleStoryHardModeC2S(FriendlyByteBuf buffer) {
		this.enabled = buffer.readBoolean();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBoolean(enabled);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			// Only the party leader (or a solo player) may change the shared setting.
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
				if (pqd.isHardModeEnabled() == enabled) {
					return;
				}
				pqd.setHardModeEnabled(enabled);
				// Pushes the new value to the leader and every party member (no-op resync for solo).
				PartyManager.syncPartyQuestState(controller);
			});
		});
		context.setPacketHandled(true);
	}
}
