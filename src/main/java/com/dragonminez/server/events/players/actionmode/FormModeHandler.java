package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.IActionModeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

public class FormModeHandler implements IActionModeHandler {
    @Override
    public int handleActionCharge(ServerPlayer player, StatsData data) {
        FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
        if (nextForm != null) {
            String group = data.getCharacter().hasActiveForm() ? data.getCharacter().getActiveFormGroup() : data.getCharacter().getSelectedFormGroup();

            String type = ConfigManager.getFormGroup(data.getCharacter().getRaceName(), group).getFormType();
            int skillLvl = data.getSkills().getSkillLevel(
                    convertSuperformTypes(type)
            );
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
        FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
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
            String group = data.getCharacter().hasActiveForm() ?
                    data.getCharacter().getActiveFormGroup() :
                    data.getCharacter().getSelectedFormGroup();

            if (!data.getCharacter().getFormsUsedBefore().getFormGroup(group).contains(nextForm.getName())) {
                data.getCharacter().getFormsUsedBefore().putForm(group, nextForm.getName());
            }
            data.getCharacter().setActiveForm(group, nextForm.getName());
            player.refreshDimensions();

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

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
        }
    }

    private String convertSuperformTypes(String type) {
        return switch (type) {
            case "super" -> "superform";
            case "god" -> "godform";
            case "legendary" -> "legendaryforms";
            case "android" -> "androidforms";
            default -> type;
        };
    }
}
