package com.dragonminez.common.util;

public interface IHealthFixable {

	void dragonminez$setHealthRestorePoint(float restorePoint);

	/** Apply deferred Health NBT restore once max health attributes are ready. */
	void dragonminez$applyDeferredHealthRestore();
}

