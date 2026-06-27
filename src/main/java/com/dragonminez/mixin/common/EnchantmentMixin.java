package com.dragonminez.mixin.common;

import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(Enchantment.class)
public class EnchantmentMixin {
	@Inject(method = "getSlotItems", at = @At("RETURN"), cancellable = true)
	private void dragonminez$getEquipmentFix(LivingEntity entity, CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
		if(entity instanceof Player player) {
			var comboCount = ((PlayerAttackProperties) player).getComboCount();
			var currentHand = PlayerAttackHelper.getCurrentAttack(player, comboCount);

			if (currentHand != null && currentHand.isOffHand()) {
				var map = cir.getReturnValue();
				if (map.get(EquipmentSlot.MAINHAND) != null) {
					map.remove(EquipmentSlot.MAINHAND);
				}
				var offHandStack = player.getOffhandItem();
				if (!offHandStack.isEmpty()) {
					map.put(EquipmentSlot.OFFHAND, offHandStack);
				}
				cir.setReturnValue(map);
			}
		}
	}
}