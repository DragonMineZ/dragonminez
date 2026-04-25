package com.dragonminez.common.init.item;

import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonRadarDefinition;
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
public class DragonRadarItem extends Item {
	public static final String NBT_RANGE = "RadarRange";
	private static final int COOLDOWN_TICKS = 20 * 16;

	private final String radarDefinitionId;

	public DragonRadarItem(String radarDefinitionId) {
		super(new Properties().stacksTo(1));
		this.radarDefinitionId = radarDefinitionId;
	}

	public String getRadarDefinitionId() {
		return radarDefinitionId;
	}

	public DragonRadarDefinition getDefinition() {
		return DragonBallDefinitions.getRadar(radarDefinitionId);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (hand == InteractionHand.OFF_HAND && !player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			return InteractionResultHolder.fail(stack);
		}

		if (player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(stack);
		}

		player.playSound(MainSounds.DRAGONRADAR.get());

		if (!world.isClientSide()) {
			DragonRadarDefinition definition = getDefinition();
			int currentRange = stack.getOrCreateTag().getInt(NBT_RANGE);
			int newRange = definition == null ? currentRange : getNextRange(definition.getRanges(), currentRange);
			stack.getOrCreateTag().putInt(NBT_RANGE, newRange);
			player.displayClientMessage(Component.translatable("gui.dmzradar.range", newRange), true);
		}

		player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}

	private int getNextRange(int[] ranges, int currentRange) {
		if (ranges.length == 0) return currentRange;
		for (int i = 0; i < ranges.length; i++) {
			if (ranges[i] == currentRange) {
				return ranges[(i + 1) % ranges.length];
			}
		}
		return ranges[0];
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag isAdvanced) {
		DragonRadarDefinition definition = getDefinition();
		if (definition != null) {
			tooltip.add(Component.translatable(definition.getTooltipKey()).withStyle(ChatFormatting.GRAY));
		}
	}
}
