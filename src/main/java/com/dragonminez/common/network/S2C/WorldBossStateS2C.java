package com.dragonminez.common.network.S2C;

import com.dragonminez.client.systems.worldboss.ClientWorldBossState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WorldBossStateS2C {

	private final boolean lairKnown;
	private final BlockPos lair;
	private final boolean bossAlive;
	private final long respawnTicks;

	public WorldBossStateS2C(boolean lairKnown, BlockPos lair, boolean bossAlive, long respawnTicks) {
		this.lairKnown = lairKnown;
		this.lair = lair == null ? BlockPos.ZERO : lair;
		this.bossAlive = bossAlive;
		this.respawnTicks = respawnTicks;
	}

	public WorldBossStateS2C(FriendlyByteBuf buf) {
		this.lairKnown = buf.readBoolean();
		this.lair = buf.readBlockPos();
		this.bossAlive = buf.readBoolean();
		this.respawnTicks = buf.readVarLong();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(lairKnown);
		buf.writeBlockPos(lair);
		buf.writeBoolean(bossAlive);
		buf.writeVarLong(Math.max(0L, respawnTicks));
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientWorldBossState.update(lairKnown, lair, bossAlive, respawnTicks)));
		ctx.get().setPacketHandled(true);
	}
}
