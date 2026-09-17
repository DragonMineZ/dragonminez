package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairSanitizer;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateCustomHairC2S {
	private static final long MIN_INTERVAL_TICKS = 10L;

	private final int hairIndex;
	private final CustomHair customHair;

	public UpdateCustomHairC2S(HairStyleSlot slot, CustomHair customHair) {
		this(slot.index(), customHair);
	}

	private UpdateCustomHairC2S(int hairIndex, CustomHair customHair) {
		this.hairIndex = hairIndex;
		this.customHair = customHair;
	}

	public static void encode(UpdateCustomHairC2S msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.hairIndex);
		boolean hasHair = msg.customHair != null;
		buf.writeBoolean(hasHair);
		if (hasHair) msg.customHair.writeToBuffer(buf);
	}

	public static UpdateCustomHairC2S decode(FriendlyByteBuf buf) {
		int hairIndex = buf.readInt();
		boolean hasHair = buf.readBoolean();
		CustomHair hair = hasHair ? CustomHair.readFromBuffer(buf) : null;
		return new UpdateCustomHairC2S(hairIndex, hair);
	}

	public static void handle(UpdateCustomHairC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null || msg.customHair == null) return;

			String playerName = player.getGameProfile().getName();
			HairStyleSlot slot = HairStyleSlot.byIndex(msg.hairIndex);
			if (slot == null) {
				LogUtil.warn(Env.SERVER, "Rejected hair update from {}: invalid style index {}", playerName, msg.hairIndex);
				return;
			}
			if (!PacketRateLimiter.allow(player.getUUID(), "hair_style_" + slot.name(), player.level().getGameTime(), MIN_INTERVAL_TICKS)) {
				LogUtil.debug(Env.SERVER, "Rate limited hair update from {} for style {}", playerName, slot);
				return;
			}

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				HairSanitizer.sanitizeAndLog(msg.customHair, slot, playerName);
				data.getCharacter().setHairStyle(slot, msg.customHair);
				data.getCharacter().setHairId(0);
				NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
