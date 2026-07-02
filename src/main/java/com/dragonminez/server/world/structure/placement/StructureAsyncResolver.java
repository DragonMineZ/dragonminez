package com.dragonminez.server.world.structure.placement;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public final class StructureAsyncResolver {
	private static final int FORCE_LOAD_RADIUS = 1;

	private static final ExecutorService COORDINATOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "DMZ-StructurePlanner");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});

	private static final ForkJoinPool SEARCH_POOL = new ForkJoinPool(
			Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1)),
			new SearchThreadFactory(),
			null,
			false);

	private StructureAsyncResolver() {}

	static void buildPlan(StructureSpawnPlanner.PlanHolder holder) {
		COORDINATOR.submit(() -> {
			try {
				StructureSpawnPlanner.runBuild(holder, SEARCH_POOL);
			} catch (Throwable t) {
				System.err.println("[DMZ] StructureAsyncResolver build failed: " + t.getMessage());
				holder.publish(java.util.Collections.emptyMap());
			}
		});
	}

	static void buildPlanSync(StructureSpawnPlanner.PlanHolder holder) {
		try {
			StructureSpawnPlanner.runBuild(holder, SEARCH_POOL);
		} catch (Throwable t) {
			System.err.println("[DMZ] StructureAsyncResolver sync build failed: " + t.getMessage());
			holder.publish(java.util.Collections.emptyMap());
		}
	}

	static void forceGenerate(ChunkGenerator generator, ChunkPos pos) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null || generator == null) return;

		server.execute(() -> {
			ServerLevel target = null;
			for (ServerLevel level : server.getAllLevels()) {
				if (level.getChunkSource().getGenerator() == generator) {
					target = level;
					break;
				}
			}
			if (target == null) return;

			for (int dx = -FORCE_LOAD_RADIUS; dx <= FORCE_LOAD_RADIUS; dx++) {
				for (int dz = -FORCE_LOAD_RADIUS; dz <= FORCE_LOAD_RADIUS; dz++) {
					target.getChunk(pos.x + dx, pos.z + dz, ChunkStatus.FULL, true);
				}
			}
		});
	}

	private static final class SearchThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
		private final AtomicInteger counter = new AtomicInteger();

		@Override
		public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
			ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
			thread.setName("DMZ-StructureSearch-" + counter.incrementAndGet());
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY);
			return thread;
		}
	}
}
