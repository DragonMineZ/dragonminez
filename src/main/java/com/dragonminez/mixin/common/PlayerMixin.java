package com.dragonminez.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	public void onAttack(Entity pTarget, CallbackInfo ci) {
		if (pTarget.isAttackable() && !pTarget.skipAttackInteraction((Player) (Object) this)) {
			float f = (float) ((Player) (Object) this).getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			float f1;
			if (pTarget instanceof LivingEntity) {
				f1 = net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(((Player) (Object) this).getMainHandItem(), ((LivingEntity) pTarget).getMobType());
			} else {
				f1 = net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(((Player) (Object) this).getMainHandItem(), net.minecraft.world.entity.MobType.UNDEFINED);
			}

			float f2 = ((Player) (Object) this).getAttackStrengthScale(0.5F);
			f *= 0.2F + f2 * f2 * 0.8F;
			f1 *= f2;
			if (f > 0.0F || f1 > 0.0F) {
				boolean flag = f2 > 0.9F;
				boolean flag1 = false;
				float i = (float) ((Player) (Object) this).getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_KNOCKBACK);
				i += net.minecraft.world.item.enchantment.EnchantmentHelper.getKnockbackBonus((Player) (Object) this);
				if (((Player) (Object) this).isSprinting() && flag) {
					((Player) (Object) this).level().playSound(null, ((Player) (Object) this).getX(), ((Player) (Object) this).getY(), ((Player) (Object) this).getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_KNOCKBACK, ((Player) (Object) this).getSoundSource(), 1.0F, 1.0F);
					++i;
					flag1 = true;
				}

				boolean flag2 = flag && ((Player) (Object) this).fallDistance > 0.0F && !((Player) (Object) this).onGround() && !((Player) (Object) this).onClimbable() && !((Player) (Object) this).isInWater() && !((Player) (Object) this).hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && !((Player) (Object) this).isPassenger() && pTarget instanceof LivingEntity;
				flag2 = flag2 && !((Player) (Object) this).isSprinting();
				net.minecraftforge.event.entity.player.CriticalHitEvent hitResult = net.minecraftforge.common.ForgeHooks.getCriticalHit((Player) (Object) this, pTarget, flag2, flag2 ? 1.5F : 1.0F);
				flag2 = hitResult != null;
				if (flag2) {
					f *= hitResult.getDamageModifier();
				}

				f += f1;
				boolean flag3 = false;
				double d0 = (double) (((Player) (Object) this).walkDist - ((Player) (Object) this).walkDistO);
				if (flag && !flag2 && !flag1 && ((Player) (Object) this).onGround() && d0 < (double) ((Player) (Object) this).getSpeed()) {
					net.minecraft.world.item.ItemStack itemstack = ((Player) (Object) this).getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
					flag3 = itemstack.canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP);
				}

				float f4 = 0.0F;
				boolean flag4 = false;
				int j = net.minecraft.world.item.enchantment.EnchantmentHelper.getFireAspect((Player) (Object) this);
				if (pTarget instanceof LivingEntity) {
					f4 = ((LivingEntity) pTarget).getHealth();
					if (j > 0 && !pTarget.isOnFire()) {
						flag4 = true;
						pTarget.setSecondsOnFire(1);
					}
				}

				net.minecraft.world.phys.Vec3 vec3 = pTarget.getDeltaMovement();
				boolean flag5 = pTarget.hurt(((Player) (Object) this).damageSources().playerAttack((Player) (Object) this), f);
				if (flag5) {
					if (i > 0) {
						if (pTarget instanceof LivingEntity) {
							((LivingEntity) pTarget).knockback((double) ((float) i * 0.5F), (double) net.minecraft.util.Mth.sin(((Player) (Object) this).getYRot() * ((float) Math.PI / 180F)), (double) (-net.minecraft.util.Mth.cos(((Player) (Object) this).getYRot() * ((float) Math.PI / 180F))));
						} else {
							pTarget.push((double) (-net.minecraft.util.Mth.sin(((Player) (Object) this).getYRot() * ((float) Math.PI / 180F)) * (float) i * 0.5F), 0.1D, (double) (net.minecraft.util.Mth.cos(((Player) (Object) this).getYRot() * ((float) Math.PI / 180F)) * (float) i * 0.5F));
						}

						((Player) (Object) this).setDeltaMovement(((Player) (Object) this).getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
						((Player) (Object) this).setSprinting(false);
					}

					if (flag3) {
						float f3 = 1.0F + net.minecraft.world.item.enchantment.EnchantmentHelper.getSweepingDamageRatio((Player) (Object) this) * f;

						for (LivingEntity livingentity : ((Player) (Object) this).level().getEntitiesOfClass(LivingEntity.class, ((Player) (Object) this).getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getSweepHitBox((Player) (Object) this, pTarget))) {
							double entityReachSq = net.minecraft.util.Mth.square(((Player) (Object) this).getEntityReach());
							if (livingentity != (Player) (Object) this && livingentity != pTarget && !((Player) (Object) this).isAlliedTo(livingentity) && (!(livingentity instanceof net.minecraft.world.entity.decoration.ArmorStand) || !((net.minecraft.world.entity.decoration.ArmorStand) livingentity).isMarker()) && ((Player) (Object) this).distanceToSqr(livingentity) < entityReachSq) {
								livingentity.knockback((double) 0.4F, (double) net.minecraft.util.Mth.sin(((Player) (Object) this).getYRot() * ((float) Math.PI / 180F)), (double) (-net.minecraft.util.Mth.cos(((Player) (Object) this).getYRot() * ((float) Math.PI / 180F))));
								livingentity.hurt(((Player) (Object) this).damageSources().playerAttack((Player) (Object) this), f3);
							}
						}

						((Player) (Object) this).level().playSound(null, ((Player) (Object) this).getX(), ((Player) (Object) this).getY(), ((Player) (Object) this).getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, ((Player) (Object) this).getSoundSource(), 1.0F, 1.0F);
						((Player) (Object) this).sweepAttack();
					}

					if (pTarget instanceof net.minecraft.server.level.ServerPlayer && pTarget.hurtMarked) {
						((net.minecraft.server.level.ServerPlayer) pTarget).connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(pTarget));
						pTarget.hurtMarked = false;
						pTarget.setDeltaMovement(vec3);
					}

					if (flag2) {
						((Player) (Object) this).level().playSound(null, ((Player) (Object) this).getX(), ((Player) (Object) this).getY(), ((Player) (Object) this).getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, ((Player) (Object) this).getSoundSource(), 1.0F, 1.0F);
						((Player) (Object) this).crit(pTarget);
					}

					if (!flag2 && !flag3) {
						if (flag) {
							((Player) (Object) this).level().playSound(null, ((Player) (Object) this).getX(), ((Player) (Object) this).getY(), ((Player) (Object) this).getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG, ((Player) (Object) this).getSoundSource(), 1.0F, 1.0F);
						} else {
							((Player) (Object) this).level().playSound(null, ((Player) (Object) this).getX(), ((Player) (Object) this).getY(), ((Player) (Object) this).getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_WEAK, ((Player) (Object) this).getSoundSource(), 1.0F, 1.0F);
						}
					}

					if (f1 > 0.0F) {
						((Player) (Object) this).magicCrit(pTarget);
					}

					((Player) (Object) this).setLastHurtMob(pTarget);
					if (pTarget instanceof LivingEntity) {
						net.minecraft.world.item.enchantment.EnchantmentHelper.doPostHurtEffects((LivingEntity) pTarget, (Player) (Object) this);
					}

					net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects((Player) (Object) this, pTarget);
					net.minecraft.world.item.ItemStack itemstack1 = ((Player) (Object) this).getMainHandItem();
					Entity entity = pTarget;
					if (pTarget instanceof net.minecraftforge.entity.PartEntity) {
						entity = ((net.minecraftforge.entity.PartEntity<?>) pTarget).getParent();
					}

					if (!((Player) (Object) this).level().isClientSide && !itemstack1.isEmpty() && entity instanceof LivingEntity) {
						net.minecraft.world.item.ItemStack copy = itemstack1.copy();
						itemstack1.hurtEnemy((LivingEntity) entity, (Player) (Object) this);
						if (itemstack1.isEmpty()) {
							net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem((Player) (Object) this, copy, net.minecraft.world.InteractionHand.MAIN_HAND);
							((Player) (Object) this).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
						}
					}

					if (pTarget instanceof LivingEntity) {
						float f5 = f4 - ((LivingEntity) pTarget).getHealth();
						((Player) (Object) this).awardStat(net.minecraft.stats.Stats.DAMAGE_DEALT, Math.round(f5 * 10.0F));
						if (j > 0) {
							pTarget.setSecondsOnFire(j * 4);
						}
					}

					((Player) (Object) this).causeFoodExhaustion(0.1F);
				} else {
					((Player) (Object) this).level().playSound(null, ((Player) (Object) this).getX(), ((Player) (Object) this).getY(), ((Player) (Object) this).getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE, ((Player) (Object) this).getSoundSource(), 1.0F, 1.0F);
					if (flag4) {
						pTarget.clearFire();
					}
				}
			}
			((Player) (Object) this).resetAttackStrengthTicker();
		}
	}
}
