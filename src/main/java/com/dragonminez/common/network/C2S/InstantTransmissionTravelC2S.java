package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.util.ITTeleportHelper;
import com.dragonminez.server.events.players.combat.DashHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
				if (!masters.containsKey(masterId)) {
					fail(player, "not_found");
					return;
				}

				var masterData = masters.get(masterId);
				String currentDim = player.level().dimension().location().toString();
				boolean sameDimension = masterData.getDimension().equals(currentDim);

				if (skillLevel < 10 && !sameDimension) {
					fail(player, "too_far");
					return;
				}

				ResourceLocation targetId = ResourceLocation.tryParse(masterData.getDimension());
				if (targetId == null) {
					fail(player, "unknown");
					return;
				}

				ResourceKey<Level> targetKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, targetId);
				ServerLevel targetLevel = player.server.getLevel(targetKey);

				if (targetLevel == null) {
					fail(player, "unknown");
					return;
				}

				BlockPos center = masterData.getPosition();
				boolean bypassCosts = player.isCreative() || player.isSpectator();

				if (!bypassCosts && data.getCooldowns().hasCooldown(Cooldowns.TELEPORT_CD)) {
					fail(player, "cooldown");
					return;
				}

				double distance = sameDimension ? Math.sqrt(player.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5)) : 0.0;
				int kiCost = (DashHandler.getFlyDashKiCost() * 5) + ITTeleportHelper.extraKiCostForDistance(distance);
				if (!bypassCosts) {
					if (data.getResources().getCurrentEnergy() < kiCost) {
						fail(player, "no_ki");
						return;
					}
					data.getResources().removeEnergy(kiCost);
					NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
					ITTeleportHelper.applyTeleportCooldown(player, data);
				}

				if (player.isVehicle()) player.stopRiding();

				BlockPos safePos = ITTeleportHelper.findSafeTeleportPos(targetLevel, center);

				double dX = center.getX() - safePos.getX();
				double dZ = center.getZ() - safePos.getZ();
				float yaw = (float) (Math.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;

				player.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, yaw, player.getXRot());
				player.playNotifySound(MainSounds.TP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			});
		});
		ctx.get().setPacketHandled(true);
	}

	private static void fail(ServerPlayer player, String reason) {
		player.displayClientMessage(Component.translatable("gui.dragonminez.transmission.fail." + reason), true);
	}
}