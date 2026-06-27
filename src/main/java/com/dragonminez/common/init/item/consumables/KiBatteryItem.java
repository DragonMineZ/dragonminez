package com.dragonminez.common.init.item.consumables;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Bulma's "Ki Accumulator" (Pillar IV novel wave). A consumable that instantly restores a chunk of the
 * player's current ki/energy pool. Granted/taught by the {@code bulma_ki_battery} sidequest.
 */
public class KiBatteryItem extends Item {

	private static final float RESTORE_FRACTION = 0.5f; // 50% of max energy per battery
	private static final int COOLDOWN_TICKS = 60;

	public KiBatteryItem() {
		super(new Properties().stacksTo(16));
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f);

		if (!level.isClientSide) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) return;
				data.getResources().addEnergy(data.getMaxEnergy() * RESTORE_FRACTION);
				if (player instanceof ServerPlayer serverPlayer) {
					NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(serverPlayer), serverPlayer);
				}
				if (!player.getAbilities().instabuild) stack.shrink(1);
				player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
			});
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}
}
