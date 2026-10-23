package com.dragonminez.common.stats;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.function.Function;

public final class FormTransition {
	private static final int CHARGE_STEP_TICKS = 20;
	private static final int FULL_CHARGE = 100;
	private static final float RELEASE_TICKS = 20.0f;
	private static final float REVERT_TICKS = 20.0f;
	private static final float CATCH_UP_TICKS = 4.0f;
	private static final int SETTLE_TICKS = 20;
	private static final float EPSILON = 0.001f;

	public record Look(String formGroup, String form, FormConfig.FormData formData,
					   String stackGroup, String stack, FormConfig.FormData stackData) {
		public static Look current(Character character) {
			return new Look(character.getActiveFormGroup(), character.getActiveForm(), character.getActiveFormData(),
					character.getActiveStackFormGroup(), character.getActiveStackForm(), character.getActiveStackFormData());
		}

		public boolean hasForm() {
			return formGroup != null && !formGroup.isEmpty() && form != null && !form.isEmpty();
		}

		public boolean hasStack() {
			return stackGroup != null && !stackGroup.isEmpty() && stack != null && !stack.isEmpty();
		}

		boolean matches(Character character) {
			return Objects.equals(formGroup, character.getActiveFormGroup()) && Objects.equals(form, character.getActiveForm())
					&& Objects.equals(stackGroup, character.getActiveStackFormGroup()) && Objects.equals(stack, character.getActiveStackForm());
		}

		public float[] overlay(float[] base, Function<FormConfig.FormData, float[]> rgb) {
			float[] out = base;
			if (formData != null) {
				float[] value = rgb.apply(formData);
				if (value != null) out = value;
			}
			if (stackData != null) {
				float[] value = rgb.apply(stackData);
				if (value != null) out = value;
			}
			return out;
		}

		public String customModel() {
			if (stackData != null && Boolean.TRUE.equals(stackData.hasCustomModel())) return stackData.getCustomModel().toLowerCase();
			if (formData != null && Boolean.TRUE.equals(formData.hasCustomModel())) return formData.getCustomModel().toLowerCase();
			return "";
		}

		public Float[] modelScaling(Character character) {
			return character.getModelScalingFor(formData, stackData);
		}
	}

	public record Charge(FormConfig.FormData target, boolean stack, String group, Look look, float factor) {}

	public record Revert(Look look, float factor) {}

	private boolean ready;
	private String identity;
	private Look look;
	private long clock;
	private double latestTime;
	private double observedAt = Double.NaN;
	private boolean locallyReleased;
	private boolean dimensionsDirty;

	private FormConfig.FormData chargeTarget;
	private boolean chargeStack;
	private String chargeGroup;
	private String chargeKey;
	private boolean charging;
	private int serverCharge;
	private int observedStep;
	private float planFrom;
	private double planStart;
	private double planEnd;
	private float fadeFrom;
	private double fadeStart = -1.0;

	private Look revertLook;
	private float revertFrom;
	private double revertStart;
	private float revertTicks;

	public void tick(StatsData data, int entityTickCount) {
		clock++;
		latestTime = Math.max(latestTime, clock);
		Character character = data.getCharacter();
		String currentIdentity = character.getRaceName() + "|" + character.isFused();

		if (entityTickCount < SETTLE_TICKS || !ready || !data.getStatus().isHasCreatedCharacter() || !currentIdentity.equals(identity)) {
			reset();
			identity = currentIdentity;
			look = Look.current(character);
			ready = data.getStatus().isHasCreatedCharacter() && entityTickCount >= SETTLE_TICKS;
			return;
		}

		observe(data, clock);
		if (revertLook != null && revertAt(clock) <= 0.0f) revertLook = null;
	}

	public void setLocallyReleased(boolean locallyReleased) {
		this.locallyReleased = locallyReleased;
	}

	public boolean isReady() {
		return ready;
	}

	public double frameTime(float partialTick) {
		return clock + partialTick;
	}

	public double now() {
		return latestTime;
	}

	public double observe(StatsData data, double time) {
		if (!ready) return time;
		if (time < latestTime) time = latestTime;
		latestTime = time;

		Character character = data.getCharacter();
		if (!look.matches(character)) {
			Look now = Look.current(character);
			if (now.hasForm() && TransformationsHelper.isGodRitualGroup(character.getRaceName(), now.formGroup())) snap();
			else onLookChanged(now, time);
			look = now;
		}
		if (time != observedAt) {
			observedAt = time;
			observeCharge(data, character, time);
		}
		return time;
	}

	public boolean consumeDimensionRefresh() {
		boolean active = ready && (chargeTarget != null || revertLook != null);
		boolean refresh = active || dimensionsDirty;
		dimensionsDirty = active;
		return refresh;
	}

	public Charge charge(Character character, double time) {
		if (!ready || chargeTarget == null) return null;
		float factor = chargeAt(time);
		if (factor <= 0.0f) return null;
		return new Charge(chargeTarget, chargeStack, chargeGroup, chargeLook(character), factor);
	}

	public Revert revert(double time) {
		if (!ready || revertLook == null) return null;
		float factor = revertAt(time);
		if (factor <= 0.0f) return null;
		return new Revert(revertLook, factor);
	}

	public float[] modelScale(Character character, double time) {
		Float[] resolved = character.getResolvedModelScaling();
		float[] out = {resolved[0], resolved[1], resolved[2]};
		Charge charge = charge(character, time);
		if (charge != null) out = lerp(out, toFloats(charge.look().modelScaling(character)), charge.factor());
		Revert revert = revert(time);
		if (revert != null) out = lerp(out, toFloats(revert.look().modelScaling(character)), revert.factor());
		return out;
	}

	private void reset() {
		if (chargeTarget != null || revertLook != null) dimensionsDirty = true;
		clearCharge();
		revertLook = null;
		observedAt = Double.NaN;
	}

	private void snap() {
		if (chargeTarget != null || revertLook != null) dimensionsDirty = true;
		clearCharge();
		revertLook = null;
		observedAt = Double.NaN;
	}

	private void onLookChanged(Look now, double time) {
		Look previous = look;
		float charge = chargeAt(time);
		boolean completed = chargeTarget != null && reachedTarget(now);
		clearCharge();
		observedAt = Double.NaN;

		if (!completed) {
			startRevert(previous, 1.0f, REVERT_TICKS, time);
			return;
		}
		float remaining = 1.0f - charge;
		if (remaining > EPSILON) startRevert(previous, remaining, CATCH_UP_TICKS, time);
		else revertLook = null;
	}

	private boolean reachedTarget(Look now) {
		String name = chargeTarget.getName();
		if (chargeStack) return name.equalsIgnoreCase(now.stack()) && sameGroup(chargeGroup, now.stackGroup());
		return name.equalsIgnoreCase(now.form()) && sameGroup(chargeGroup, now.formGroup());
	}

	private static boolean sameGroup(String expected, String actual) {
		return expected == null || expected.isEmpty() || expected.equalsIgnoreCase(actual);
	}

	private void startRevert(Look previous, float from, float ticks, double time) {
		revertLook = previous;
		revertFrom = from;
		revertStart = time;
		revertTicks = ticks;
	}

	private void observeCharge(StatsData data, Character character, double time) {
		FormConfig.FormData next = null;
		boolean stack = false;
		String group = null;
		if (data.getStatus().isActionCharging() && !locallyReleased) {
			ActionMode mode = data.getStatus().getSelectedAction();
			if (mode == ActionMode.FORM) {
				next = TransformationsHelper.presentNextForm(data);
				group = TransformationsHelper.getTransformTargetGroup(data);
			} else if (mode == ActionMode.STACK) {
				next = TransformationsHelper.presentNextStackForm(data);
				stack = true;
				group = character.hasActiveStackForm() ? character.getActiveStackFormGroup() : character.getSelectedStackFormGroup();
			}
		}

		int currentCharge = data.getResources().getActionCharge();
		if (next != null) {
			String key = (stack ? "stack|" : "form|") + group + "|" + next.getName();
			if (!key.equals(chargeKey)) {
				chargeTarget = next;
				chargeStack = stack;
				chargeGroup = group;
				chargeKey = key;
				fadeStart = -1.0;
				beginPlan(data, currentCharge, time);
			} else if (!charging) {
				fadeFrom = chargeAt(time);
				fadeStart = time;
				chargeTarget = next;
				beginPlan(data, currentCharge, time);
			} else {
				chargeTarget = next;
				if (currentCharge > serverCharge) {
					observedStep = currentCharge - serverCharge;
					planTo(plannedAt(time), stepsRemaining(currentCharge, observedStep), time);
				} else if (currentCharge < serverCharge) {
					fadeFrom = chargeAt(time);
					fadeStart = time;
					beginPlan(data, currentCharge, time);
				}
			}
			serverCharge = currentCharge;
		} else if (charging) {
			fadeFrom = chargeAt(time);
			fadeStart = time;
			charging = false;
		}

		if (!charging && chargeTarget != null && fadeAt(time) <= 0.0f) clearCharge();
	}

	private void beginPlan(StatsData data, int currentCharge, double time) {
		charging = true;
		observedStep = 0;
		serverCharge = currentCharge;
		planTo(0.0f, stepsRemaining(currentCharge, masteryStep(data)), time);
	}

	private void planTo(float from, int steps, double time) {
		planFrom = from;
		planStart = time;
		planEnd = time + (double) steps * CHARGE_STEP_TICKS;
	}

	private static int stepsRemaining(int currentCharge, int step) {
		if (currentCharge >= FULL_CHARGE) return 0;
		int safeStep = Math.max(1, step);
		return (FULL_CHARGE - Math.max(0, currentCharge) + safeStep - 1) / safeStep;
	}

	private int masteryStep(StatsData data) {
		Character character = data.getCharacter();
		String name = chargeTarget.getName();
		double mastery = chargeStack
				? data.getStackFormChargeMastery(chargeGroup, name)
				: character.getFormMasteries().getMastery(chargeGroup, name);
		return TransformationsHelper.formChargeStep((int) mastery);
	}

	private void clearCharge() {
		chargeTarget = null;
		chargeKey = null;
		chargeGroup = null;
		charging = false;
		serverCharge = 0;
		observedStep = 0;
		fadeStart = -1.0;
		fadeFrom = 0.0f;
	}

	private float plannedAt(double time) {
		if (!charging) return 0.0f;
		if (planEnd <= planStart) return 1.0f;
		float progress = (float) Mth.clamp((time - planStart) / (planEnd - planStart), 0.0, 1.0);
		return planFrom + (1.0f - planFrom) * progress;
	}

	private float fadeAt(double time) {
		if (fadeStart < 0.0) return 0.0f;
		float progress = (float) Mth.clamp((time - fadeStart) / RELEASE_TICKS, 0.0, 1.0);
		return fadeFrom * (1.0f - progress);
	}

	private float chargeAt(double time) {
		if (chargeTarget == null) return 0.0f;
		return Math.max(plannedAt(time), fadeAt(time));
	}

	private float revertAt(double time) {
		if (revertLook == null) return 0.0f;
		float progress = (float) Mth.clamp((time - revertStart) / revertTicks, 0.0, 1.0);
		return revertFrom * (1.0f - progress);
	}

	private Look chargeLook(Character character) {
		if (chargeStack) {
			return new Look(character.getActiveFormGroup(), character.getActiveForm(), character.getActiveFormData(),
					chargeGroup, chargeTarget.getName(), chargeTarget);
		}
		return new Look(chargeGroup, chargeTarget.getName(), chargeTarget,
				character.getActiveStackFormGroup(), character.getActiveStackForm(), character.getActiveStackFormData());
	}

	private static float[] toFloats(Float[] values) {
		return new float[]{values[0], values[1], values[2]};
	}

	public static float[] lerp(float[] from, float[] to, float factor) {
		if (from == null || to == null || from == to || factor <= 0.0f) return from;
		if (factor >= 1.0f) return to;
		return new float[]{
				Mth.lerp(factor, from[0], to[0]),
				Mth.lerp(factor, from[1], to[1]),
				Mth.lerp(factor, from[2], to[2])
		};
	}
}
