package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SkinPixelsSyncS2C {
	private final int playerId;
	private final CompoundTag tag;

	public SkinPixelsSyncS2C(ServerPlayer player) {
		this.playerId = player.getId();
		CompoundTag pixels = new CompoundTag();
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> pixels.merge(data.getCharacter().getSkinPixels().save()));
		this.tag = pixels;
	}

	public SkinPixelsSyncS2C(int playerId, CompoundTag tag) {
		this.playerId = playerId;
		this.tag = tag;
	}

	public static void encode(SkinPixelsSyncS2C msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.playerId);
		buf.writeNbt(msg.tag);
	}

	public static SkinPixelsSyncS2C decode(FriendlyByteBuf buf) {
		return new SkinPixelsSyncS2C(buf.readInt(), buf.readNbt());
	}

	public static void handle(SkinPixelsSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSkinPixelsSync(msg.playerId, msg.tag)));
		ctx.get().setPacketHandled(true);
	}
}
