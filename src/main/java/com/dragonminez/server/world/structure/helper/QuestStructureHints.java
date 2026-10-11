package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestStructureHints {
	private static final Map<String, Pair<ResourceKey<Structure>, ResourceKey<Level>>> DMZ_STRUCTURE_INFO = Map.of(
			Reference.MOD_ID + ":goku_house", Pair.of(DMZStructures.GOKU_HOUSE, Level.OVERWORLD),
			Reference.MOD_ID + ":roshi_house", Pair.of(DMZStructures.ROSHI_HOUSE, Level.OVERWORLD),
			Reference.MOD_ID + ":elder_guru", Pair.of(DMZStructures.ELDER_GURU, NamekDimension.NAMEK_KEY),
			Reference.MOD_ID + ":timechamber", Pair.of(DMZStructures.TIMECHAMBER, HTCDimension.HTC_KEY),
			Reference.MOD_ID + ":kamilookout", Pair.of(DMZStructures.KAMILOOKOUT, Level.OVERWORLD),
			Reference.MOD_ID + ":gero_lab", Pair.of(DMZStructures.GERO_LAB, Level.OVERWORLD),
			Reference.MOD_ID + ":babidi", Pair.of(DMZStructures.BABIDI, Level.OVERWORLD)
	);

	private static final Map<String, QuestPrerequisites.StructureHint> CACHE = new ConcurrentHashMap<>();

	private static final Deque<String> PENDING = new ArrayDeque<>();

	private static volatile CompletableFuture<Void> resolveFuture;

	private QuestStructureHints() {}

	public static QuestPrerequisites.StructureHint getCached(String structureId) {
		String normalized = normalize(structureId);
		return normalized == null ? null : CACHE.get(normalized);
	}

	public static boolean isResolved() {
		CompletableFuture<Void> f = resolveFuture;
		return f != null && f.isDone();
	}

	public static CompletableFuture<Void> ensureResolvedAsync(MinecraftServer server) {
		CompletableFuture<Void> f = resolveFuture;
		if (f != null) return f;
		synchronized (QuestStructureHints.class) {
			if (resolveFuture == null) {
				PENDING.clear();
				for (String id : DMZ_STRUCTURE_INFO.keySet()) {
					if (!CACHE.containsKey(id)) PENDING.add(id);
				}
				resolveFuture = new CompletableFuture<>();
				if (PENDING.isEmpty()) resolveFuture.complete(null);
			}
			return resolveFuture;
		}
	}

	public static void reset() {
		CompletableFuture<Void> previous;
		synchronized (QuestStructureHints.class) {
			previous = resolveFuture;
			resolveFuture = null;
			PENDING.clear();
			CACHE.clear();
		}
		if (previous != null && !previous.isDone()) previous.cancel(false);
	}

	@SubscribeEvent
	public static void onServerStarting(ServerStartingEvent event) {
		reset();
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		reset();
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
		CompletableFuture<Void> f = resolveFuture;
		if (f == null || f.isDone()) return;

		String id;
		boolean finished;
		synchronized (QuestStructureHints.class) {
			if (f != resolveFuture) return;
			id = PENDING.poll();
		}
		if (id != null) resolveOne(event.getServer(), id);
		synchronized (QuestStructureHints.class) {
			finished = f == resolveFuture && PENDING.isEmpty();
		}
		if (finished) f.complete(null);
	}

	private static void resolveOne(MinecraftServer server, String id) {
		Pair<ResourceKey<Structure>, ResourceKey<Level>> info = DMZ_STRUCTURE_INFO.get(id);
		if (info == null || CACHE.containsKey(id)) return;
		try {
			QuestPrerequisites.StructureHint hint = resolve(server, id, info);
			if (hint != null) CACHE.put(id, hint);
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "QuestStructureHints: failed to resolve hint for '" + id + "': " + e.getMessage());
		}
	}

	private static QuestPrerequisites.StructureHint resolve(MinecraftServer server, String structureId,
														   Pair<ResourceKey<Structure>, ResourceKey<Level>> info) {
		String dimensionId = info.getSecond().location().toString();
		if (server == null) return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);

		ServerLevel targetLevel = server.getLevel(info.getSecond());
		if (targetLevel == null) return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);

		BlockPos structurePos = StructureLocator.locateStructure(targetLevel, info.getFirst(), targetLevel.getSharedSpawnPos());
		if (structurePos == null) return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);

		return new QuestPrerequisites.StructureHint(
				dimensionId,
				structurePos.getX(),
				structurePos.getY(),
				structurePos.getZ()
		);
	}

	private static String normalize(String structureId) {
		if (structureId == null || structureId.isBlank() || !structureId.contains(":")) return null;
		try {
			return ResourceLocation.parse(structureId).toString();
		} catch (Exception e) {
			return null;
		}
	}
}
