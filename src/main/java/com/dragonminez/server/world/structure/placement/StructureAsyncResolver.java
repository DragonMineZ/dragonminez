package com.dragonminez.server.world.structure.placement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public final class StructureAsyncResolver {

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

	/** Runs the search on the calling thread and blocks until it finishes. Used at world-load for the
	 *  spawn dimension so its plan is ready before spawn chunks generate. */
	static void buildPlanSync(StructureSpawnPlanner.PlanHolder holder) {
		try {
			StructureSpawnPlanner.runBuild(holder, SEARCH_POOL);
		} catch (Throwable t) {
			System.err.println("[DMZ] StructureAsyncResolver sync build failed: " + t.getMessage());
			holder.publish(java.util.Collections.emptyMap());
		}
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
