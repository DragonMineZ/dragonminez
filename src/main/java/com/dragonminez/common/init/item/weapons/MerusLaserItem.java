package com.dragonminez.common.init.item.weapons;

import com.dragonminez.common.init.entities.ki.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MerusLaserItem extends Item {
	public MerusLaserItem( ) {
		super(new Properties().stacksTo(1));
	}
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (!pLevel.isClientSide) {

            KiBlastEntity kiBlast = new KiBlastEntity(pLevel, pPlayer);

            kiBlast.setupKiBlast(pPlayer, 10.0f,  2.5f, 0x00FFFF, 0x78FFFF, 1.0f, 5);

            //kiBlast.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 1.5f, 0.5F);

        }


        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);

        pPlayer.getCooldowns().addCooldown(this, 30);

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

}
