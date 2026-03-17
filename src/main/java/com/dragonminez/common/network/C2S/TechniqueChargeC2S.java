package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TechniqueChargeC2S {
	private final boolean charging;

	public TechniqueChargeC2S(boolean charging) {
		this.charging = charging;
	}

	public TechniqueChargeC2S(FriendlyByteBuf buf) {
		this.charging = buf.readBoolean();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeBoolean(this.charging);
	}

	public static void handle(TechniqueChargeC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()) {
					data.getTechniques().clearTechniqueCharge();
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
					return;
				}

				if (msg.charging) {
					TechniqueData selected = data.getTechniques().getSelectedTechnique();
					if (selected instanceof KiAttackData kiAttack) {
						String cooldownKey = "TechniqueCooldown_" + kiAttack.getId();
						if (data.getCooldowns().hasCooldown(cooldownKey)) {
							data.getTechniques().clearTechniqueCharge();
						} else {
							data.getTechniques().startTechniqueCharge(kiAttack.getId());
						}
					} else {
						data.getTechniques().clearTechniqueCharge();
					}
				} else {
					data.getTechniques().setTechniqueCharging(false);
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});

		ctx.get().setPacketHandled(true);
	}
}

