package com.dragonminez.common.init.entities.masters;

import com.dragonminez.client.gui.MasterTextScreen;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MasterGuruEntity extends MastersEntity {

	public MasterGuruEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
		this.setPersistenceRequired();
		this.lookControl = new LookControl(this) {
			@Override
			public void tick() {}
		};
		this.masterName = "guru";
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
		if (this.level().isClientSide && masterName != null) {
			StatsProvider.get(StatsCapability.INSTANCE, pPlayer).ifPresent(data -> {
				if (data.getStatus().hasCreatedCharacter()) {
					Minecraft mc = Minecraft.getInstance();
					mc.setScreen(new MasterTextScreen(masterName));
					mc.player.playSound(MainSounds.UI_MENU_SWITCH.get());
				} else {
					pPlayer.displayClientMessage(Component.translatable("gui.dragonminez.lines.generic.createcharacter"), true);
				}
			});
			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(pPlayer, pHand);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getYRot() != 180.0F) this.setYRot(180.0F);
		if (this.getYHeadRot() != 180.0F) this.setYHeadRot(180.0F);
		if (this.yBodyRot != 180.0F) this.yBodyRot = 180.0F;
		if (this.yHeadRot != 180.0F) this.yHeadRot = 180.0F;
	}
}