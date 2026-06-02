package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TechniqueChargeC2S {
	public enum Action { START, SET_HOLDING, BUMP, CANCEL, FIRE_NOW }

	private final Action action;
	private final int slot;
	private final boolean holding;

	private TechniqueChargeC2S(Action action, int slot, boolean holding) {
		this.action = action;
		this.slot = slot;
		this.holding = holding;
	}

	public static TechniqueChargeC2S start(int slot) {
		return new TechniqueChargeC2S(Action.START, slot, false);
	}

	public static TechniqueChargeC2S setHolding(boolean holding) {
		return new TechniqueChargeC2S(Action.SET_HOLDING, -1, holding);
	}

	public static TechniqueChargeC2S bump() {
		return new TechniqueChargeC2S(Action.BUMP, -1, false);
	}

	public static TechniqueChargeC2S cancel() {
		return new TechniqueChargeC2S(Action.CANCEL, -1, false);
	}

	public static TechniqueChargeC2S fireNow() {
		return new TechniqueChargeC2S(Action.FIRE_NOW, -1, false);
	}

	public TechniqueChargeC2S(FriendlyByteBuf buf) {
		this.action = buf.readEnum(Action.class);
		this.slot = buf.readVarInt();
		this.holding = buf.readBoolean();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeEnum(this.action);
		buf.writeVarInt(this.slot);
		buf.writeBoolean(this.holding);
	}

	public static void handle(TechniqueChargeC2S msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned()
						|| data.getStatus().isStrikeLocked() || data.getStatus().isKnockedDown()) {
					data.getTechniques().clearTechniqueCharge();
					NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
					return;
				}

				switch (msg.action) {
					case START -> {
						if (msg.slot < 0 || msg.slot >= 5) {
							data.getTechniques().clearTechniqueCharge();
							break;
						}
						String techId = data.getTechniques().getEquippedSlots()[msg.slot];
						TechniqueData selected = (techId == null || techId.isEmpty())
								? null : data.getTechniques().getUnlockedTechniques().get(techId);
						boolean meetsRequirements = data.getSkills().getSkillLevel("kicontrol") > 0
								&& data.getResources().getPowerRelease() >= 5
								&& player.getMainHandItem().isEmpty();
						if (selected instanceof KiAttackData kiAttack && meetsRequirements) {
							String cooldownKey = "TechniqueCooldown_" + kiAttack.getId();
							if (data.getCooldowns().hasCooldown(cooldownKey)) {
								data.getTechniques().clearTechniqueCharge();
							} else {
								data.getTechniques().selectSlot(msg.slot);
								data.getTechniques().startTechniqueCharge(kiAttack.getId());
							}
						} else {
							data.getTechniques().clearTechniqueCharge();
						}
					}
					case SET_HOLDING -> {
						if (data.getTechniques().isTechniqueChargeActive() || data.getTechniques().isTechniqueCharging()) {
							data.getTechniques().setChargeHolding(msg.holding);
						}
					}
					case BUMP -> {
						if (data.getTechniques().isTechniqueChargeActive() || data.getTechniques().isTechniqueCharging()) {
							data.getTechniques().bumpChargeTier();
						}
					}
					case CANCEL -> {
						if (data.getTechniques().getTechniqueChargePercent() < 50.0f) {
							data.getTechniques().clearTechniqueCharge();
						}
					}
					case FIRE_NOW -> {
						if (data.getTechniques().isTechniqueChargeActive() || data.getTechniques().isTechniqueCharging()) {
							data.getTechniques().setChargeHolding(false);
							data.getTechniques().setChargeTierCeiling(data.getTechniques().getTechniqueChargePercent());
						}
					}
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		});

		ctx.get().setPacketHandled(true);
	}
}
