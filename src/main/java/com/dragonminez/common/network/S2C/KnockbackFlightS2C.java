package com.dragonminez.common.network.S2C;

import com.dragonminez.client.flight.CombatFlightHandler;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Injects a server-applied knockback velocity into the local player's active flight handler.
 * Without this the client flight loop overwrites {@code setDeltaMovement} every tick, so a flying
 * player would ignore all knockback. Only sent to a flying player who is being launched.
 */
public class KnockbackFlightS2C {

	private final double x;
	private final double y;
	private final double z;

	public KnockbackFlightS2C(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public KnockbackFlightS2C(FriendlyByteBuf buf) {
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player == null) return;

			Vec3 knockback = new Vec3(x, y, z);
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getSkills().isSkillActive("fly")) return;
				if (data.getStatus().getFlightMode() == Status.FLIGHT_COMBAT) {
					CombatFlightHandler.injectKnockback(knockback);
				} else {
					FlySkillEvent.injectKnockback(knockback);
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
