package com.dragonminez.client.util;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class SaiyanTailRules {
	private static final String SSJ4_KEY = "ssj4";
	private static final float WRAP_TICKS = 12.0f;
	private static final float ENROLLED_FADE_START = 0.85f;
	private static final float CURL_STAGGER = 0.6f;
	private static final float FIRST_TURN = Mth.HALF_PI;
	private static final float TOTAL_TURN = Mth.TWO_PI;
	private static final int MAX_TAIL_BONES = 16;
	private static final double STALE_TICKS = 40.0;
	private static final int PRUNE_SIZE = 256;
	private static final Map<Integer, WrapState> WRAPS = new HashMap<>();

	public static boolean wrapFrozen;

	private SaiyanTailRules() {}

	public static boolean ssj4ForcesTail(Character character) {
		if (formHidesTail(character)) return false;
		String form = character.getActiveForm();
		if (form != null) {
			String lowered = form.toLowerCase(Locale.ROOT);
			if (lowered.contains(SaiyanForms.SUPER_SAIYAN_4) || lowered.contains(SSJ4_KEY)) return true;
		}
		return isSsj4Model(character.hasActiveStackForm() ? character.getActiveStackFormData() : null)
				|| isSsj4Model(character.hasActiveForm() ? character.getActiveFormData() : null);
	}

	public static boolean hasTail(Character character) {
		if (formHidesTail(character)) return false;
		return character.isHasSaiyanTail() || ssj4ForcesTail(character);
	}

	public static boolean formHidesTail(Character character) {
		FormConfig.FormData stack = character.hasActiveStackForm() ? character.getActiveStackFormData() : null;
		FormConfig.FormData form = character.hasActiveForm() ? character.getActiveFormData() : null;
		return (stack != null && stack.hidesTail()) || (form != null && form.hidesTail());
	}

	public static float wrapAmount(Entity entity, boolean tailOut) {
		float target = tailOut ? 0.0f : 1.0f;
		if (wrapFrozen || entity == null || entity.level() == null) return target;
		double now = entity.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
		WrapState state = WRAPS.get(entity.getId());
		if (state == null || now < state.seen - 1.0 || now - state.seen > STALE_TICKS) {
			if (WRAPS.size() > PRUNE_SIZE) WRAPS.values().removeIf(s -> Math.abs(now - s.seen) > STALE_TICKS);
			state = new WrapState(tailOut, target, now);
			WRAPS.put(entity.getId(), state);
		} else if (state.out != tailOut) {
			state.from = state.amount(now);
			state.out = tailOut;
			state.start = now;
		}
		state.seen = now;
		return state.amount(now);
	}

	public static boolean looseTailShown(Entity entity, boolean tailOut) {
		return wrapAmount(entity, tailOut) < 1.0f;
	}

	public static float enrolledTailAlpha(Entity entity, boolean tailOut) {
		return Mth.clamp((wrapAmount(entity, tailOut) - ENROLLED_FADE_START) / (1.0f - ENROLLED_FADE_START), 0.0f, 1.0f);
	}

	public static void applyWrapCurl(Entity entity, boolean tailOut, Function<String, GeoBone> bones) {
		float amount = wrapAmount(entity, tailOut);
		if (amount <= 0.0f || amount >= 1.0f) return;
		int count = 0;
		while (count < MAX_TAIL_BONES && bones.apply("tail" + (count + 1)) != null) count++;
		if (count == 0) return;
		float laterTurn = count > 1 ? (TOTAL_TURN - FIRST_TURN) / (count - 1) : 0.0f;
		for (int i = 0; i < count; i++) {
			GeoBone bone = bones.apply("tail" + (i + 1));
			float delay = count > 1 ? CURL_STAGGER * i / (count - 1) : 0.0f;
			float local = Mth.clamp(amount * (1.0f + CURL_STAGGER) - delay, 0.0f, 1.0f);
			local = local * local * (3.0f - 2.0f * local);
			bone.setRotX(Mth.lerp(local, bone.getRotX(), 0.0f));
			bone.setRotY(Mth.lerp(local, bone.getRotY(), i == 0 ? FIRST_TURN : laterTurn));
			bone.setRotZ(Mth.lerp(local, bone.getRotZ(), 0.0f));
		}
	}

	private static boolean isSsj4Model(FormConfig.FormData form) {
		return form != null && Boolean.TRUE.equals(form.hasCustomModel()) && form.getCustomModel() != null
				&& form.getCustomModel().toLowerCase(Locale.ROOT).contains(SSJ4_KEY);
	}

	private static final class WrapState {
		private boolean out;
		private float from;
		private double start;
		private double seen;

		private WrapState(boolean out, float from, double now) {
			this.out = out;
			this.from = from;
			this.start = now;
			this.seen = now;
		}

		private float amount(double now) {
			float step = (float) ((now - start) / WRAP_TICKS);
			return out ? Math.max(0.0f, from - step) : Math.min(1.0f, from + step);
		}
	}
}
