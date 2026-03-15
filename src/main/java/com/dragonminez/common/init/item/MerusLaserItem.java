package com.dragonminez.common.init.item;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiExplosionEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
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

            //kiBlast.setupKiSmall(pPlayer,7.2f, 2.2f, 0x00FFFF);
            //kiBlast.setupKiBlast(pPlayer,10,2.2f, 0x00FFFF, 2.5f);
            //kiBlast.setupKiLargeBlast(pPlayer,10,2.2f, 0x00FFFF, 4.0f);
            //kiBlast.setupInvertedKiBlast(pPlayer,10,2.5f, 0x362440, 0xFFFFFF, 1.5f);
            //kiBlast.setupKiSouls(pPlayer,10,0.8f, 0xFFFFFF);
            //kiBlast.setupKiGenki(pPlayer,10,0.8f);
            //kiBlast.setupKiNova(pPlayer,10,0.6f);
            //kiBlast.setupKiDeathBall(pPlayer,10,2.0f, 0xA927F5);

            //kiWave.setupKiHame(pPlayer, 10.0f, 2.0f, 5.0f);
            //kiWave.setupKiGalickGun(pPlayer, 10.0f, 1.0f, 3.0f);
            //kiWave.setupKiWave(pPlayer, 10.0f, 2.0f, 0x43E620 , 2.0f);

            //kilaser.setupKiLaser(pPlayer, 10.0f, 0.5f, 0xFF5C5C, 0xBF2828);
//            kilaser.setupKiMakkankosanpo(pPlayer, 10.0f, 0.5f);
            kiexp.setupKiExplosion(pPlayer, 10.0f, 0xFFEB63, 0xFFE633);
//            pLevel.addFreshEntity(kiWave);
        }


        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);

        pPlayer.getCooldowns().addCooldown(this, 30);

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

}
