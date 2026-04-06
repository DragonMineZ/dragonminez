package com.dragonminez.common.network.C2S;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.server.events.players.CombatEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MeleeManualAttackC2S {
	private final int targetId;
	private final MeleeAttackIntentC2S.IntentType intentType;
	private final int chargeTicks;

	public MeleeManualAttackC2S(int targetId, MeleeAttackIntentC2S.IntentType intentType, int chargeTicks) {
		this.targetId = targetId;
		this.intentType = intentType;
		this.chargeTicks = Math.max(0, Math.min(chargeTicks, MeleeAttackIntentC2S.MAX_TOTAL_HOLD_TICKS));
	}

	public MeleeManualAttackC2S(FriendlyByteBuf buffer) {
		this.targetId = buffer.readVarInt();
		this.intentType = buffer.readEnum(MeleeAttackIntentC2S.IntentType.class);
		this.chargeTicks = Math.max(0, Math.min(buffer.readVarInt(), MeleeAttackIntentC2S.MAX_TOTAL_HOLD_TICKS));
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(targetId);
		buffer.writeEnum(intentType);
		buffer.writeVarInt(chargeTicks);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer attacker = context.getSender();
			if (attacker == null) return;
			if (!CombatEvent.canUseCustomCombat(attacker)) return;
			if (attacker.hasEffect(MainEffects.STUN.get()) || attacker.isBlocking()) return;

			Entity targetEntity = attacker.level().getEntity(targetId);
			if (!(targetEntity instanceof LivingEntity livingTarget)) return;
			if (!livingTarget.isAlive() || livingTarget == attacker) return;
			if (!attacker.hasLineOfSight(livingTarget)) return;

			double reach = attacker.getAttributeValue(ForgeMod.ENTITY_REACH.get());
			double maxReach = Math.max(2.8D, reach + 0.75D);
			if (attacker.distanceToSqr(livingTarget) > maxReach * maxReach) return;

			DMZEvent.PlayerAttackStartEvent.AttackType attackType = intentType == MeleeAttackIntentC2S.IntentType.HEAVY
					? DMZEvent.PlayerAttackStartEvent.AttackType.HEAVY
					: DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT;
			CombatEvent.registerManualAttackIntent(attacker, attackType, chargeTicks);
			attacker.attack(livingTarget);
			attacker.swing(InteractionHand.MAIN_HAND, true);
		});
		context.setPacketHandled(true);
	}
}

