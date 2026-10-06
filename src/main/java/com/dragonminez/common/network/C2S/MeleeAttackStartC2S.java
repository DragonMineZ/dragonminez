package com.dragonminez.common.network.C2S;

import com.dragonminez.common.combat.logic.player.MeleeWindupTracker;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.util.SoundHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.MeleeAnimationS2C;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MeleeAttackStartC2S {
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
		long arrivalNanos = System.nanoTime();
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null || this.selectedSlot != player.getInventory().selected) return;
			if (!MeleeWindupTracker.canStrike(player) || PlayerAttackHelper.isChargingTechnique(player)) return;

			var hand = PlayerAttackHelper.getCurrentAttack(player, this.comboCount);
			if (hand == null || hand.attack() == null) return;
			if (!MeleeWindupTracker.registerStart(player, this.comboCount, this.selectedSlot, arrivalNanos)) return;

			if (player.level() instanceof ServerLevel serverLevel) {
				SoundHelper.playSound(serverLevel, player, player, hand.attack().swingSound());
			}
			float animSpeed = PlayerAttackHelper.getMeleeAnimationSpeed(PlayerAttackHelper.getAttackCooldownTicksCapped(player));
			NetworkHandler.sendToTrackingEntity(new MeleeAnimationS2C(player.getId(), hand.attack().animation(), hand.isOffHand(), animSpeed), player);
		});
		ctx.get().setPacketHandled(true);
	}
}
