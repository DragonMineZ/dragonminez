package com.dragonminez.common.network.C2S;

import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.util.SoundHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.MeleeAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MeleeAttackStartC2S {
	private static final String START_TIME_TAG = "dmz_melee_attack_start_time";
	private static final String START_COMBO_TAG = "dmz_melee_attack_start_combo";
	private static final String START_SLOT_TAG = "dmz_melee_attack_start_slot";
	private static final String LAST_ATTACK_TIME_TAG = "dmz_last_melee_attack_time";
	private static final int MAX_PENDING_TICKS = 40;
	private static final int ATTACK_RATE_TOLERANCE_TICKS = 2;

	private final int comboCount;
	private final int selectedSlot;

	public MeleeAttackStartC2S(int comboCount, int selectedSlot) {
		this.comboCount = comboCount;
		this.selectedSlot = selectedSlot;
	}

	public MeleeAttackStartC2S(FriendlyByteBuf buffer) {
		this.comboCount = buffer.readInt();
		this.selectedSlot = buffer.readInt();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(this.comboCount);
		buffer.writeInt(this.selectedSlot);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null || this.selectedSlot != player.getInventory().selected) return;
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				if (stats.getStatus().isStunned() || stats.getStatus().isBlocking() || player.isBlocking()
						|| PlayerAttackHelper.isChargingTechnique(player)) return;

				long gameTime = player.level().getGameTime();
				var data = player.getPersistentData();
				long pendingStart = data.getLong(START_TIME_TAG);
				if (pendingStart > 0 && gameTime - pendingStart <= MAX_PENDING_TICKS) return;

				long lastAttack = data.getLong(LAST_ATTACK_TIME_TAG);
				int minInterval = Math.max(0, (int) Math.floor(player.getCurrentItemAttackStrengthDelay()) - ATTACK_RATE_TOLERANCE_TICKS);
				if (lastAttack > 0 && gameTime - lastAttack < minInterval) return;

				var hand = PlayerAttackHelper.getCurrentAttack(player, this.comboCount);
				if (hand == null || hand.attack() == null) return;

				data.putLong(START_TIME_TAG, gameTime);
				data.putInt(START_COMBO_TAG, this.comboCount);
				data.putInt(START_SLOT_TAG, this.selectedSlot);

				if (player.level() instanceof ServerLevel serverLevel) {
					SoundHelper.playSound(serverLevel, player, player, hand.attack().swingSound());
				}
				float animSpeed = PlayerAttackHelper.getMeleeAnimationSpeed(PlayerAttackHelper.getAttackCooldownTicksCapped(player));
				NetworkHandler.sendToTrackingEntity(new MeleeAnimationS2C(player.getId(), hand.attack().animation(), hand.isOffHand(), animSpeed), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}

	public static boolean consumeIfReady(ServerPlayer player, int comboCount, int selectedSlot) {
		var data = player.getPersistentData();
		long startTime = data.getLong(START_TIME_TAG);
		long elapsed = player.level().getGameTime() - startTime;
		boolean matches = startTime > 0
				&& data.getInt(START_COMBO_TAG) == comboCount
				&& data.getInt(START_SLOT_TAG) == selectedSlot;
		if (!matches || elapsed > MAX_PENDING_TICKS) {
			clearPending(data);
			return false;
		}

		var hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);
		if (hand == null || elapsed < Math.max(1, PlayerAttackHelper.getMeleeAttackWindupTicks(
				PlayerAttackHelper.getAttackCooldownTicksCapped(player), hand) - 1)) return false;

		clearPending(data);
		return true;
	}

	public static void cancelPending(ServerPlayer player) {
		clearPending(player.getPersistentData());
	}

	private static void clearPending(net.minecraft.nbt.CompoundTag data) {
		data.remove(START_TIME_TAG);
		data.remove(START_COMBO_TAG);
		data.remove(START_SLOT_TAG);
	}
}
