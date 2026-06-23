package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.combat.DashHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

					int kiCost = DashHandler.getFlyDashKiCost() * 5;
					if (!player.isCreative() && !player.isSpectator()) {
						if (data.getResources().getCurrentEnergy() < kiCost) return;
						data.getResources().removeEnergy(kiCost);
						NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
					}

					if (player.isVehicle()) player.stopRiding();

					BlockPos center = masterData.getPosition();
					BlockPos safePos = com.dragonminez.common.util.ITTeleportHelper.findSafeTeleportPos(targetLevel, center);

					double dX = center.getX() - safePos.getX();
					double dZ = center.getZ() - safePos.getZ();
					float yaw = (float) (Math.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;

					player.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, yaw, player.getXRot());
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}
}