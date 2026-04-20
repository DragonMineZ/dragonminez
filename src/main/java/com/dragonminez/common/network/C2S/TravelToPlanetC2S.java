package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class TravelToPlanetC2S {
	private final String targetDimensionId;

	public TravelToPlanetC2S(String targetDimensionId) {
		this.targetDimensionId = targetDimensionId;
	}

	public TravelToPlanetC2S(ResourceKey<Level> dimensionKey) {
		this.targetDimensionId = dimensionKey.location().toString();
	}

	public static void encode(TravelToPlanetC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.targetDimensionId);
	}

	public static TravelToPlanetC2S decode(FriendlyByteBuf buf) {
		return new TravelToPlanetC2S(buf.readUtf(32767));
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			if (player == null) return;

			ServerLevel currentLevel = player.serverLevel();
			ResourceLocation targetId = ResourceLocation.tryParse(targetDimensionId);
			if (targetId == null) {
				return;
			}

			ResourceKey<Level> targetKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, targetId);
			ServerLevel targetLevel = player.server.getLevel(targetKey);
			if (targetLevel == null || targetLevel == currentLevel) {
				return;
			}

			player.stopRiding();
			List<SpacePodEntity> nearbyPods = currentLevel.getEntitiesOfClass(
					SpacePodEntity.class,
					player.getBoundingBox().inflate(10.0D)
			);

			for (SpacePodEntity pod : nearbyPods) {
				if (!pod.isVehicle()) {
					pod.discard();
				}
			}

			BlockPos targetPos;
			if (targetKey.equals(OtherworldDimension.OTHERWORLD_KEY)) {
				targetPos = new BlockPos(54, 210, 1082);
			} else {
				double newY = player.getY();
				if (newY < 0) newY = 180;
				else if (newY < 60) newY += 90;

				targetPos = new BlockPos((int) player.getX(), (int) newY, (int) player.getZ());
			}

			player.teleportTo(targetLevel, targetPos.getX(), targetPos.getY(), targetPos.getZ(), player.getYRot(), player.getXRot());

			SpacePodEntity newPod = new SpacePodEntity(MainEntities.SPACE_POD.get(), targetLevel);
			newPod.setPos(targetPos.getX(), targetPos.getY(), targetPos.getZ());

			if (targetLevel.addFreshEntity(newPod)) {
				player.startRiding(newPod);
			}
		});
		context.get().setPacketHandled(true);
	}
}
