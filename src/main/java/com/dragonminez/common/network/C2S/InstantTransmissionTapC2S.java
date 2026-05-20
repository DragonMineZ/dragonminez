package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class InstantTransmissionTapC2S {
	private final UUID targetId;

	public InstantTransmissionTapC2S(UUID targetId) {
		this.targetId = targetId;
	}

	public InstantTransmissionTapC2S(FriendlyByteBuf buf) {
		this.targetId = buf.readBoolean() ? buf.readUUID() : null;
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeBoolean(targetId != null);
		if (targetId != null) buf.writeUUID(targetId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				int skillLevel = data.getSkills().getSkillLevel("instant_transmission");
				if (skillLevel <= 0) return;

				ServerLevel level = player.serverLevel();
				LivingEntity finalTarget = null;

				if (targetId != null) {
					if (level.getEntity(targetId) instanceof LivingEntity le) finalTarget = le;
				} else {
					double range = 25.0 + (skillLevel * 10.0);
					Vec3 eyePos = player.getEyePosition();
					Vec3 viewVec = player.getViewVector(1.0F);
					AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(5.0D);

					List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, searchBox,
							e -> e != player && e.isAlive() && player.hasLineOfSight(e));

					if (!list.isEmpty()) {
						finalTarget = list.stream().max(Comparator.comparingDouble(LivingEntity::getMaxHealth)).orElse(null);
					}
				}

				if (finalTarget != null) {
					float targetYaw = finalTarget.getYRot();
					double rad = Math.toRadians(targetYaw);
					double xOffset = -Math.sin(rad) * -1.5;
					double zOffset = Math.cos(rad) * -1.5;

					double newX = finalTarget.getX() + xOffset;
					double newY = finalTarget.getY();
					double newZ = finalTarget.getZ() + zOffset;

					player.teleportTo(level, newX, newY, newZ, finalTarget.getYRot(), player.getXRot());
					player.playNotifySound(MainSounds.TP_SHORT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
				}
			});
		});
		ctx.get().setPacketHandled(true);
	}
}