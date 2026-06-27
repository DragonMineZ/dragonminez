package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class RadarSyncS2C {
	private final List<BlockPos> earthPositions;
	private final List<BlockPos> namekPositions;
	private final Map<String, List<BlockPos>> positionsBySet;

	public RadarSyncS2C(List<BlockPos> earthPositions, List<BlockPos> namekPositions) {
		this(earthPositions, namekPositions, Map.of("earth", earthPositions, "namek", namekPositions));
	}

	public RadarSyncS2C(List<BlockPos> earthPositions, List<BlockPos> namekPositions, Map<String, List<BlockPos>> positionsBySet) {
		this.earthPositions = earthPositions;
		this.namekPositions = namekPositions;
		this.positionsBySet = positionsBySet;
	}

	public static void encode(RadarSyncS2C msg, FriendlyByteBuf buf) {
		buf.writeCollection(msg.earthPositions, FriendlyByteBuf::writeBlockPos);
		buf.writeCollection(msg.namekPositions, FriendlyByteBuf::writeBlockPos);
		buf.writeInt(msg.positionsBySet.size());
		for (Map.Entry<String, List<BlockPos>> entry : msg.positionsBySet.entrySet()) {
			buf.writeUtf(entry.getKey());
			buf.writeCollection(entry.getValue(), FriendlyByteBuf::writeBlockPos);
		}
	}

	public static RadarSyncS2C decode(FriendlyByteBuf buf) {
		List<BlockPos> earth = buf.readList(FriendlyByteBuf::readBlockPos);
		List<BlockPos> namek = buf.readList(FriendlyByteBuf::readBlockPos);
		int size = buf.readInt();
		Map<String, List<BlockPos>> positionsBySet = new HashMap<>();
		for (int i = 0; i < size; i++) {
			positionsBySet.put(buf.readUtf(), buf.readList(FriendlyByteBuf::readBlockPos));
		}
		positionsBySet.put("earth", earth);
		positionsBySet.put("namek", namek);
		return new RadarSyncS2C(earth, namek, positionsBySet);
	}

	public static void handle(RadarSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientPacketHandler.handleRadarSyncPacket(msg.earthPositions, msg.namekPositions, msg.positionsBySet)));
		ctx.get().setPacketHandled(true);
	}
}
