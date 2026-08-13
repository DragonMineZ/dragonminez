package com.dragonminez.client.render.compat;

import java.lang.reflect.Method;

/** Optional bridge to Aero Cam Sync without making it a required DMZ dependency. */
public final class AeroCamSyncCompat {
	private static final Method IS_RENDERING_FIRST_PERSON_BODY = resolveMethod();

	private AeroCamSyncCompat() {}

	public static boolean isLoaded() {
		return IS_RENDERING_FIRST_PERSON_BODY != null;
	}

	private static Method resolveMethod() {
		try {
			Class<?> compat = Class.forName("com.playsi.aero_cam_sync.client.utils.FirstPersonCompat");
			return compat.getMethod("isRenderingFirstPersonBody");
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}

	public static boolean isRenderingFirstPersonBody() {
		if (IS_RENDERING_FIRST_PERSON_BODY == null) return false;
		try {
			return Boolean.TRUE.equals(IS_RENDERING_FIRST_PERSON_BODY.invoke(null));
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}
}
