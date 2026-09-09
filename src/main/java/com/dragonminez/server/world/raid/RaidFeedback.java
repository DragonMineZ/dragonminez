package com.dragonminez.server.world.raid;

import net.minecraft.core.BlockPos;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RaidMusicS2C;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.FireworkRocketItem;

import java.util.List;

public final class RaidFeedback {

	private static final int VICTORY_ROCKETS = 6;
	private static final double ROCKET_SPREAD = 5.0D;

	public static final String VICTORY_MUSIC = "dragonminez:menu_music_12";
	private static final int HERO_LEVEL = 0;
	private static final int HERO_DURATION_TICKS = 20 * 60 * 10;

	private RaidFeedback() {}

	public static void announce(ServerPlayer player, String translationKey) {
		if (translationKey == null || translationKey.isBlank()) return;

		player.sendSystemMessage(Component.translatable(translationKey));
		player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	public static void celebrate(ServerLevel level, List<ServerPlayer> winners, BlockPos center,
								 List<Component> rewardLines) {
		for (ServerPlayer player : winners) {
			player.sendSystemMessage(Component.translatable("raid.dragonminez.victory"));

			if (!rewardLines.isEmpty()) {
				player.sendSystemMessage(Component.translatable("raid.dragonminez.rewards.header"));
				for (Component line : rewardLines) player.sendSystemMessage(line);
			}

			player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
			NetworkHandler.sendToPlayer(new RaidMusicS2C(VICTORY_MUSIC), player);

			player.addEffect(new MobEffectInstance(MainEffects.WORLD_HERO.get(),
					HERO_DURATION_TICKS, HERO_LEVEL, false, true, true));

			totemBurst(level, player);
		}

		launchFireworks(level, center);
	}

	private static void totemBurst(ServerLevel level, ServerPlayer player) {
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
				player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
				80, 0.4, 0.8, 0.4, 0.35);
	}

	public static void launchFireworks(ServerLevel level, BlockPos center) {
		for (int i = 0; i < VICTORY_ROCKETS; i++) {
			double x = center.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 2.0 * ROCKET_SPREAD;
			double z = center.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 2.0 * ROCKET_SPREAD;
			double y = center.getY() + 1.0;

			FireworkRocketEntity rocket = new FireworkRocketEntity(level, x, y, z, rocket(level.getRandom().nextInt(3)));
			level.addFreshEntity(rocket);
		}
		level.sendParticles(ParticleTypes.FIREWORK, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5,
				60, 2.0, 1.0, 2.0, 0.05);
	}

	private static ItemStack rocket(int shapeIndex) {
		ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);

		CompoundTag explosion = new CompoundTag();
		explosion.putByte("Type", (byte) switch (shapeIndex) {
			case 1 -> FireworkRocketItem.Shape.LARGE_BALL.getId();
			case 2 -> FireworkRocketItem.Shape.STAR.getId();
			default -> FireworkRocketItem.Shape.SMALL_BALL.getId();
		});
		explosion.putIntArray("Colors", new int[]{
				DyeColor.YELLOW.getFireworkColor(), DyeColor.RED.getFireworkColor(), DyeColor.PURPLE.getFireworkColor()});
		explosion.putBoolean("Trail", true);

		ListTag explosions = new ListTag();
		explosions.add(explosion);

		CompoundTag fireworks = new CompoundTag();
		fireworks.putByte("Flight", (byte) 1);
		fireworks.put("Explosions", explosions);

		stack.getOrCreateTag().put("Fireworks", fireworks);
		return stack;
	}
}
