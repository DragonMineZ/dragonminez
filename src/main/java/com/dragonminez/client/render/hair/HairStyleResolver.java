package com.dragonminez.client.render.hair;

import com.dragonminez.client.render.layer.AuraTintTracker;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairCodec;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HairStyleResolver {
	private static final float FADE_OUT_RATE = 0.05f;
	private static final float MILLIS_PER_TICK = 50.0f;
	private static final float MAX_DELTA_TICKS = 5.0f;
	private static final float KI_CHARGE_RISE = 0.25f;
	private static final float KI_CHARGE_FALL = 0.15f;
	private static final float AURA_TINT_INTENSITY = 0.2f;
	private static final long TRACKING_TTL_MS = 30_000L;
	private static final long CLEANUP_INTERVAL_MS = 5_000L;
	private static final int FORCED_CODE_CACHE_SIZE = 64;

	private static final Map<Integer, float[]> PUBLISHED_BASE_COLOR = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> PUBLISHED_BASE_TIME = new ConcurrentHashMap<>();
	private static final Map<String, CustomHair> FORCED_CODE_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
		protected boolean removeEldestEntry(Map.Entry<String, CustomHair> eldest) {
			return size() > FORCED_CODE_CACHE_SIZE;
		}
	};

	private final Map<Integer, Tracking> tracking = new HashMap<>();
	private long lastCleanupMs;

	public record StyleChoice(CustomHair hair, HairStyleSlot slot) {}

	public static final class Resolved {
		public CustomHair from;
		public CustomHair to;
		public HairStyleSlot fromSlot = HairStyleSlot.BASE;
		public HairStyleSlot toSlot = HairStyleSlot.BASE;
		public float factor;
		public float[] rgbFrom;
		public float[] rgbTo;
		public boolean forceColorFrom;
		public boolean forceColorTo;
		public float kiChargeProgress;
		public float auraIntensity;
	}

	private static final class Tracking {
		private float progress;
		private float kiCharge;
		private StyleChoice fadeTarget;
		private float[] fadeRgb;
		private boolean fadeForce;
		private long lastUpdateMs;
		private long lastSeenMs;
	}

	public Resolved resolve(Player player, StatsData stats, Resolved out) {
		Character character = stats.getCharacter();
		int entityId = player.getId();
		long nowMs = Util.getMillis();
		long gameTime = player.level().getGameTime();

		Tracking track = tracking.computeIfAbsent(entityId, id -> new Tracking());
		float deltaTicks = track.lastUpdateMs == 0L ? 0.0f : Math.min(MAX_DELTA_TICKS, (nowMs - track.lastUpdateMs) / MILLIS_PER_TICK);
		track.lastUpdateMs = nowMs;
		track.lastSeenMs = nowMs;

		StyleChoice from = new StyleChoice(style(player, character, HairStyleSlot.BASE), HairStyleSlot.BASE);
		float[] rgbFrom = character.getRgbHairColor();
		if (character.hasActiveForm()) {
			from = styleForForm(player, character, character.getActiveFormGroup(), character.getActiveForm());
			rgbFrom = rgbForForm(character, character.getActiveFormGroup(), character.getActiveForm());
		}
		if (character.hasActiveStackForm()) {
			from = styleForStackForm(player, character, character.getActiveStackFormGroup(), character.getActiveStackForm(), from);
			rgbFrom = rgbForStackForm(character.getActiveStackFormGroup(), character.getActiveStackForm(), rgbFrom);
		}

		boolean overrideFrom = hasColorOverride(character.hasActiveForm() ? character.getActiveFormData() : null)
				|| hasColorOverride(character.hasActiveStackForm() ? character.getActiveStackFormData() : null);

		StyleChoice to = from;
		float[] rgbTo = rgbFrom;
		boolean forceTo = overrideFrom;
		float factor = 0.0f;

		if (stats.getStatus().isActionCharging() && resolveChargeTarget(player, stats, character, from, rgbFrom, overrideFrom, track, deltaTicks)) {
			to = track.fadeTarget;
			rgbTo = track.fadeRgb;
			forceTo = track.fadeForce;
			factor = track.progress;
		} else if (track.progress > 0.0f && track.fadeTarget != null) {
			track.progress = Math.max(0.0f, track.progress - FADE_OUT_RATE * deltaTicks);
			if (track.progress > 0.0f) {
				to = track.fadeTarget;
				rgbTo = track.fadeRgb != null ? track.fadeRgb : rgbFrom;
				forceTo = track.fadeForce;
				factor = track.progress;
			} else {
				clearFade(track);
			}
		} else {
			clearFade(track);
		}

		FormConfig.FormData tintForm = DMZSkinLayer.resolveTintForm(stats);
		float[] formTint = tintForm != null ? tintForm.getRgbTintColor() : null;
		float formTintIntensity = tintForm != null ? (float) tintForm.getTintIntensity() : 0.0f;
		boolean hasFormTint = formTintIntensity > 0.0f && formTint != null;
		boolean auraFadeIn = stats.getStatus().isChargingKi() || stats.getStatus().isAuraActive() || stats.getStatus().isPermanentAura() || stats.getStatus().isForcedAura();
		float auraTint = AuraTintTracker.update(entityId, gameTime, auraFadeIn);

		if (hasFormTint || auraTint > 0.0f) {
			float[] baseFrom = rgbFrom;
			rgbFrom = baseFrom.clone();
			rgbTo = rgbTo == baseFrom ? rgbFrom : rgbTo.clone();
		}
		if (hasFormTint) {
			applyFormTint(rgbFrom, formTint, formTintIntensity);
			if (rgbTo != rgbFrom) applyFormTint(rgbTo, formTint, formTintIntensity);
		} else if (auraTint > 0.0f) {
			float[] auraRgb = resolveAuraRgb(character);
			applyAuraTint(rgbFrom, auraRgb, AURA_TINT_INTENSITY * auraTint);
			if (rgbTo != rgbFrom) applyAuraTint(rgbTo, auraRgb, AURA_TINT_INTENSITY * auraTint);
		}

		boolean kiCharging = stats.getStatus().isChargingKi() || stats.getStatus().isPermanentAura() || stats.getStatus().isForcedAura() || stats.getStatus().isForcedCharge() || stats.getStatus().isActionCharging();
		track.kiCharge = kiCharging
				? Math.min(1.0f, track.kiCharge + deltaTicks * KI_CHARGE_RISE)
				: Math.max(0.0f, track.kiCharge - deltaTicks * KI_CHARGE_FALL);

		publishBaseColor(entityId, gameTime, rgbFrom, rgbTo, factor);
		cleanup(nowMs);

		out.from = from.hair();
		out.fromSlot = from.slot();
		out.to = to.hair();
		out.toSlot = to.slot();
		out.factor = factor;
		out.rgbFrom = rgbFrom;
		out.rgbTo = rgbTo;
		out.forceColorFrom = overrideFrom;
		out.forceColorTo = forceTo;
		out.kiChargeProgress = track.kiCharge;
		out.auraIntensity = Math.max(auraTint, factor);
		return out;
	}

	public static float[] getPublishedBaseColor(int entityId, long gameTime) {
		Long time = PUBLISHED_BASE_TIME.get(entityId);
		if (time == null || gameTime - time > 2L) return null;
		return PUBLISHED_BASE_COLOR.get(entityId);
	}

	private boolean resolveChargeTarget(Player player, StatsData stats, Character character, StyleChoice from, float[] rgbFrom,
										boolean overrideFrom, Tracking track, float deltaTicks) {
		FormConfig.FormData nextForm;
		StyleChoice target;
		float[] targetRgb;
		int mastery;

		if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
			String group = character.getSelectedFormGroup();
			nextForm = TransformationsHelper.getNextAvailableForm(stats);
			if (nextForm == null) return false;
			target = styleForForm(player, character, group, nextForm.getName());
			targetRgb = rgbForForm(character, group, nextForm.getName());
			String masteryGroup = character.hasActiveForm() ? character.getActiveFormGroup() : group;
			mastery = (int) character.getFormMasteries().getMastery(masteryGroup, nextForm.getName());
		} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
			String group = character.getSelectedStackFormGroup();
			nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
			if (nextForm == null) return false;
			target = styleForStackForm(player, character, group, nextForm.getName(), from);
			targetRgb = rgbForStackForm(group, nextForm.getName(), rgbFrom);
			String masteryGroup = character.hasActiveStackForm() ? character.getActiveStackFormGroup() : group;
			mastery = (int) character.getStackFormMasteries().getMastery(masteryGroup, nextForm.getName());
		} else {
			return false;
		}

		float ratePerTick = (5 + Math.max(20, mastery)) / 2000.0f;
		track.progress = Math.min(1.0f, track.progress + ratePerTick * deltaTicks);
		track.fadeTarget = target;
		track.fadeRgb = nextForm.hasHairColorOverride() ? targetRgb : rgbFrom;
		track.fadeForce = nextForm.hasHairColorOverride() || overrideFrom;
		return true;
	}

	private static void clearFade(Tracking track) {
		track.progress = 0.0f;
		track.fadeTarget = null;
		track.fadeRgb = null;
		track.fadeForce = false;
	}

	private static CustomHair style(Player player, Character character, HairStyleSlot slot) {
		CustomHair editing = HairEditSession.resolve(player.getUUID(), slot);
		return editing != null ? editing : character.getHairStyle(slot);
	}

	private static StyleChoice styleForForm(Player player, Character character, String group, String formName) {
		FormConfig config = ConfigManager.getFormGroup(character.getRaceName(), group);
		FormConfig.FormData formData = config != null ? config.getForm(formName) : null;
		return styleForFormData(player, character, formData, new StyleChoice(style(player, character, HairStyleSlot.BASE), HairStyleSlot.BASE));
	}

	private static StyleChoice styleForStackForm(Player player, Character character, String group, String formName, StyleChoice fallback) {
		FormConfig config = ConfigManager.getStackFormGroup(group);
		FormConfig.FormData formData = config != null ? config.getForm(formName) : null;
		return styleForFormData(player, character, formData, fallback);
	}

	private static StyleChoice styleForFormData(Player player, Character character, FormConfig.FormData formData, StyleChoice fallback) {
		if (formData == null) return fallback;
		HairStyleSlot typedSlot = formData.hasDefinedHairType() ? HairStyleSlot.byHairType(formData.getHairType()) : null;
		if (formData.hasHairCodeOverride()) {
			CustomHair forced = decodeForced(formData.getForcedHairCode());
			if (forced != null) return new StyleChoice(forced, typedSlot != null ? typedSlot : HairStyleSlot.BASE);
		}
		if (formData.hasDefinedHairType()) {
			if (typedSlot == null) return new StyleChoice(character.emptyHair(), HairStyleSlot.BASE);
			return new StyleChoice(style(player, character, typedSlot), typedSlot);
		}
		return fallback;
	}

	private static CustomHair decodeForced(String code) {
		synchronized (FORCED_CODE_CACHE) {
			CustomHair cached = FORCED_CODE_CACHE.get(code);
			if (cached != null) return cached;
			CustomHair decoded = HairCodec.fromCode(code);
			if (decoded != null) FORCED_CODE_CACHE.put(code, decoded);
			return decoded;
		}
	}

	private static boolean hasColorOverride(FormConfig.FormData formData) {
		return formData != null && Boolean.TRUE.equals(formData.hasHairColorOverride());
	}

	private static float[] rgbForForm(Character character, String group, String formName) {
		FormConfig config = ConfigManager.getFormGroup(character.getRaceName(), group);
		if (config != null) {
			FormConfig.FormData formData = config.getForm(formName);
			if (formData != null && formData.getRgbHairColor() != null) return formData.getRgbHairColor();
		}
		return character.getRgbHairColor();
	}

	private static float[] rgbForStackForm(String group, String formName, float[] fallback) {
		FormConfig config = ConfigManager.getStackFormGroup(group);
		if (config != null) {
			FormConfig.FormData formData = config.getForm(formName);
			if (formData != null && formData.getRgbHairColor() != null) return formData.getRgbHairColor();
		}
		return fallback;
	}

	private static float[] resolveAuraRgb(Character character) {
		float[] auraRgb = character.getRgbAuraColor();
		FormConfig.FormData activeForm = character.hasActiveForm() ? character.getActiveFormData() : null;
		FormConfig.FormData activeStackForm = character.hasActiveStackForm() ? character.getActiveStackFormData() : null;
		if (activeForm != null && activeForm.getRgbAuraColor() != null) auraRgb = activeForm.getRgbAuraColor();
		if (activeStackForm != null && activeStackForm.getRgbAuraColor() != null) auraRgb = activeStackForm.getRgbAuraColor();
		return auraRgb;
	}

	private static void applyFormTint(float[] rgb, float[] tint, float intensity) {
		float scaled = Mth.clamp(intensity, 0.0f, 1.0f) * AuraTintTracker.darkTintScale(rgb);
		rgb[0] = Mth.clamp(rgb[0] * (1.0f - scaled) + tint[0] * scaled, 0.0f, 1.0f);
		rgb[1] = Mth.clamp(rgb[1] * (1.0f - scaled) + tint[1] * scaled, 0.0f, 1.0f);
		rgb[2] = Mth.clamp(rgb[2] * (1.0f - scaled) + tint[2] * scaled, 0.0f, 1.0f);
	}

	private static void applyAuraTint(float[] rgb, float[] auraRgb, float intensity) {
		float scaled = intensity * AuraTintTracker.darkTintScale(rgb);
		rgb[0] = rgb[0] * (1.0f - scaled) + auraRgb[0] * scaled;
		rgb[1] = rgb[1] * (1.0f - scaled) + auraRgb[1] * scaled;
		rgb[2] = rgb[2] * (1.0f - scaled) + auraRgb[2] * scaled;
	}

	private static void publishBaseColor(int entityId, long gameTime, float[] rgbFrom, float[] rgbTo, float factor) {
		float smooth = factor;
		if (smooth > 0.0001f && smooth < 0.9999f) smooth = smooth * smooth * (3.0f - 2.0f * smooth);
		float[] color;
		if (smooth <= 0.0001f) color = rgbFrom.clone();
		else if (smooth >= 0.9999f) color = rgbTo.clone();
		else color = new float[]{Mth.lerp(smooth, rgbFrom[0], rgbTo[0]), Mth.lerp(smooth, rgbFrom[1], rgbTo[1]), Mth.lerp(smooth, rgbFrom[2], rgbTo[2])};
		PUBLISHED_BASE_COLOR.put(entityId, color);
		PUBLISHED_BASE_TIME.put(entityId, gameTime);
	}

	private void cleanup(long nowMs) {
		if (nowMs - lastCleanupMs < CLEANUP_INTERVAL_MS) return;
		lastCleanupMs = nowMs;
		Iterator<Map.Entry<Integer, Tracking>> iterator = tracking.entrySet().iterator();
		while (iterator.hasNext()) {
			if (nowMs - iterator.next().getValue().lastSeenMs > TRACKING_TTL_MS) iterator.remove();
		}
	}
}
