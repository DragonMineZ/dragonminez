package com.dragonminez.client.animation;

import software.bernie.geckolib.core.animation.RawAnimation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationCache {
	private static final Map<String, RawAnimation> PLAY_CACHE = new ConcurrentHashMap<>();
	private static final Map<String, RawAnimation> LOOP_CACHE = new ConcurrentHashMap<>();
	private static final Map<String, RawAnimation> PLAY_AND_HOLD_CACHE = new ConcurrentHashMap<>();

	public static RawAnimation getPlay(String animName) {
		return PLAY_CACHE.computeIfAbsent(animName, k -> RawAnimation.begin().thenPlay(k));
	}

	public static RawAnimation getLoop(String animName) {
		return LOOP_CACHE.computeIfAbsent(animName, k -> RawAnimation.begin().thenLoop(k));
	}

	public static RawAnimation getPlayAndHold(String animName) {
		return PLAY_AND_HOLD_CACHE.computeIfAbsent(animName, k -> RawAnimation.begin().thenPlayAndHold(k));
	}

	public static void clear() {
		PLAY_CACHE.clear();
		LOOP_CACHE.clear();
		PLAY_AND_HOLD_CACHE.clear();
	}
}