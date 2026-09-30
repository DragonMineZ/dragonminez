package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.SkinPixelsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.SkinPixels;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateSkinPixelsC2S {
	private static final long MIN_INTERVAL_TICKS = 10L;

	private final CompoundTag tag;

	public UpdateSkinPixelsC2S(SkinPixels pixels) {
		this.tag = pixels.save();
	}

	private UpdateSkinPixelsC2S(CompoundTag tag) {
		this.tag = tag;
	}

	public static void encode(UpdateSkinPixelsC2S msg, FriendlyByteBuf buf) {
		buf.writeNbt(msg.tag);
	}

	public static UpdateSkinPixelsC2S decode(FriendlyByteBuf buf) {
		return new UpdateSkinPixelsC2S(buf.readNbt());
	}

	public static void handle(UpdateSkinPixelsC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null || msg.tag == null) return;

			String playerName = player.getGameProfile().getName();
			if (!PacketRateLimiter.allow(player.getUUID(), "skin_pixels", player.level().getGameTime(), MIN_INTERVAL_TICKS)) {
				LogUtil.debug(Env.SERVER, "Rate limited skin pixel update from {}", playerName);
				return;
			}

			SkinPixels parsed;
			try {
				parsed = SkinPixels.parse(msg.tag, true);
			} catch (IllegalArgumentException e) {
				LogUtil.warn(Env.SERVER, "Rejected skin pixel update from {}: {}", playerName, e.getMessage());
				return;
			}

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getCharacter().getSkinPixels().copyFrom(parsed);
				NetworkHandler.sendToTrackingEntityAndSelf(new SkinPixelsSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
