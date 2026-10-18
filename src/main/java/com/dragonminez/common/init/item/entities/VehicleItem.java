package com.dragonminez.common.init.item.entities;

import com.dragonminez.common.init.entities.vehicle.VehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class VehicleItem extends Item {

	private final Function<Level, ? extends VehicleEntity> factory;

	public VehicleItem(Function<Level, ? extends VehicleEntity> factory) {
		super(new Properties().stacksTo(1));
		this.factory = factory;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		if (level.isClientSide) return InteractionResult.SUCCESS;

		Direction face = context.getClickedFace();
		BlockPos pos = face == Direction.UP ? context.getClickedPos().above() : context.getClickedPos().relative(face);
		VehicleEntity vehicle = factory.apply(level);
		float yaw = player != null ? player.getYRot() : 0.0F;
		vehicle.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
		vehicle.setYBodyRot(yaw);
		vehicle.setYHeadRot(yaw);
		if (!level.noCollision(vehicle, vehicle.getBoundingBox())) {
			if (player != null) player.displayClientMessage(Component.translatable("message.dragonminez.vehicle.no_space"), true);
			return InteractionResult.FAIL;
		}

		level.addFreshEntity(vehicle);
		level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
		if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
		tooltip.add(Component.translatable(this.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
	}
}
