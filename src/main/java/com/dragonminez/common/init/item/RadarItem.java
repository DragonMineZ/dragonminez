package com.dragonminez.common.init.item;

import com.dragonminez.common.init.MainSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class RadarItem extends Item {

	private static final int[] RANGES = {150, 300};
	public static final String NBT_RANGE = "RadarRange";
	private static final int COOLDOWN_TICKS = 20 * 16;

	public RadarItem() {
		super(new Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(stack);
		}

		player.playSound(MainSounds.DRAGONRADAR.get());

		if (!world.isClientSide()) {
			int currentRange = stack.getOrCreateTag().getInt(NBT_RANGE);
			int newRange = RANGES[(indexOf(currentRange) + 1) % RANGES.length];

			stack.getOrCreateTag().putInt(NBT_RANGE, newRange);

			Component message = Component.translatable("ui.dmzradar.range").append(": ")
					.append(String.valueOf(newRange)).append(" ")
					.append(Component.translatable("ui.dmzradar.blocks"));

			player.displayClientMessage(message, true);
		}

		player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}

	private int indexOf(int range) {
		for (int i = 0; i < RANGES.length; i++) {
			if (RANGES[i] == range) return i;
		}
		return 0;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.translatable("item.dragonminez.dball_radar.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
