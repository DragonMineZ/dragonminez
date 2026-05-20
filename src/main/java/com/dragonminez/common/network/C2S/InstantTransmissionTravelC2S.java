package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class InstantTransmissionTravelC2S {
	private final String masterId;

	public InstantTransmissionTravelC2S(String masterId) {
		this.masterId = masterId;
	}

	public InstantTransmissionTravelC2S(FriendlyByteBuf buf) {
		this.masterId = buf.readUtf(256);
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(masterId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int skillLevel = data.getSkills().getSkillLevel("instant_transmission");
				if (skillLevel < 5) return;

				var masters = data.getCharacter().getInteractedMasters();
				if (masters.containsKey(masterId)) {
					var masterData = masters.get(masterId);
					String currentDim = player.level().dimension().location().toString();

					if (skillLevel < 10 && !masterData.getDimension().equals(currentDim)) return;

					ResourceLocation targetId = ResourceLocation.tryParse(masterData.getDimension());
					if (targetId == null) return;

					ResourceKey<Level> targetKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, targetId);
					ServerLevel targetLevel = player.server.getLevel(targetKey);

					if (targetLevel == null) return;

					if (player.isVehicle()) player.stopRiding();

					BlockPos center = masterData.getPosition();
					BlockPos safePos = findSafeTeleportPos(targetLevel, center);

					double dX = center.getX() - safePos.getX();
					double dZ = center.getZ() - safePos.getZ();
					float yaw = (float) (Math.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;

					player.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, yaw, player.getXRot());
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}

	private BlockPos findSafeTeleportPos(ServerLevel level, BlockPos center) {
		for (int r = 3; r <= 4; r++) {
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					double dist = Math.sqrt(x * x + z * z);
					if (dist >= 3.0 && dist <= 4.5) {
						for (int y = 3; y >= -3; y--) {
							BlockPos testPos = center.offset(x, y, z);
							if (isSafe(level, testPos)) {
								return testPos;
							}
						}
					}
				}
			}
		}

		return center.above(1);
	}

	private boolean isSafe(ServerLevel level, BlockPos pos) {
		boolean hasFloor = !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();

		boolean bodyClear = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
		boolean headClear = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty() && level.getFluidState(pos.above()).isEmpty();

		return hasFloor && bodyClear && headClear;
	}
}