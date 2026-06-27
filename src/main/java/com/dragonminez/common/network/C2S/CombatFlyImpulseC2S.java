package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Status;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CombatFlyImpulseC2S {

	private final byte direction;

	public CombatFlyImpulseC2S(int direction) {
		this.direction = (byte) direction;
	}

	public CombatFlyImpulseC2S(FriendlyByteBuf buf) {
		this.direction = buf.readByte();
	}

	public static void encode(CombatFlyImpulseC2S msg, FriendlyByteBuf buf) {
		buf.writeByte(msg.direction);
	}

	public static CombatFlyImpulseC2S decode(FriendlyByteBuf buf) {
		return new CombatFlyImpulseC2S(buf);
	}

	public static void handle(CombatFlyImpulseC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) return;
				if (!data.getSkills().isSkillActive("fly")) return;
				if (data.getStatus().getFlightMode() != Status.FLIGHT_COMBAT) return;
				if (data.getCooldowns().hasCooldown(Cooldowns.COMBATFLY_IMPULSE_CD)) return;

				if (player.isCreative() || player.isSpectator()) {
					data.getCooldowns().setCooldown(Cooldowns.COMBATFLY_IMPULSE_CD, ConfigManager.getCombatConfig().getCombatFlyImpulseCooldownTicks());
					return;
				}

				int kiCost = (int) Math.ceil(ConfigManager.getCombatConfig().getBaselineFormDrain() * ConfigManager.getCombatConfig().getCombatFlyImpulseKiCostPct());
				if (data.getResources().getCurrentEnergy() < kiCost) return;

				data.getResources().removeEnergy(kiCost);
				data.getCooldowns().setCooldown(Cooldowns.COMBATFLY_IMPULSE_CD, ConfigManager.getCombatConfig().getCombatFlyImpulseCooldownTicks());

				NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}
}
