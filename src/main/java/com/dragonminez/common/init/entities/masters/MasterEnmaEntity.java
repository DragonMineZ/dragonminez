package com.dragonminez.common.init.entities.masters;

import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class MasterEnmaEntity extends MastersEntity {

    public MasterEnmaEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
		this.lookControl = new LookControl(this) {
			@Override
			public void tick() {}
		};
    }

	@Override
	public void tick() {
		super.tick();
		if (this.getYRot() != 180.0F) this.setYRot(180.0F);
		if (this.getYHeadRot() != 180.0F) this.setYHeadRot(180.0F);
		if (this.yBodyRot != 180.0F) this.yBodyRot = 180.0F;
		if (this.yHeadRot != 180.0F) this.yHeadRot = 180.0F;
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
