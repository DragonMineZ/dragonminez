package com.dragonminez.mixin.common;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {

	@Shadow public boolean hurt(int pAmount, RandomSource pRandom, @Nullable ServerPlayer pUser) {
		if (!((ItemStack) (Object) this).isDamageableItem()) {
			return false;
		} else {
			if (pAmount > 0) {
				int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, ((ItemStack) (Object) this));
				int j = 0;

				for(int k = 0; i > 0 && k < pAmount; ++k) {
					if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(((ItemStack) (Object) this), i, pRandom)) {
						++j;
					}
				}

				pAmount -= j;
				if (pAmount <= 0) {
					return false;
				}
			}

			if (pUser != null && pAmount != 0) {
				CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(pUser, ((ItemStack) (Object) this), ((ItemStack) (Object) this).getDamageValue() + pAmount);
			}

			int l = ((ItemStack) (Object) this).getDamageValue() + pAmount;
			((ItemStack) (Object) this).setDamageValue(l);
			return l >= ((ItemStack) (Object) this).getMaxDamage();
		}
	}


	@Inject(method = "hurtAndBreak", at = @At("HEAD"), cancellable = true)
	public <T extends LivingEntity> void onHurtAndBreak(int pAmount, T pEntity, Consumer<T> pOnBroken, CallbackInfo ci) {
		if (((ItemStack) (Object) this).getItem() instanceof ArmorItem) {
			int unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, (ItemStack) (Object) this);

			if (unbreakingLevel > 0) {
				Random rand = new Random();
				if (rand.nextInt(unbreakingLevel + 1) < unbreakingLevel) {
					ci.cancel();
					return;
				}
			}

			((ItemStack) (Object) this).setDamageValue(((ItemStack) (Object) this).getDamageValue() + 1);

			if (((ItemStack) (Object) this).getMaxDamage() < ((ItemStack) (Object) this).getDamageValue()) {
				pOnBroken.accept(pEntity);
				((ItemStack) (Object) this).shrink(1);
				((Player)pEntity).awardStat(Stats.ITEM_BROKEN.get(((ItemStack) (Object) this).getItem()));
				((ItemStack) (Object) this).setDamageValue(0);
			}

			ci.cancel();
		}
	}
}
