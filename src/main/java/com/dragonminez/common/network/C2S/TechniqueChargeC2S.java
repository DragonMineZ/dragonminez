package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import com.dragonminez.common.stats.techniques.Techniques;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TechniqueChargeC2S {
	public enum Action { START, SET_HOLDING }

	private final Action action;
	private final int slot;
	private final boolean holding;
	private final int targetId;

	private TechniqueChargeC2S(Action action, int slot, boolean holding, int targetId) {
		this.action = action;
		this.slot = slot;
		this.holding = holding;
		this.targetId = targetId;
	}

	public static TechniqueChargeC2S start(int slot, int targetId) {
		return new TechniqueChargeC2S(Action.START, slot, false, targetId);
	}

	public static TechniqueChargeC2S setHolding(boolean holding) {
		return new TechniqueChargeC2S(Action.SET_HOLDING, -1, holding, -1);
	}

	public TechniqueChargeC2S(FriendlyByteBuf buf) {
		this.action = buf.readEnum(Action.class);
		this.slot = buf.readVarInt();
		this.holding = buf.readBoolean();
		this.targetId = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeEnum(this.action);
		buf.writeVarInt(this.slot);
		buf.writeBoolean(this.holding);
		buf.writeInt(this.targetId);
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
						if (player.isSpectator()
								|| (data.getStatus().isFused() && !data.getStatus().isFusionLeader())) {
							data.getTechniques().clearTechniqueCharge();
							break;
						}
						if (msg.slot < 0 || msg.slot >= Techniques.SLOT_COUNT) {
							data.getTechniques().clearTechniqueCharge();
							break;
						}
						String techId = data.getTechniques().getEquippedSlots()[msg.slot];
						TechniqueData selected = (techId == null || techId.isEmpty())
								? null : data.getTechniques().getUnlockedTechniques().get(techId);
						boolean meetsRequirements = data.getSkills().getSkillLevel("kicontrol") > 0
								&& data.getResources().getPowerRelease() >= 5
								&& player.getMainHandItem().isEmpty();
						if (selected instanceof KiAttackData kiAttack && meetsRequirements
								&& !data.getCooldowns().hasCooldown("TechniqueCooldown_" + kiAttack.getId())) {
							if (player.isPassenger() && TechniqueDispatcher.restrictsMovementWhileCharging(kiAttack.getKiType())) {
								data.getTechniques().clearTechniqueCharge();
								break;
							}
							data.getTechniques().selectSlot(msg.slot);
							data.getTechniques().startTechniqueCharge(kiAttack.getId());
							data.getTechniques().setHomingTargetId(msg.targetId);
							net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
									new com.dragonminez.common.events.DMZEvent.KiAttackCastEvent(player, data, kiAttack));
						} else {
							data.getTechniques().clearTechniqueCharge();
						}
					}
					case SET_HOLDING -> {
						if (data.getTechniques().isTechniqueChargeActive() || data.getTechniques().isTechniqueCharging()) {
							data.getTechniques().setChargeHolding(msg.holding);
						}
					}
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			});
		});

		ctx.get().setPacketHandled(true);
	}
}
