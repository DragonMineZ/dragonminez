package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FlightModeC2S {
	private final int targetMode;

	public FlightModeC2S() {
		this.targetMode = -1;
	}

	public FlightModeC2S(int targetMode) {
		this.targetMode = targetMode;
	}

	public FlightModeC2S(FriendlyByteBuf buf) {
		this.targetMode = buf.readInt();
	}

	public static void encode(FlightModeC2S msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.targetMode);
	}

	public static FlightModeC2S decode(FriendlyByteBuf buf) {
		return new FlightModeC2S(buf);
	}

	public static void handle(FlightModeC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) return;
				if (data.getStatus().isStunned()) return;

				Skill flySkill = data.getSkills().getSkill("fly");
				Skill kiControlSkill = data.getSkills().getSkill("kicontrol");
				if (flySkill == null || kiControlSkill == null || flySkill.getLevel() <= 0 || kiControlSkill.getLevel() <= 0) return;

				int currentMode = data.getStatus().getFlightMode();
				int targetMode = msg.targetMode == -1
						? (currentMode == Status.FLIGHT_COMBAT ? Status.FLIGHT_SEARCH : Status.FLIGHT_COMBAT)
						: msg.targetMode;
				if ((targetMode != Status.FLIGHT_SEARCH && targetMode != Status.FLIGHT_COMBAT) || targetMode == currentMode) return;

				if (targetMode == Status.FLIGHT_SEARCH && data.getCooldowns().hasCooldown(Cooldowns.COMBAT_FLY_LOCK)) return;

				boolean wasActive = flySkill.isActive();

				if (!wasActive && msg.targetMode == -1) {
					int flyLevel = flySkill.getLevel();
					double energyCostPercent = Math.max(0.01, 0.04 - (flyLevel * 0.003));
					int energyCost = (int) Math.ceil(ConfigManager.getCombatConfig().getBaselineFormDrain() * energyCostPercent);
					if (data.getResources().getCurrentEnergy() < energyCost) return;

					flySkill.setActive(true);
					player.getAbilities().mayfly = true;
					player.getAbilities().flying = false;
					player.onUpdateAbilities();
				}

				data.getStatus().setFlightMode(targetMode);

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
