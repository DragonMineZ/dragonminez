package com.dragonminez.common.network.S2C;

import com.dragonminez.client.systems.worldboss.ClientWorldBossPlayerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WorldBossPlayerStateS2C {
	private final boolean inFight;
	private final boolean knockedOut;
	private final int livesLeft;
	private final boolean casting;
	private final float castProgress;
	private final String castTargetName;
	private final boolean beingRevived;
	private final String reviverName;

	public WorldBossPlayerStateS2C(boolean inFight, boolean knockedOut, int livesLeft, boolean casting, float castProgress,
								   String castTargetName, boolean beingRevived, String reviverName) {
		this.inFight = inFight;
		this.knockedOut = knockedOut;
		this.livesLeft = livesLeft;
		this.casting = casting;
		this.castProgress = castProgress;
		this.castTargetName = castTargetName == null ? "" : castTargetName;
		this.beingRevived = beingRevived;
		this.reviverName = reviverName == null ? "" : reviverName;
	}

	public static WorldBossPlayerStateS2C cleared() {
		return new WorldBossPlayerStateS2C(false, false, 0, false, 0.0f, "", false, "");
	}

	public WorldBossPlayerStateS2C(FriendlyByteBuf buf) {
		this.inFight = buf.readBoolean();
		this.knockedOut = buf.readBoolean();
		this.livesLeft = buf.readVarInt();
		this.casting = buf.readBoolean();
		this.castProgress = buf.readFloat();
		this.castTargetName = buf.readUtf();
		this.beingRevived = buf.readBoolean();
		this.reviverName = buf.readUtf();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(inFight);
		buf.writeBoolean(knockedOut);
		buf.writeVarInt(Math.max(0, livesLeft));
		buf.writeBoolean(casting);
		buf.writeFloat(castProgress);
		buf.writeUtf(castTargetName);
		buf.writeBoolean(beingRevived);
		buf.writeUtf(reviverName);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ClientWorldBossPlayerState.accept(inFight, knockedOut, livesLeft, casting, castProgress,
						castTargetName, beingRevived, reviverName)));
		ctx.get().setPacketHandled(true);
	}
}
