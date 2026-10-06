package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.skills.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FlightModeLockC2S {
	public FlightModeLockC2S() {}

	public FlightModeLockC2S(FriendlyByteBuf buf) {}

	public static void encode(FlightModeLockC2S msg, FriendlyByteBuf buf) {}

	public static FlightModeLockC2S decode(FriendlyByteBuf buf) {
		return new FlightModeLockC2S(buf);
	}

	public static void handle(FlightModeLockC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()) return;

				Skill fly = data.getSkills().getSkill("fly");
				Skill kiControl = data.getSkills().getSkill("kicontrol");
				if (fly == null || fly.getLevel() <= 0 || kiControl == null || kiControl.getLevel() <= 0) return;

				boolean lock = !data.getStatus().isFlightModeLocked();
				data.getStatus().setFlightModeLocked(lock);
				if (lock) data.getStatus().setLockedFlightMode(data.getStatus().getFlightMode());
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
