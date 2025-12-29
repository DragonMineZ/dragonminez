package com.dragonminez.common.init.item;

import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiVolleyEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MerusLaserItem extends Item {
	public MerusLaserItem( ) {
		super(new Properties().stacksTo(1));
	}
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        if (!pLevel.isClientSide) {

            KiBlastEntity kiBlast = new KiBlastEntity(pLevel, pPlayer);
            kiBlast.setup(pPlayer, 7.2F, 0.5F, 0xEBFDFF, 0x51EBFC);

            kiBlast.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 0.85F, 0.8F);

            pLevel.addFreshEntity(kiBlast);
        }


        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);

        pPlayer.getCooldowns().addCooldown(this, 30);

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

	@Override
	public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
		pTooltipComponents.add(Component.translatable("item.dragonminez.saiyan_ship.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
