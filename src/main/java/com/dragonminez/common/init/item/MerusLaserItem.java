package com.dragonminez.common.init.item;

import com.dragonminez.common.init.entities.ki.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
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
            KiWaveEntity kiWave = new KiWaveEntity(pLevel, pPlayer);
            KiLaserEntity kilaser = new KiLaserEntity(pLevel, pPlayer);
            KiExplosionEntity kiexp = new KiExplosionEntity(pLevel, pPlayer);
            KiDiskEntity kidisc = new KiDiskEntity(pLevel, pPlayer);
            KiBarrierEntity kibarrier = new KiBarrierEntity(pLevel, pPlayer);


            //kiBlast.setupKiSmall(pPlayer,7.2f, 0.2f, 0x00FFFF);
            //kiBlast.setupKiBlast(pPlayer,10,2.2f, 0x00FFFF, 2.5f, 100);
            //kiBlast.setupKiLargeBlast(pPlayer,10,2.2f, 0x00FFFF, 4.0f, 100);
            //kiBlast.setupInvertedKiBlast(pPlayer,10,2.5f, 0x362440, 0xFFFFFF, 1.5f, 100);
            //kiBlast.setupKiSouls(pPlayer,10,1.2f, 0xFFFFFF, 100);
            //kiBlast.setupKiGenki(pPlayer,10,0.8f, 100);
            //kiBlast.setupKiNova(pPlayer,10,0.6f, 100);
            //kiBlast.setupKiDeathBall(pPlayer,10,2.0f, 0xA927F5, 100);
            //kiBlast.setupSokidan(pPlayer,10,0.5f, 0xFFEF26, 1.0f, 100);

            //kiWave.setupKiHame(pPlayer, 10.0f, 2.0f, 1.0f, 100);
            //kiWave.setupKiGalickGun(pPlayer, 10.0f, 1.0f, 3.0f, 100);
            //kiWave.setupFinalFlash(pPlayer, 10.0f, 1.0f, 2.5f, 100);
            //kiWave.setupKiWave(pPlayer, 10.0f, 2.0f, 0x43E620 , 2.0f, 100);
            //kidisc.setupKiDisk(pPlayer, 10.0f, 2.0f, 0xF5E027, 2.5f, 100);
            //kilaser.setupKiLaser(pPlayer, 10.0f, 0.5f, 0xFF5C5C, 100);
            //kilaser.setupKiMakkankosanpo(pPlayer, 10.0f, 0.5f, 100);
            //kiexp.setupKiExplosion(pPlayer, 10.0f, 0xFFED00, 0xFFED00, 100);
            //kibarrier.setupKiBarrier(pPlayer, 0x43FF00, 0xF2FF00, 100);
        }


        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);

        pPlayer.getCooldowns().addCooldown(this, 30);

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

}
