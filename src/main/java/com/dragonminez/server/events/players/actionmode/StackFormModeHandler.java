package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.IActionModeHandler;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class StackFormModeHandler implements IActionModeHandler {
    @Override
    public int handleActionCharge(ServerPlayer player, StatsData data) {
        FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableStackForm(data);
        if (nextForm != null) {
            String group = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();

            String type = ConfigManager.getStackFormGroup(group).getFormType();
            int skillLvl = data.getSkills().getSkillLevel(type);
            return (5 * Math.max(1, skillLvl));
        }
        return 0;
    }

    @Override
    public boolean performAction(ServerPlayer player, StatsData data) {
        attemptTransform(player, data);
        return true;
    }

    private static void attemptTransform(ServerPlayer player, StatsData data) {
        FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableStackForm(data);
        if (nextForm == null) return;

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
            String group = data.getCharacter().hasActiveStackForm() ?
                    data.getCharacter().getActiveStackFormGroup() :
                    data.getCharacter().getSelectedStackFormGroup();

            data.getCharacter().setActiveStackForm(group, nextForm.getName());
            player.refreshDimensions();

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            String translatedFormGroup = I18n.get("race.dragonminez.stack.group." + data.getCharacter().getSelectedStackFormGroup());
            String translatedFormName = I18n.get("race.dragonminez.stack.form." + data.getCharacter().getSelectedStackFormGroup() + "." + nextForm.getName());
            if (data.getCharacter().getActiveForm() != null && !data.getCharacter().getActiveForm().isEmpty()) {
                String translatedStackFormName = I18n.get("race.dragonminez." + data.getCharacter().getRace() + ".form." + data.getCharacter().getActiveFormGroup() + "." + data.getCharacter().getActiveForm());
                translatedFormName = translatedStackFormName + " " + translatedFormGroup + " " + translatedFormName;
            }
            player.sendSystemMessage(Component.translatable("message.dragonminez.transformation", (translatedFormName)), true);
        }
    }
}
