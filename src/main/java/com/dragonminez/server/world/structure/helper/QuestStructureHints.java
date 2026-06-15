package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


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
			if (resolveFuture == null)
				resolveFuture = CompletableFuture.runAsync(() -> resolveAll(server), Util.backgroundExecutor());
			return resolveFuture;
		}
	}

	private static void resolveAll(MinecraftServer server) {
		for (Map.Entry<String, Pair<ResourceKey<Structure>, ResourceKey<Level>>> entry : DMZ_STRUCTURE_INFO.entrySet()) {
			String id = entry.getKey();
			if (CACHE.containsKey(id)) continue;
			try {
				QuestPrerequisites.StructureHint hint = resolve(server, id, entry.getValue());
				if (hint != null) CACHE.put(id, hint);
			} catch (Exception e) {
				LogUtil.error(Env.COMMON, "QuestStructureHints: failed to resolve hint for '" + id + "': " + e.getMessage());
			}
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
