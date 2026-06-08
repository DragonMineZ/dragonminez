package com.dragonminez.client.render.util;

import java.lang.reflect.Method;

public final class IrisCompat {
	private static final String[] IRIS_API_CLASSES = {
			"net.irisshaders.iris.api.v0.IrisApi",
			"net.coderbot.iris.api.v0.IrisApi"
	};

	private static boolean resolved = false;
	private static Object apiInstance = null;
	private static Method isShaderPackInUseMethod = null;

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
			} catch (Throwable ignored) {}
		}
	}

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

	public static boolean isShaderPackInUse(long frameId) {
		if (frameId != cachedFrame) {
			cachedFrame = frameId;
			cachedInUse = isShaderPackInUse();
		}
		return cachedInUse;
	}
}
