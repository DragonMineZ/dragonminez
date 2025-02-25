package com.yuseix.dragonminez.network.C2S;

import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.network.S2C.UpdateDragonRadarS2C;
import com.yuseix.dragonminez.world.DragonBallGenProvider;
import com.yuseix.dragonminez.world.NamekDragonBallGenProvider;
import com.yuseix.dragonminez.worldgen.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class DragonRadarC2S {

	public DragonRadarC2S() {}

	public static void encode(DragonRadarC2S msg, FriendlyByteBuf buf) {
	}

	public static DragonRadarC2S decode(FriendlyByteBuf buf) {
		return new DragonRadarC2S();
	}

	public static void handle(DragonRadarC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) {
				Level level = player.level();
				if (level.isClientSide()) return; // Solo manejar en el servidor

				List<BlockPos> positions = null;
				if (level.getCapability(DragonBallGenProvider.CAPABILITY).isPresent()) {
					positions = level.getCapability(DragonBallGenProvider.CAPABILITY)
							.orElseThrow(() -> new IllegalStateException("DragonBallGenProvider not found"))
							.DragonBallPositions();
				} else if (level.getCapability(NamekDragonBallGenProvider.CAPABILITY).isPresent()) {
					positions = level.getCapability(NamekDragonBallGenProvider.CAPABILITY)
							.orElseThrow(() -> new IllegalStateException("NamekDragonBallGenProvider not found"))
							.namekDragonBallPositions();
				}

				if (positions != null) {
					ModMessages.sendToPlayer(new UpdateDragonRadarS2C(positions), player);
				}
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
