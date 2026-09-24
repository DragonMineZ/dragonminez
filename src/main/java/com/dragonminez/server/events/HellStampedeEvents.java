package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.sagas.MiniJanembaStampedeEntity;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import com.dragonminez.server.world.gen.OtherworldGeneration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HellStampedeEvents {

	private static final int CHECK_INTERVAL_TICKS = 20;
	private static final int TICKS_BEFORE_STAMPEDE = 20 * 60 * 5;
	private static final int MIN_JANEMBAS = 20;
	private static final int MAX_JANEMBAS = 20;
	private static final double SPAWN_DISTANCE = 20.0D;
	private static final double SPAWN_WIDTH = 16.0D;
	private static final double SPAWN_DEPTH = 6.0D;
	private static final int GROUND_SEARCH_UP = 6;
	private static final int GROUND_SEARCH_DOWN = 10;

	private static final Map<UUID, Integer> HELL_TIME = new HashMap<>();

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
		if (player.tickCount % CHECK_INTERVAL_TICKS != 0) return;

		UUID id = player.getUUID();
		if (!isInHell(player)) {
			HELL_TIME.remove(id);
			return;
		}

		int time = HELL_TIME.merge(id, CHECK_INTERVAL_TICKS, Integer::sum);
		if (time < TICKS_BEFORE_STAMPEDE) return;

		HELL_TIME.remove(id);
		spawnStampede(player);
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		HELL_TIME.remove(event.getEntity().getUUID());
	}

	private static boolean isInHell(ServerPlayer player) {
		if (player.isSpectator() || player.isCreative()) return false;
		return player.level().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)
				&& player.getY() < OtherworldGeneration.HELL_CEILING;
	}

	private static void spawnStampede(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		RandomSource random = player.getRandom();

		float angle = random.nextFloat() * Mth.TWO_PI;
		Vec3 forward = new Vec3(Mth.cos(angle), 0.0D, Mth.sin(angle));
		Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 origin = player.position().add(forward.scale(SPAWN_DISTANCE));

		int count = MIN_JANEMBAS + random.nextInt(MAX_JANEMBAS - MIN_JANEMBAS + 1);
		int spawned = 0;

		for (int i = 0; i < count; i++) {
			Vec3 spot = origin
					.add(side.scale((random.nextDouble() - 0.5D) * SPAWN_WIDTH))
					.add(forward.scale(random.nextDouble() * SPAWN_DEPTH));
			BlockPos ground = findGround(level, BlockPos.containing(spot.x, player.getY(), spot.z));
			if (ground == null) continue;

			MiniJanembaStampedeEntity janemba = MainEntities.MINI_JANEMBA_STAMPEDE.get().create(level);
			if (janemba == null) continue;

			double dx = player.getX() - (ground.getX() + 0.5D);
			double dz = player.getZ() - (ground.getZ() + 0.5D);
			float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
			janemba.moveTo(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, yaw, 0.0F);
			janemba.setYHeadRot(yaw);

			if (!level.addFreshEntity(janemba)) continue;
			janemba.charge(player);
			spawned++;
		}

		if (spawned == 0) return;

		player.displayClientMessage(Component.translatable("message.dragonminez.hell_stampede"), true);
		level.playSound(null, BlockPos.containing(origin), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 1.6F);
	}

	private static BlockPos findGround(ServerLevel level, BlockPos start) {
		if (!level.isLoaded(start)) return null;

		for (int dy = GROUND_SEARCH_UP; dy >= -GROUND_SEARCH_DOWN; dy--) {
			BlockPos feet = start.above(dy);
			BlockPos below = feet.below();
			if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
			if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) continue;
			if (!level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
			if (!level.getFluidState(feet).isEmpty()) continue;
			return feet;
		}
		return null;
	}
}
