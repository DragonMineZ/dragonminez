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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	@Overwrite
	public boolean hurt(int pAmount, RandomSource pRandom, @Nullable ServerPlayer pUser) {
		ItemStack stack = (ItemStack) (Object) this;

		if (!stack.isDamageableItem()) {
			return false;
		} else {
			if (pAmount > 0) {
				int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);
				int j = 0;

				for(int k = 0; i > 0 && k < pAmount; ++k) {
					if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(stack, i, pRandom)) {
						++j;
					}
				}

				pAmount -= j;
				if (pAmount <= 0) {
					return false;
				}
			}

			if (pUser != null && pAmount != 0) {
				CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(pUser, stack, stack.getDamageValue() + pAmount);
			}

			int l = stack.getDamageValue() + pAmount;
			stack.setDamageValue(l);
			return l >= stack.getMaxDamage();
		}
	}

	@Inject(method = "hurtAndBreak", at = @At("HEAD"), cancellable = true)
	public <T extends LivingEntity> void onHurtAndBreak(int pAmount, T pEntity, Consumer<T> pOnBroken, CallbackInfo ci) {
		ItemStack stack = (ItemStack) (Object) this;

		if (!stack.isDamageableItem()) {
			ci.cancel();
			return;
		}

		if (stack.getItem() instanceof ArmorItem) {
			int unbreakingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);

			if (unbreakingLevel > 0) {
				if (pEntity.getRandom().nextInt(unbreakingLevel + 1) < unbreakingLevel) {
					ci.cancel();
					return;
				}
			}

			stack.setDamageValue(stack.getDamageValue() + 1);

			if (stack.getDamageValue() >= stack.getMaxDamage()) {
				pOnBroken.accept(pEntity);
				stack.shrink(1);
				if (pEntity instanceof Player player) {
					player.awardStat(Stats.ITEM_BROKEN.get(stack.getItem()));
				}
				stack.setDamageValue(0);
			}

			ci.cancel();
		}
	}
}