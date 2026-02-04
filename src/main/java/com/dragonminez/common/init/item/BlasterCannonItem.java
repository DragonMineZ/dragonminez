package com.dragonminez.common.init.item;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlasterCannonItem extends Item {
	public BlasterCannonItem( ) {
		super(new Properties().stacksTo(1));
	}

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (!pLevel.isClientSide) {

            KiBlastEntity kiBlast = new KiBlastEntity(pLevel, pPlayer);
            kiBlast.setup(pPlayer, 10.0F, 1.0F, 0.0f,0xFF5E7C, 0xB5102E);

            kiBlast.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 0.05F, 0.5F);

            pLevel.addFreshEntity(kiBlast);
        }


        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);

        pPlayer.getCooldowns().addCooldown(this, 60);

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }
}
