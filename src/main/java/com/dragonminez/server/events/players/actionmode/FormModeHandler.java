package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.IActionModeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class FormModeHandler implements IActionModeHandler {
    @Override
    public int handleActionCharge(ServerPlayer player, StatsData data) {
        FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableForm(data);
        if (nextForm != null) {
            String group = data.getCharacter().hasActiveForm() ? data.getCharacter().getActiveFormGroup() : data.getCharacter().getSelectedFormGroup();

            String type = ConfigManager.getFormGroup(data.getCharacter().getRaceName(), group).getFormType();
            int skillLvl = switch (type) {
                case "super" -> data.getSkills().getSkillLevel("superform");
                case "god" -> data.getSkills().getSkillLevel("godform");
                case "legendary" -> data.getSkills().getSkillLevel("legendaryforms");
                case "android" -> data.getSkills().getSkillLevel("androidforms");
                default -> 1;
            };
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

        int cost = (int) (data.getMaxEnergy() * nextForm.getEnergyDrain());
        if (data.getResources().getCurrentEnergy() >= cost) {
            data.getResources().removeEnergy(cost);

            String group = data.getCharacter().hasActiveForm() ?
                    data.getCharacter().getActiveFormGroup() :
                    data.getCharacter().getSelectedFormGroup();

            data.getCharacter().setActiveForm(group, nextForm.getName());
            player.refreshDimensions();

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            player.displayClientMessage(Component.translatable("message.dragonminez.form.no_ki", cost), true);
        }
    }
}
