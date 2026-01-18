package com.dragonminez.common.network.S2C;

import com.dragonminez.client.events.RadarRenderEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class RadarSyncS2C {
	private final List<BlockPos> earthPositions;
	private final List<BlockPos> namekPositions;

	public RadarSyncS2C(List<BlockPos> earthPositions, List<BlockPos> namekPositions) {
		this.earthPositions = earthPositions;
		this.namekPositions = namekPositions;
	}

	public static void encode(RadarSyncS2C msg, FriendlyByteBuf buf) {
		buf.writeCollection(msg.earthPositions, FriendlyByteBuf::writeBlockPos);
		buf.writeCollection(msg.namekPositions, FriendlyByteBuf::writeBlockPos);
	}

	public static RadarSyncS2C decode(FriendlyByteBuf buf) {
		List<BlockPos> earth = buf.readList(FriendlyByteBuf::readBlockPos);
		List<BlockPos> namek = buf.readList(FriendlyByteBuf::readBlockPos);
		return new RadarSyncS2C(earth, namek);
	}

	public static void handle(RadarSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				RadarRenderEvent.updateRadarData(msg.earthPositions, msg.namekPositions);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
