package com.dragonminez.common.init.entities.masters;

import com.dragonminez.client.gui.MasterTextScreen;
import com.dragonminez.client.gui.MastersSkillsScreen;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MasterKarinEntity extends MastersEntity {

    public MasterKarinEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
		this.masterName = "karin";
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
}
