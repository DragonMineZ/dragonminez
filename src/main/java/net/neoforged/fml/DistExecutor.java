package com.dragonminez.compat;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Compatibility shim for Forge DistExecutor.
 */
public final class DistExecutor {
	private DistExecutor() {}

	public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> toRun) {
		if (FMLEnvironment.dist == dist) {
			toRun.get().run();
		}
	}

	public static void runWhenOn(Dist dist, Supplier<Runnable> toRun) {
		unsafeRunWhenOn(dist, toRun);
	}

	public static <T> T unsafeCallWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
		if (FMLEnvironment.dist == dist) {
			try {
				return toRun.get().call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	public static <T> T callWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
		return unsafeCallWhenOn(dist, toRun);
	}
}
