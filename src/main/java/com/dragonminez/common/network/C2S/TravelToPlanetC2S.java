package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.spacepod.SpacePodDestinationDefinition;
import com.dragonminez.common.spacepod.SpacePodDestinationRegistry;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TravelToPlanetC2S {
	private final String destinationId;

	public TravelToPlanetC2S(String destinationId) {
		this.destinationId = destinationId;
	}

	public static void encode(TravelToPlanetC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.destinationId);
	}

	public static TravelToPlanetC2S decode(FriendlyByteBuf buf) {
		return new TravelToPlanetC2S(buf.readUtf(32767));
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			if (player == null) return;

			// Dead spirits are forced back to the Otherworld each tick by TickHandler's
			// handleOtherworldTransfer. Allowing travel would spawn the pod at the destination
			// while the spirit gets yanked back, leaving the player pod-less in the Otherworld.
			boolean dead = StatsProvider.get(StatsCapability.INSTANCE, player)
					.map(data -> !data.getStatus().isAlive())
					.orElse(false);
			if (dead) return;

			ServerLevel currentLevel = player.serverLevel();
			SpacePodDestinationDefinition destination = SpacePodDestinationRegistry.getServerDestination(destinationId);
			if (destination == null || !destination.unlockRules().test(player)) {
				return;
			}

			ResourceLocation targetId = ResourceLocation.tryParse(destination.dimension());
			if (targetId == null) {
				return;
			}

			ResourceKey<Level> targetKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, targetId);
			ServerLevel targetLevel = player.server.getLevel(targetKey);
			if (targetLevel == null || targetLevel == currentLevel) {
				return;
			}

			Entity vehicle = player.getVehicle();
			player.stopRiding();
			if (vehicle instanceof SpacePodEntity ownPod) {
				ownPod.discard();
			}

			Vec3 targetPos = destination.resolvePosition(player.position());
			player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, player.getYRot(), player.getXRot());

			SpacePodEntity newPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), targetLevel);
			newPod.setPos(targetPos.x, targetPos.y, targetPos.z);

			if (targetLevel.addFreshEntity(newPod)) {
				player.startRiding(newPod);
			}
		});
		context.get().setPacketHandled(true);
	}
}
