package com.dragonminez.common.init.item;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (!pLevel.isClientSide) {

            KiWaveEntity wave = new KiWaveEntity(pLevel, pPlayer);

            wave.setKiDamage(25.0F);

            wave.setKiSpeed(0.5F);

            wave.setSize(2);

            wave.setColors(0x87F9FF, 0x00F3FF);

            pLevel.addFreshEntity(wave);
        }

        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.5F);

        pPlayer.getCooldowns().addCooldown(this, 20);

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.translatable("item.dragonminez.saiyan_ship.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
