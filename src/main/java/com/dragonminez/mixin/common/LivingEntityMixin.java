package com.dragonminez.mixin.common;

import com.dragonminez.common.combat.logic.knockback.ConfigurableKnockback;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.MobBattlePowerHelper;
import com.dragonminez.common.util.IHealthFixable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IBattlePower, IHealthFixable, ConfigurableKnockback {
	@Unique
	private float customKnockbackMultiplier = 1.0F;
	@Shadow public abstract float getMaxHealth();
	@Shadow public abstract AttributeMap getAttributes();
	@Shadow public abstract float getHealth();
	@Shadow public abstract void setHealth(float newHealth);

	@Unique
	public int battlePower = 0;

	@Unique
	private Float dragonminez$actualHealth = null;

	@Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"))
	private void dragonminez$readAdditionalSaveData(CompoundTag tag, CallbackInfo callback) {
		if (tag.contains("Health", Tag.TAG_ANY_NUMERIC)) {
			float savedHealth = tag.getFloat("Health");
			if (savedHealth > this.getMaxHealth() && savedHealth > 0.0F && Float.isFinite(savedHealth)) {
				this.dragonminez$actualHealth = savedHealth;
			}
		}
	}

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void dragonminez$restoreStoredHealth(CallbackInfo callback) {
		dragonminez$applyDeferredHealthRestore();
	}

	@Override
	public void dragonminez$setHealthRestorePoint(float restorePoint) {
		this.dragonminez$actualHealth = restorePoint;
	}

	@Override
	public void dragonminez$applyDeferredHealthRestore() {
		if (this.dragonminez$actualHealth == null) return;
		float target = this.dragonminez$actualHealth;
		this.dragonminez$actualHealth = null;
		if (!(target > 0.0F) || !Float.isFinite(target)) return;
		float max = this.getMaxHealth();
		if (!(max > 0.0F) || !Float.isFinite(max)) {
			this.dragonminez$actualHealth = target;
			return;
		}
		float applied = Math.min(target, max);
		if (applied > this.getHealth()) {
			this.setHealth(applied);
		}
	}

	@Override
	public int getBattlePower() {
		if (this.battlePower == 0) {
			LivingEntity self = (LivingEntity) (Object) this;
			boolean accurate = com.dragonminez.common.config.ConfigManager.getCombatConfig().getAccurateMobBattlePower()
					&& self instanceof net.minecraft.world.entity.Mob
					&& !MobBattlePowerHelper.isDmzManaged(self);
			if (accurate) {
				this.battlePower = MobBattlePowerHelper.calculate(self);
			} else {
				double attackDamage = 0;
				if (this.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) attackDamage = this.getAttributes().getValue(Attributes.ATTACK_DAMAGE);
				this.battlePower = (int) (this.getMaxHealth() + attackDamage * 5);
			}
		}
		return Math.max(5, this.battlePower);
	}

	@Override
	public void setBattlePower(int power) {
		this.battlePower = power;
	}

	@Inject(method = "getAttributeValue*", at = @At("HEAD"), cancellable = true)
	public void dragonminez$getAttributeValue_Inject(Holder<Attribute> attribute, CallbackInfoReturnable<Double> cir) {
		Object object = this;
		if (object instanceof Player player) {
			var comboCount = ((PlayerAttackProperties) player).getComboCount();
			if (player.level().isClientSide
					&& comboCount > 0
					&& PlayerAttackHelper.shouldAttackWithOffHand(player, comboCount)) {

				PlayerAttackHelper.offhandAttributes(player, () -> {
					var value = player.getAttributes().getValue(attribute);
					cir.setReturnValue(value);
				});
				cir.cancel();
			}
		}
	}

	@Override
	public void setKnockbackMultiplier(float value) {
		this.customKnockbackMultiplier = value;
	}

	@ModifyVariable(method = "knockback", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	public double dragonminez$takeKnockback_HEAD_changeStrength(double knockbackStrength) {
		return knockbackStrength * customKnockbackMultiplier;
	}
}