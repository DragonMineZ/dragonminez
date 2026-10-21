package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StrikeCinematicS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StrikeCinematics {

	private enum Source { STRIKE, COMBO, BOSS_ABILITY }

	private record Take(LivingEntity attacker, UUID victimId, Source source, int move) {}

	private static final Map<UUID, Take> TAKES = new HashMap<>();

	private StrikeCinematics() {}

	public static void startStrike(ServerPlayer attacker, LivingEntity victim) {
		start(attacker, victim, Source.STRIKE, -1);
	}

	public static void startCombo(DBSagasEntity attacker, LivingEntity victim, int comboId) {
		if (victim instanceof ServerPlayer) start(attacker, victim, Source.COMBO, comboId);
	}

	public static void startBossAbility(WorldBossEntity attacker, LivingEntity victim, int ability) {
		if (victim instanceof ServerPlayer) start(attacker, victim, Source.BOSS_ABILITY, ability);
	}

	private static void start(LivingEntity attacker, LivingEntity victim, Source source, int move) {
		if (attacker.level().isClientSide || victim == null) return;
		Take current = TAKES.get(attacker.getUUID());
		if (current != null && current.victimId().equals(victim.getUUID())) {
			if (current.source() != source || current.move() != move) TAKES.put(attacker.getUUID(), new Take(attacker, victim.getUUID(), source, move));
			return;
		}
		stop(attacker);
		if (!(attacker instanceof ServerPlayer) && !(victim instanceof ServerPlayer)) return;
		StrikeCinematicS2C packet = StrikeCinematicS2C.start(attacker.getId(), victim.getId());
		if (attacker instanceof ServerPlayer player) NetworkHandler.sendToPlayer(packet, player);
		if (victim instanceof ServerPlayer player) NetworkHandler.sendToPlayer(packet, player);
		TAKES.put(attacker.getUUID(), new Take(attacker, victim.getUUID(), source, move));
	}

	public static void stop(LivingEntity attacker) {
		Take take = TAKES.remove(attacker.getUUID());
		if (take == null) return;
		StrikeCinematicS2C packet = StrikeCinematicS2C.stop(attacker.getId());
		if (attacker instanceof ServerPlayer player) NetworkHandler.sendToPlayer(packet, player);
		MinecraftServer server = attacker.getServer();
		ServerPlayer victim = server != null ? server.getPlayerList().getPlayer(take.victimId()) : null;
		if (victim != null) NetworkHandler.sendToPlayer(packet, victim);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || TAKES.isEmpty()) return;
		for (Take take : new ArrayList<>(TAKES.values())) {
			if (take.source() == Source.STRIKE || stillPerforming(event.getServer(), take)) continue;
			stop(take.attacker());
		}
	}

	private static boolean stillPerforming(MinecraftServer server, Take take) {
		LivingEntity attacker = take.attacker();
		if (attacker.isRemoved() || !attacker.isAlive()) return false;
		ServerPlayer victim = server.getPlayerList().getPlayer(take.victimId());
		if (victim == null || victim.level() != attacker.level()) return false;
		if (take.source() == Source.BOSS_ABILITY) {
			return attacker instanceof WorldBossEntity boss && boss.getBossAbility() == take.move();
		}
		return attacker instanceof DBSagasEntity saga && saga.isComboing() && saga.getComboId() == take.move();
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		stop(event.getEntity());
	}
}
