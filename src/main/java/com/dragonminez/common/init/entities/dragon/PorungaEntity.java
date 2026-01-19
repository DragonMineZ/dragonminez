package com.dragonminez.common.init.entities.dragon;

import com.dragonminez.client.gui.WishesScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.server.events.DragonBallsHandler;
import com.eliotlash.mclib.math.functions.limit.Min;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PorungaEntity extends DragonWishEntity {

	public PorungaEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (this.level().isClientSide && this.getOwnerName().equals(player.getName().getString())) {
			if (!this.hasGrantedWish() && Minecraft.getInstance().player.equals(player)) {
				Minecraft.getInstance().setScreen(new WishesScreen("porunga", 3));
				Minecraft.getInstance().player.playSound(MainSounds.UI_MENU_SWITCH.get());
			}
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide && reason == RemovalReason.DISCARDED) {
			onDespawn();
		}
		super.remove(reason);
	}

	private void onDespawn() {
		if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
			serverLevel.setWeatherParameters(6000, 0, false, false);
			serverLevel.setDayTime(this.getInvokingTime());

			if (ConfigManager.getServerConfig().getWorldGen().isGenerateDragonBalls()) {
				DragonBallsHandler.scatterDragonBalls(serverLevel, true);
				ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayerByName(this.getOwnerName());
				if (owner != null) {
					DragonBallsHandler.syncRadar(owner.serverLevel());
				}
			}
		}
	}
}
