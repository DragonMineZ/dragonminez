package com.dragonminez.common.init.entities.masters;

import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class MasterRoshiEntity extends MastersEntity {

    public MasterRoshiEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setPersistenceRequired();

    }

    @Override
    protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {

        if (!this.level().isClientSide && pHand == InteractionHand.MAIN_HAND) {
            pPlayer.sendSystemMessage(Component.literal("Menu"));
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(pPlayer, pHand);
    }
}
