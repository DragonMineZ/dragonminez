package com.dragonminez.common.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.InstantTransmissionVfxS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Shared safe-landing logic for Instant Transmission teleports (masters and players).
 */
public final class ITTeleportHelper {

	private ITTeleportHelper() {}

	public static int extraKiCostForDistance(double distance) {
		if (distance <= 0) return 0;
		return (int) (distance / 25.0) * 5;
	}

	public static void departVfx(ServerPlayer player, SoundEvent sound) {
		NetworkHandler.sendToTrackingEntityAndSelf(new InstantTransmissionVfxS2C(player.getId(), false), player);
		player.level().playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	public static void arriveVfx(ServerPlayer player, SoundEvent sound) {
		NetworkHandler.sendToTrackingEntityAndSelf(new InstantTransmissionVfxS2C(player.getId(), true), player);
		player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.level().playSound(player, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	public static int interplanetaryKiCost(String sourceDimension, String destinationDimension) {
		if (sourceDimension.equals(destinationDimension)) return 0;
		int sourceValue = dimensionTravelValue(sourceDimension);
		int destinationValue = dimensionTravelValue(destinationDimension);
		return sourceValue < 0 || destinationValue < 0 ? 10_000 : Math.abs(sourceValue - destinationValue);
	}

	private static int dimensionTravelValue(String dimension) {
		// These fees are intentionally arbitrary; suggestions for better values are welcome. :)
		return switch (dimension) {
			case "minecraft:overworld" -> 0;
			case "minecraft:the_nether" -> 12_000;
			case "minecraft:the_end" -> 18_000;
			case "dragonminez:namek" -> 2_000;
			case "dragonminez:sacredkaiplanet" -> 8_000;
			case "dragonminez:cereal_planet" -> 12_000;
			case "dragonminez:demon_realm" -> 15_000;
			case "dragonminez:otherworld" -> 20_000;
			case "dmzsuper:beerus_planet" -> 25_000;
			case "dragonminez:time_chamber" -> 10_000;
			default -> -1;
		};
	}

	public static int travelKiCost(int baseCost, double distance, String sourceDimension, String destinationDimension) {
		return baseCost + extraKiCostForDistance(distance)
				+ interplanetaryKiCost(sourceDimension, destinationDimension);
	}

	public static void applyTeleportCooldown(ServerPlayer player, StatsData data) {
		int ticks = Math.max(0, ConfigManager.getCombatConfig().getTeleportCooldownSeconds()) * 20;
		if (ticks <= 0) return;
		data.getCooldowns().setCooldown(Cooldowns.TELEPORT_CD, ticks);
		player.addEffect(new MobEffectInstance(MainEffects.TELEPORT_CD, ticks, 0, false, false, true));
	}

	/** Finds a safe block to land on in a ring around {@code center}, falling back to {@code center.above()}. */
	public static BlockPos findSafeTeleportPos(ServerLevel level, BlockPos center) {
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

	public static boolean isSafe(ServerLevel level, BlockPos pos) {
		boolean hasFloor = !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();

		boolean bodyClear = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
		boolean headClear = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty() && level.getFluidState(pos.above()).isEmpty();

		return hasFloor && bodyClear && headClear;
	}
}
