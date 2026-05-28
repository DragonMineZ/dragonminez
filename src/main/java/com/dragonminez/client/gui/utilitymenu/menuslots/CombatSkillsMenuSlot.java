package com.dragonminez.client.gui.utilitymenu.menuslots;

import com.dragonminez.client.gui.utilitymenu.AbstractMenuSlot;
import com.dragonminez.client.gui.utilitymenu.ButtonInfo;
import com.dragonminez.client.gui.utilitymenu.IUtilityMenuSlot;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class CombatSkillsMenuSlot extends AbstractMenuSlot implements IUtilityMenuSlot {

	private record MenuFunction(String id, Predicate<StatsData> hasFunction, Predicate<StatsData> isActive, BiConsumer<StatsData, Boolean> onToggle) {}

	private static final List<MenuFunction> FUNCTIONS = new ArrayList<>();
	private int selectedIndex = 0;

	static {
		FUNCTIONS.add(new MenuFunction(
				"kiprotection",
				stats -> stats.getSkills().hasSkill("kiprotection"),
				stats -> stats.getSkills().isSkillActive("kiprotection"),
				(stats, wasActive) -> NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "kiprotection", 0))
		));

		FUNCTIONS.add(new MenuFunction(
				"ki_infusion",
				stats -> stats.getSkills().hasSkill("ki_infusion"),
				stats -> stats.getSkills().isSkillActive("ki_infusion"),
				(stats, wasActive) -> NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.TOGGLE, "ki_infusion", 0))
		));
	}

	@Override
	public ButtonInfo render(StatsData statsData) {
		if (selectedIndex >= FUNCTIONS.size()) selectedIndex = 0;

		MenuFunction current = FUNCTIONS.get(selectedIndex);
		if (!current.hasFunction.test(statsData)) {
			int attempts = 0;
			while (!current.hasFunction.test(statsData) && attempts < FUNCTIONS.size()) {
				selectedIndex = (selectedIndex + 1) % FUNCTIONS.size();
				current = FUNCTIONS.get(selectedIndex);
				attempts++;
			}
		}

		if (current.hasFunction.test(statsData)) {
			boolean active = current.isActive.test(statsData);
			return new ButtonInfo(
					Component.translatable("skill.dragonminez." + current.id).withStyle(ChatFormatting.BOLD),
					Component.translatable("gui.action.dragonminez." + (active ? "true" : "false")),
					active
			);
		}

		return new ButtonInfo();
	}

	@Override
	public void handle(StatsData statsData, boolean rightClick) {
		if (rightClick) {
			int nextIndex = selectedIndex;
			boolean found = false;

			for (int i = 0; i < FUNCTIONS.size(); i++) {
				nextIndex = (nextIndex + 1) % FUNCTIONS.size();
				if (FUNCTIONS.get(nextIndex).hasFunction.test(statsData)) {
					selectedIndex = nextIndex;
					found = true;
					break;
				}
			}

			if (found) playUiSound(SoundEvents.UI_BUTTON_CLICK.get());

		} else {
			MenuFunction current = FUNCTIONS.get(selectedIndex);
			if (current.hasFunction.test(statsData)) {
				boolean wasActive = current.isActive.test(statsData);
				current.onToggle.accept(statsData, wasActive);
				playToggleSound(wasActive);
			}
		}
	}

	private void playUiSound(net.minecraft.sounds.SoundEvent sound) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
	}

	@Override
	public boolean hasRightClickAction(StatsData statsData) {
		return statsData.getSkills().hasSkill("kiprotection") && statsData.getSkills().hasSkill("ki_infusion");
	}
}