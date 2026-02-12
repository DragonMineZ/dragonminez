package com.dragonminez.common.init.item;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.BlackNimbusEntity;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlackNimbusItem extends Item {
	public BlackNimbusItem( ) {
		super(new Properties().stacksTo(1));
	}

	@Override
	public InteractionResult useOn(UseOnContext pContext) {
		Player player = pContext.getPlayer();
		Level level = pContext.getLevel();
		BlockPos pos = pContext.getClickedPos();
		Direction direction = pContext.getClickedFace();

		BlockPos spawnPos = pos.above();

		if (player != null && level != null) {
			BlackNimbusEntity nube = new BlackNimbusEntity(MainEntities.BLACK_NIMBUS.get(), level);
			nube.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

			level.addFreshEntity(nube);

			pContext.getItemInHand().shrink(1);

			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		return super.useOn(pContext);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.translatable("item.dragonminez.black_nimbus.tooltip").withStyle(ChatFormatting.GRAY));
	}
}