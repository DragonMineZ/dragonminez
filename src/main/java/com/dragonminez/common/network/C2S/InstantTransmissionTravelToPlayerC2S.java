package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.ITTeleportHelper;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class InstantTransmissionTravelToPlayerC2S {

	private static final int MENU_SKILL_LEVEL = 5;
	private static final int CROSS_DIMENSION_SKILL_LEVEL = 10;

	private final UUID targetId;

	public InstantTransmissionTravelToPlayerC2S(UUID targetId) {
		this.targetId = targetId;
	}

	public InstantTransmissionTravelToPlayerC2S(FriendlyByteBuf buf) {
		this.targetId = buf.readUUID();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUUID(targetId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int skillLevel = data.getSkills().getSkillLevel("instant_transmission");
				if (skillLevel < MENU_SKILL_LEVEL) return;

				ServerPlayer target = player.server.getPlayerList().getPlayer(targetId);
				if (target == null || target.getUUID().equals(player.getUUID())) return;

				StatsData targetData = StatsProvider.get(StatsCapability.INSTANCE, target).orElse(null);
				if (targetData == null || !targetData.getStatus().isHasCreatedCharacter()) return;
				if (TransformationsHelper.isInstantTransmissionBlocked(data, targetData)) return;

				boolean sameDimension = target.level().dimension().equals(player.level().dimension());

				if (PartyManager.areInSameParty(player, target)) {
					if (!sameDimension && skillLevel < CROSS_DIMENSION_SKILL_LEVEL) return;
				} else {
					long selfBp = data.getBattlePowerExact();
					int rangePerLevel = ConfigManager.getServerConfig().getGameplay().getInstantTransmissionPlayerRangePerLevel();
					double maxRange = (double) rangePerLevel * skillLevel;
					if (!RequestITTargetsC2S.isValidExternalTarget(player, data, selfBp, target, targetData, maxRange)) return;
				}

				teleportTo(player, target);
			});
		});
		ctx.get().setPacketHandled(true);
	}

	private void teleportTo(ServerPlayer player, ServerPlayer target) {
		ServerLevel targetLevel = target.serverLevel();
		BlockPos center = target.blockPosition();
		BlockPos safePos = ITTeleportHelper.findSafeTeleportPos(targetLevel, center);

		if (player.isVehicle()) player.stopRiding();

		double dX = center.getX() - safePos.getX();
		double dZ = center.getZ() - safePos.getZ();
		float yaw = (float) (Math.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;

		player.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, yaw, player.getXRot());
		player.playNotifySound(MainSounds.TP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
	}
}
