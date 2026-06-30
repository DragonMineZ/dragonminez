package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationItemCostHelper;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.IActionModeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

public class StackFormModeHandler implements IActionModeHandler {
	@Override
	public boolean canCharge(ServerPlayer player, StatsData data) {
		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableStackForm(data);
		if (nextForm == null) return false;

		if (data.getCharacter().hasActiveForm()) {
			FormConfig.FormData activeFormData = data.getCharacter().getActiveFormData();
			if (activeFormData != null) {
				if (!activeFormData.getFormStackable() || !nextForm.getFormStackable()) return false;
				String stackGroup = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();
				if (!TransformationsHelper.areFormsCompatible(activeFormData, data.getCharacter().getActiveFormGroup(), nextForm, stackGroup)) return false;
			}
		}
		return true;
	}

	@Override
	public int handleActionCharge(ServerPlayer player, StatsData data) {
		if (!canCharge(player, data)) return 0;

		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableStackForm(data);
		String group = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();

		int mastery = (int) data.getCharacter().getStackFormMasteries().getMastery(group, nextForm.getName());
		return 5 + Math.min(20, (int)(mastery * 0.2));
	}

	@Override
	public boolean performAction(ServerPlayer player, StatsData data) {
		attemptTransform(player, data);
		return true;
	}

	private static void attemptTransform(ServerPlayer player, StatsData data) {
		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableStackForm(data);
		if (nextForm == null) return;

		if (data.getCharacter().hasActiveForm()) {
			FormConfig.FormData activeFormData = data.getCharacter().getActiveFormData();
			if (activeFormData != null) {
				if (!activeFormData.getFormStackable() || !nextForm.getFormStackable()) {
					player.displayClientMessage(Component.translatable("message.dragonminez.form.not_stackable"), true);
					return;
				}

				String stackGroup = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();
				if (!TransformationsHelper.areFormsCompatible(activeFormData, data.getCharacter().getActiveFormGroup(), nextForm, stackGroup)) {
					player.displayClientMessage(Component.translatable("message.dragonminez.form.not_stackable"), true);
					return;
				}

				String nextGroup = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();
				double baseMastery = data.getCharacter().getFormMasteries().getMastery(data.getCharacter().getActiveFormGroup(), data.getCharacter().getActiveForm());
				double stackMastery = data.getCharacter().getStackFormMasteries().getMastery(nextGroup, nextForm.getName());
				if (baseMastery < activeFormData.getStackOnMastery() || stackMastery < nextForm.getStackOnMastery()) {
					player.displayClientMessage(Component.translatable("message.dragonminez.form.not_stackable"), true);
					return;
				}
			}
		}

		int energyCost = (int) (data.getMaxEnergy() * nextForm.getEnergyDrain());
		int staminaCost = (int) (data.getMaxStamina() * nextForm.getStaminaDrain());
		int healthCost = (int) (data.getMaxHealth() * nextForm.getHealthDrain());

		boolean hasEnoughEnergy = data.getResources().getCurrentEnergy() >= energyCost;
		boolean hasEnoughStamina = data.getResources().getCurrentStamina() >= staminaCost;
		boolean hasEnoughHealth = data.getPlayer().getHealth() >= healthCost;

		if (!hasEnoughEnergy) {
			player.displayClientMessage(Component.translatable("message.dragonminez.form.no_ki", energyCost), true);
		}

		if (!hasEnoughStamina) {
			player.displayClientMessage(Component.translatable("message.dragonminez.form.no_stamina", staminaCost), true);
		}

		if (!hasEnoughHealth) {
			player.displayClientMessage(Component.translatable("message.dragonminez.form.no_health", healthCost), true);
		}

		if (hasEnoughEnergy && hasEnoughStamina && hasEnoughHealth) {
			if (!TransformationItemCostHelper.canAffordAndHandleTriggerCost(player, nextForm)) {
				player.displayClientMessage(Component.translatable("message.dragonminez.form.no_trigger_item"), true);
				return;
			}
			String group = data.getCharacter().hasActiveStackForm() ?
					data.getCharacter().getActiveStackFormGroup() :
					data.getCharacter().getSelectedStackFormGroup();

			if (!data.getCharacter().getStackFormsUsedBefore().getFormGroup(group).contains(nextForm.getName())) {
				data.getCharacter().getStackFormsUsedBefore().putForm(group, nextForm.getName());
			}
			float[] resourceSnapshot = data.snapshotMultiplierResources();
			data.getCharacter().recordPreviousStackForm();
			data.getCharacter().setActiveStackForm(group, nextForm.getName());
			data.restoreMultiplierGains(player, resourceSnapshot);
			TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(player);
			player.refreshDimensions();

			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

			Component translatedStackFormGroup = Component.translatable("race.dragonminez.stack.group." + data.getCharacter().getSelectedStackFormGroup());
			Component translatedStackFormName = Component.translatable("race.dragonminez.stack.form." + data.getCharacter().getSelectedStackFormGroup() + "." + nextForm.getName());
			Component fullFormName;

			if (data.getCharacter().getActiveForm() != null && !data.getCharacter().getActiveForm().isEmpty()) {
				Component translatedFormName = Component.translatable("race.dragonminez." + data.getCharacter().getRace() + ".form." + data.getCharacter().getActiveFormGroup() + "." + data.getCharacter().getActiveForm());
				fullFormName = Component.empty()
						.append(translatedFormName)
						.append(Component.literal(" x "))
						.append(translatedStackFormGroup)
						.append(Component.literal(" "))
						.append(translatedStackFormName);
			} else {
				fullFormName = Component.empty()
						.append(translatedStackFormGroup)
						.append(Component.literal(" "))
						.append(translatedStackFormName);
			}

			player.sendSystemMessage(Component.translatable("message.dragonminez.transformation", fullFormName), true);

			if (!player.hasEffect(MainEffects.STACK_TRANSFORMED.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.STACK_TRANSFORMED.get(), -1, 0, false, false, true));
			}
			player.refreshDimensions();
		}
	}
}