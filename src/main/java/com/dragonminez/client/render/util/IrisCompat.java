package com.dragonminez.client.render.util;

import java.lang.reflect.Method;

/**
 * Soft-optional bridge to Iris/Oculus. The mod does not depend on Iris at
 * compile time; everything here is resolved reflectively so the class loads
 * fine whether or not Oculus is installed.
 *
 * <p>When a shaderpack (BSL, Complementary, ...) is loaded, Oculus replaces the
 * vanilla world pipeline with a deferred one. The mod's custom-shader effects
 * (auras, sparks, ki) and the vanilla PostChain outline get discarded by that
 * pipeline, so the renderers query {@link #isShaderPackInUse()} to switch to a
 * shader-compatible rendering path.</p>
 */
public final class IrisCompat {
	private static final String[] IRIS_API_CLASSES = {
			"net.irisshaders.iris.api.v0.IrisApi", // Oculus / Iris (recent)
			"net.coderbot.iris.api.v0.IrisApi"     // Oculus (legacy)
	};

	private static boolean resolved = false;
	private static Object apiInstance = null;
	private static Method isShaderPackInUseMethod = null;

	// Per-frame cache so we don't reflect on every draw call.
	private static long cachedFrame = Long.MIN_VALUE;
	private static boolean cachedInUse = false;

	private IrisCompat() {}

	private static void resolve() {
		if (resolved) return;
		resolved = true;

		for (String className : IRIS_API_CLASSES) {
			try {
				Class<?> apiClass = Class.forName(className);
				Method getInstance = apiClass.getMethod("getInstance");
				Object instance = getInstance.invoke(null);
				Method inUse = apiClass.getMethod("isShaderPackInUse");
				apiInstance = instance;
				isShaderPackInUseMethod = inUse;
				return;
			} catch (Throwable ignored) {
				// Try next candidate; if none match, Iris/Oculus is absent.
			}
		}
	}

	/**
	 * @return {@code true} when Oculus/Iris is present and a shaderpack is
	 *         currently active. Always {@code false} when the mod is absent.
	 */
	public static boolean isShaderPackInUse() {
		resolve();
		if (isShaderPackInUseMethod == null || apiInstance == null) return false;
		try {
			Object result = isShaderPackInUseMethod.invoke(apiInstance);
			return result instanceof Boolean && (Boolean) result;
		} catch (Throwable ignored) {
			return false;
		}
	}

	/**
	 * Per-frame cached variant of {@link #isShaderPackInUse()} for hot paths
	 * that query the state many times within a single rendered frame.
	 *
	 * @param frameId a value that changes once per frame (e.g. game time).
	 */
	public static boolean isShaderPackInUse(long frameId) {
		if (frameId != cachedFrame) {
			cachedFrame = frameId;
			cachedInUse = isShaderPackInUse();
		}
		return cachedInUse;
	}
}
