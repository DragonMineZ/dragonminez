package com.yuseix.dragonminez.common.init.items.custom;

import com.yuseix.dragonminez.common.config.old.DMZGeneralConfig;
import com.yuseix.dragonminez.common.init.MainSounds;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import com.yuseix.dragonminez.common.util.DMZDatos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SenzuBeanItem extends Item {
	public SenzuBeanItem() {
		super(new Properties()
				.stacksTo(8)
		);
	}

	@Override
	public @NotNull Component getName(@NotNull ItemStack pStack) {
		return Component.translatable("item.dragonminez.senzu_bean");
	}

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.translatable("item.dragonminez.senzu_bean.tooltip").withStyle(ChatFormatting.GRAY));
	}

	// Permitir consumir incluso con la barra de hambre llena
	@Override
	public boolean isEdible() {
		return true;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack pStack) {
		return UseAnim.BLOCK;
	}

	@Override
	public int getUseDuration(ItemStack pStack) {
		return 20 * 15; // 15s y se reinicia la anim
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, @NotNull InteractionHand pUsedHand) {
		if (pPlayer.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
		}
		pPlayer.startUsingItem(pUsedHand);
		return InteractionResultHolder.consume(pPlayer.getItemInHand(pUsedHand));
	}

	@Override
	public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity, int timeLeft) {
		if (!(pLivingEntity instanceof Player player) || pLevel.isClientSide) return;

		DMZDatos dmzdatos = new DMZDatos();

		pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.SENZU_BEAN.get(), SoundSource.NEUTRAL, 1.5F, 1.0F);

		DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(stats -> {
			double VidaTotal = dmzdatos.calcConstitution(stats);
			int energiaMax = dmzdatos.calcEnergy(stats);
			int staminaMax = dmzdatos.calcStamina(stats);

			player.heal((float) VidaTotal);
			stats.setIntValue("curstam", staminaMax);
			stats.setIntValue("curenergy", energiaMax);
		});

		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(15.0F);

		var segundos = DMZGeneralConfig.SENZU_COOLDOWN.get();
		var tiempo = segundos * 20;

		player.getCooldowns().addCooldown(this, tiempo);

		if (pLivingEntity instanceof ServerPlayer player1 && player1.isCreative()) {
			pStack.shrink(0);
		} else pStack.shrink(1);
	}
}
