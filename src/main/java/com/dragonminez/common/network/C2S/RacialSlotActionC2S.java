package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.RacialDataSyncS2C;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.racial.impl.MajinAbsorption;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RacialSlotActionC2S {

	public enum SlotAction {
		EJECT,

		SELECT_BIO_SKILL
	}

	private final SlotAction action;
	private final int index;

	public RacialSlotActionC2S(SlotAction action, int index) {
		this.action = action;
		this.index = index;
	}

	public RacialSlotActionC2S(FriendlyByteBuf buf) {
		this.action = buf.readEnum(SlotAction.class);
		this.index = buf.readVarInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeEnum(action);
		buf.writeVarInt(index);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "racial_slot_action", player.level().getGameTime(), 20L)) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				String race = data.getCharacter().getRaceName();
				switch (action) {
					case EJECT -> {
						if ("majin".equals(race)) MajinAbsorption.ejectSlot(player, data, index);
					}
					case SELECT_BIO_SKILL -> {
						if (!"bioandroid".equals(race)) return;

						if (data.getRacialData().getBioSwell() > 0f) {
							player.displayClientMessage(Component.translatable("message.dragonminez.racial.bioandroid.explode_reverting"), true);
							return;
						}
						data.getRacialData().setBioSelectedSkill(index == 1
								? RacialData.BIO_SKILL_EXPLODE : RacialData.BIO_SKILL_DRAIN);
						NetworkHandler.sendToPlayer(new RacialDataSyncS2C(player), player);
					}
				}
			});
		});
		context.setPacketHandled(true);
	}
}
