package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.server.world.tournament.Tournament;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
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

		if (MainGameRules.isInBuildProtectedArea(level, entity.blockPosition())) {
			event.setCanGrief(false);
		}
	}

	@SubscribeEvent
	public static void onAttack(LivingIncomingDamageEvent event) {
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

		if (target instanceof ServerPlayer player
				&& !Tournament.Manager.canDamageFighter(player, attacker, gameTime)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onPlayerHurt(LivingDamageEvent.Pre event) {
		LivingEntity target = event.getEntity();
		if (target == null || target.level().isClientSide()) return;

		if (target instanceof ServerPlayer player) {
			if (!Tournament.Manager.isInNonLethalMatch(player)) return;
			if (player.getHealth() - event.getNewDamage() > 0.0F) return;

			event.setNewDamage(0.0F);
			Tournament.Manager.knockOut(player);
			return;
		}

		if (!(target instanceof net.minecraft.world.entity.Mob mob)) return;
		if (!Tournament.Manager.isNonLethalOpponent(mob)) return;
		if (mob.getHealth() - event.getNewDamage() > 0.0F) return;

		event.setNewDamage(0.0F);
		Tournament.Manager.knockOutOpponent(mob);
	}

	@SubscribeEvent
	public static void onChangeTarget(LivingChangeTargetEvent event) {
		LivingEntity mob = event.getEntity();
		if (mob == null || mob.level().isClientSide()) return;

		UUID owner = Tournament.Manager.ownerOf(mob);
		if (owner == null) return;

		LivingEntity newTarget = event.getNewAboutToBeSetTarget();
		if (newTarget == null) return;
		if (!(newTarget instanceof ServerPlayer player) || !player.getUUID().equals(owner)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
		handleInteract(event);
	}

	@SubscribeEvent
	public static void onInteract(PlayerInteractEvent.RightClickItem event) {
		handleInteract(event);
	}

	@SubscribeEvent
	public static void onInteract(PlayerInteractEvent.EntityInteract event) {
		handleInteract(event);
	}

	@SubscribeEvent
	public static void onInteract(PlayerInteractEvent.EntityInteractSpecific event) {
		handleInteract(event);
	}

	@SubscribeEvent
	public static void onInteract(PlayerInteractEvent.LeftClickBlock event) {
		handleInteract(event);
	}

	private static void handleInteract(PlayerInteractEvent event) {
		if (event.getLevel().isClientSide()) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (Tournament.Manager.isInGrace(player.getUUID(), event.getLevel().getGameTime())) {
			if (event instanceof PlayerInteractEvent.RightClickBlock rightClick) rightClick.setCanceled(true);
			else if (event instanceof PlayerInteractEvent.LeftClickBlock leftClick) leftClick.setCanceled(true);
			else if (event instanceof PlayerInteractEvent.EntityInteract entityInteract) entityInteract.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
		}
	}

	@SubscribeEvent
	public static void onUseItem(LivingEntityUseItemEvent.Start event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (Tournament.Manager.isInGrace(player.getUUID(), player.level().getGameTime())) event.setCanceled(true);
	}

	private static void notifyDenied(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.displayClientMessage(
					Component.translatable("message.dragonminez.arena.protected"), true);
		}
	}
}
