package com.dragonminez.common.init.item;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
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

public class SaiyanShipItem extends Item {
	public SaiyanShipItem( ) {
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
			SpacePodEntity nave = new SpacePodEntity(MainEntities.SPACE_POD.get(), level);
			nave.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

			level.addFreshEntity(nave);

			pContext.getItemInHand().shrink(1);

			return InteractionResult.sidedSuccess(level.isClientSide);
		}


		return super.useOn(pContext);
	}

//    @Override
//    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
//        ItemStack itemstack = pPlayer.getItemInHand(pHand);
//
//        if (!pLevel.isClientSide) {
//
//            KiBlastEntity kiBlast = new KiBlastEntity(pLevel, pPlayer);
//            kiBlast.setup(pPlayer, 10.0F, 1.0F, 0xFF5C5C, 0xC21B1B);
//
//            kiBlast.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 0.9F, 0.5F);
//
//            pLevel.addFreshEntity(kiBlast);
//        }
//
//
//        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
//                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
//
//        pPlayer.getCooldowns().addCooldown(this, 10);
//
//        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
//    }

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.translatable("item.dragonminez.saiyan_ship.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
