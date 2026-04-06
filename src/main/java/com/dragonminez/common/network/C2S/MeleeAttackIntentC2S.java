package com.dragonminez.common.network.C2S;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.server.events.players.CombatEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MeleeAttackIntentC2S {
	public static final int MIN_EFFECTIVE_CHARGE_TICKS = 5; // 0.25s
	public static final int MAX_EFFECTIVE_CHARGE_TICKS = 25; // 1.25s
	public static final int MAX_TOTAL_HOLD_TICKS = 65; // 2.0s

	public enum IntentType {
		LIGHT,
		HEAVY
	}

	private final IntentType intentType;
	private final int chargeTicks;

	public MeleeAttackIntentC2S(IntentType intentType) {
		this(intentType, 0);
	}

	public MeleeAttackIntentC2S(IntentType intentType, int chargeTicks) {
		this.intentType = intentType;
		this.chargeTicks = clampChargeTicks(chargeTicks);
	}

	public MeleeAttackIntentC2S(FriendlyByteBuf buffer) {
		int ordinal = buffer.readVarInt();
		IntentType[] values = IntentType.values();
		this.intentType = ordinal >= 0 && ordinal < values.length ? values[ordinal] : IntentType.LIGHT;
		this.chargeTicks = clampChargeTicks(buffer.readVarInt());
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(intentType.ordinal());
		buffer.writeVarInt(chargeTicks);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || player.hasEffect(MainEffects.STUN.get())) return;

			DMZEvent.PlayerAttackStartEvent.AttackType attackType = switch (intentType) {
				case HEAVY -> DMZEvent.PlayerAttackStartEvent.AttackType.HEAVY;
				default -> DMZEvent.PlayerAttackStartEvent.AttackType.LIGHT;
			};
			CombatEvent.registerAttackIntent(player, attackType, chargeTicks);
		});
		context.setPacketHandled(true);
	}

	private static int clampChargeTicks(int chargeTicks) {
		return Math.max(0, Math.min(chargeTicks, MAX_TOTAL_HOLD_TICKS));
	}
}

