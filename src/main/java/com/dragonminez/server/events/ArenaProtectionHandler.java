package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.server.world.tournament.Tournament;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArenaProtectionHandler {

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		Player player = event.getPlayer();
		if (!(event.getLevel() instanceof Level level)) return;
		if (MainGameRules.canModifyBlock(level, event.getPos(), player)) return;

		event.setCanceled(true);
		notifyDenied(player);
	}

	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		if (!(event.getLevel() instanceof Level level)) return;
		if (MainGameRules.canModifyBlock(level, event.getPos(), player)) return;

		event.setCanceled(true);
		notifyDenied(player);
	}

    @SubscribeEvent
	public static void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		if (!(event.getLevel() instanceof Level level)) return;

		for (var placed : event.getReplacedBlockSnapshots()) {
			if (MainGameRules.canModifyBlock(level, placed.getPos(), player)) continue;
			event.setCanceled(true);
			notifyDenied(player);
			return;
		}
	}

	@SubscribeEvent
	public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
		Level level = event.getLevel();
		if (level.isClientSide()) return;

		List<BlockPos> affected = event.getAffectedBlocks();
		if (affected.isEmpty()) return;
		affected.removeIf(pos -> !MainGameRules.canModifyBlock(level, pos, null));
	}

	@SubscribeEvent
	public static void onMobGriefing(EntityMobGriefingEvent event) {
		Entity entity = event.getEntity();
		if (entity == null) return;

		Level level = entity.level();
		if (level.isClientSide()) return;
		if (level.getGameRules().getBoolean(MainGameRules.ALLOW_BUILDING_IN_ARENA_STRUCTURES)) return;

		if (MainGameRules.isInsideTaggedStructure(level, entity.blockPosition(),
				MainTags.Structures.BUILD_PROTECTED)) {
			event.setResult(Event.Result.DENY);
		}
	}

	@SubscribeEvent
	public static void onAttack(LivingAttackEvent event) {
		LivingEntity target = event.getEntity();
		if (target == null || target.level().isClientSide()) return;

		long gameTime = target.level().getGameTime();
		Entity attacker = event.getSource().getEntity();

		UUID owner = Tournament.Manager.ownerOf(target);
		if (owner != null) {
			if (!(attacker instanceof ServerPlayer player) || !player.getUUID().equals(owner)) {
				event.setCanceled(true);
				return;
			}
			if (Tournament.Manager.isInGrace(owner, gameTime)) {
				event.setCanceled(true);
			}
			return;
		}

		if (target instanceof ServerPlayer player) {
			UUID attackerOwner = Tournament.Manager.ownerOf(attacker);
			if (attackerOwner != null && attackerOwner.equals(player.getUUID())
					&& Tournament.Manager.isInGrace(player.getUUID(), gameTime)) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerHurt(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (!Tournament.Manager.isInNonLethalMatch(player)) return;
		if (player.getHealth() - event.getAmount() > 0.0F) return;

		event.setCanceled(true);
		Tournament.Manager.knockOut(player);
	}

	@SubscribeEvent
	public static void onChangeTarget(LivingChangeTargetEvent event) {
		LivingEntity mob = event.getEntity();
		if (mob == null || mob.level().isClientSide()) return;

		UUID owner = Tournament.Manager.ownerOf(mob);
		if (owner == null) return;

		LivingEntity newTarget = event.getNewTarget();
		if (newTarget == null) return;
		if (!(newTarget instanceof ServerPlayer player) || !player.getUUID().equals(owner)) {
			event.setCanceled(true);
		}
	}

	private static void notifyDenied(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.displayClientMessage(
					Component.translatable("message.dragonminez.arena.protected"), true);
		}
	}
}
