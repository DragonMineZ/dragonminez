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

public class FormModeHandler implements IActionModeHandler {
	@Override
	public boolean canCharge(ServerPlayer player, StatsData data) {
		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
		if (nextForm == null) return false;
		if (TransformationsHelper.isOozaruForm(nextForm)) {
			return TransformationsHelper.shouldAutoChargeOozaru(player, data);
		}
		return true;
	}

	@Override
	public int handleActionCharge(ServerPlayer player, StatsData data) {
		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
		if (nextForm != null) {
			String group = data.getCharacter().hasActiveForm() ? data.getCharacter().getActiveFormGroup() : data.getCharacter().getSelectedFormGroup();

			int mastery = (int) data.getCharacter().getFormMasteries().getMastery(group, nextForm.getName());
			return (5 + Math.max(20, mastery));
		}
		return 0;
	}

	@Override
	public boolean performAction(ServerPlayer player, StatsData data) {
		attemptTransform(player, data);
		return true;
	}

	private static void attemptTransform(ServerPlayer player, StatsData data) {
		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
		if (nextForm == null) return;

		if (data.getCharacter().hasActiveStackForm()) {
			FormConfig.FormData activeStackData = data.getCharacter().getActiveStackFormData();

			if (activeStackData != null) {
				boolean isFormStackable = nextForm.getFormStackable();
				boolean isStackStackable = activeStackData.getFormStackable();

				String nextGroup = data.getCharacter().hasActiveForm() ? data.getCharacter().getActiveFormGroup() : data.getCharacter().getSelectedFormGroup();
				double baseMastery = data.getCharacter().getFormMasteries().getMastery(nextGroup, nextForm.getName());
				double stackMastery = data.getCharacter().getStackFormMasteries().getMastery(data.getCharacter().getActiveStackFormGroup(), data.getCharacter().getActiveStackForm());
				boolean meetsStackMastery = baseMastery >= nextForm.getStackOnMastery() && stackMastery >= activeStackData.getStackOnMastery();

				if (!isFormStackable || !isStackStackable || !meetsStackMastery) {
					data.getCharacter().clearActiveStackForm(player);
					player.removeEffect(MainEffects.STACK_TRANSFORMED.get());
					player.sendSystemMessage(Component.translatable("message.dragonminez.form.stack_removed"));
				}
			}
		}

		int energyCost = (int) (data.getMaxEnergy() * 0.1 * nextForm.getEnergyDrain());
		int staminaCost = (int) (data.getMaxStamina() * 0.1 * nextForm.getStaminaDrain());
		int healthCost = (int) (data.getMaxHealth() * 0.1 * nextForm.getHealthDrain());

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
			String group = data.getCharacter().hasActiveForm() ?
					data.getCharacter().getActiveFormGroup() :
					data.getCharacter().getSelectedFormGroup();

			if (!data.getCharacter().getFormsUsedBefore().getFormGroup(group).contains(nextForm.getName())) {
				data.getCharacter().getFormsUsedBefore().putForm(group, nextForm.getName());
			}
			float[] resourceSnapshot = data.snapshotMultiplierResources();
			data.getCharacter().recordPreviousForm();
			data.getCharacter().setActiveForm(group, nextForm.getName());
			data.restoreMultiplierGains(player, resourceSnapshot);
			TransformationItemCostHelper.clearFormDurationSecondsRemaining(player);
			player.refreshDimensions();

			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

			String race = data.getCharacter().getRaceName();
			Component translatedFormName = Component.translatable("race.dragonminez." + race + ".form." + data.getCharacter().getSelectedFormGroup() + "." + nextForm.getName());
			if (data.getCharacter().getActiveStackForm() != null && !data.getCharacter().getActiveStackForm().isEmpty()) {
				Component translatedStackFormGroup = Component.translatable("race.dragonminez.stack.group." + data.getCharacter().getSelectedStackFormGroup());
				Component translatedStackFormName = Component.translatable("race.dragonminez.stack.form." + data.getCharacter().getActiveStackFormGroup() + "." + data.getCharacter().getActiveStackForm());

				translatedFormName = Component.empty()
						.append(translatedFormName)
						.append(Component.literal(" x "))
						.append(translatedStackFormGroup)
						.append(Component.literal(" "))
						.append(translatedStackFormName);
			}

			if (!player.hasEffect(MainEffects.TRANSFORMED.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.TRANSFORMED.get(), -1, 0, false, false, true));
			}
			player.sendSystemMessage(Component.translatable("message.dragonminez.transformation", translatedFormName), true);
			player.refreshDimensions();
		}
	}
}