package com.dragonminez.common.init.entities;

import com.dragonminez.client.gui.WishesScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.dragonminez.server.events.DragonBallsHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

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
