package com.dragonminez.mixin.common;

import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.combat.util.Player_DMZ;
// import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerMixin implements Player_DMZ, PlayerAttackProperties {
	@Unique
	private int comboCount = 0;
	private AttackHand lastAttack = null;

	@Override
	public int getComboCount() {
		return this.comboCount;
	}

	@Override
	public void setComboCount(int comboCount) {
		this.comboCount = comboCount;
	}

	@Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
	private int dragonminez$suppressDamageIndicatorParticles(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double xDist, double yDist, double zDist, double speed) {
		if (particle == ParticleTypes.DAMAGE_INDICATOR) return 0;
		return level.sendParticles(particle, x, y, z, count, xDist, yDist, zDist, speed);
	}

	@Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
	public ItemStack dragonminez$getMainHandItem_Redirect(Player instance) {
		if (this.comboCount < 0) return instance.getMainHandItem();
		AttackHand hand = PlayerAttackHelper.getCurrentAttack(instance, this.comboCount);
		if (hand == null) return instance.getMainHandItem();

		this.lastAttack = hand;
		return hand.itemStack();
	}

	@Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
	public void dragonminez$setItemInHand_Redirect(Player instance, InteractionHand handArg, ItemStack itemStack) {
		if (this.comboCount < 0) instance.setItemInHand(handArg, itemStack);

		AttackHand hand = this.lastAttack;
		if (hand == null) hand = PlayerAttackHelper.getCurrentAttack(instance, this.comboCount);

		if (hand == null) {
			instance.setItemInHand(handArg, itemStack);
			return;
		}

		InteractionHand redirectedHand = hand.isOffHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		instance.setItemInHand(redirectedHand, itemStack);
	}

	@Nullable
	@Override
	public AttackHand getCurrentAttack() {
		if (this.comboCount < 0) return null;
		return PlayerAttackHelper.getCurrentAttack((Player)(Object)this, this.comboCount);
	}
}