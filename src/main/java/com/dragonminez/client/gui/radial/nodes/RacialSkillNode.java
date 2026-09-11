package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.ExecuteActionC2S;
import com.dragonminez.common.network.C2S.SwitchActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.racial.impl.BioAndroidEvolution;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class RacialSkillNode extends AbstractRadialNode {

	private String lastChildSignature = null;

	private boolean isTailRace(StatsData stats) {
		String race = stats.getCharacter().getRaceName();
		String form = stats.getCharacter().getActiveForm();
		return "saiyan".equals(race) || stats.getCharacter().isHasSaiyanTail() || (form != null && form.contains("oozaru"));
	}

	private String racialSkill(StatsData stats) {
		String race = stats.getCharacter().getRaceName();
		return ConfigManager.getRaceCharacter(race) == null ? "" : ConfigManager.getRaceCharacter(race).getRacialSkill();
	}

	private boolean isActionRacial(StatsData stats) {
		return RacialRegistry.get(racialSkill(stats)).map(a -> a.hasActiveAction(stats)).orElse(false);
	}

	@Override
	public Component label(StatsData stats) {
		if (isActionRacial(stats)) {
			String skill = racialSkill(stats);
			if ("bioandroid".equals(skill)) {
				String tier = BioAndroidEvolution.resolveTier(stats);
				if (tier.equals("semi") && RacialData.BIO_SKILL_EXPLODE.equals(stats.getRacialData().getBioSelectedSkill())) {
					return Component.translatable("gui.action.dragonminez.racial.bioandroid.explode");
				}
				return Component.translatable("gui.action.dragonminez.racial.bioandroid." + tier);
			}
			return Component.translatable("gui.action.dragonminez.racial." + skill);
		}
		if (isTailRace(stats)) return Component.translatable("gui.action.dragonminez.tail");
		return Component.empty();
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon("racial");
	}

	@Override
	public boolean visible(StatsData stats) {
		if (stats.getCharacter() == null) return false;
		String race = stats.getCharacter().getRaceName();
		if (race == null || race.isEmpty()) return false;
		return isTailRace(stats) || isActionRacial(stats);
	}

	@Override
	public boolean active(StatsData stats) {
		if (isActionRacial(stats)) return stats.getStatus().getSelectedAction() == ActionMode.RACIAL;
		if (isTailRace(stats)) return stats.getStatus().isTailVisible();
		return false;
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : RED;
	}

	@Override
	public void onSelect(StatsData stats) {
		if (isActionRacial(stats)) {
			boolean wasActive = stats.getStatus().getSelectedAction() == ActionMode.RACIAL;
			NetworkHandler.sendToServer(new SwitchActionC2S(ActionMode.RACIAL));
			playToggle(!wasActive);
		} else if (isTailRace(stats)) {
			boolean wasActive = stats.getStatus().isTailVisible();
			NetworkHandler.sendToServer(new ExecuteActionC2S(ExecuteActionC2S.ActionType.TOGGLE_TAIL));
			playToggle(!wasActive);
		}
	}

	/**
	 * Rebuilds the children whenever what they should contain changes — the Majin pool losing a slot
	 * to an ejection, or a BioAndroid changing tier — so the expanded ring updates in place instead
	 * of waiting for the menu to be reopened. An unchanged signature keeps the same node instances,
	 * which is what preserves each Majin slot's pending eject-confirm window mid-interaction.
	 */
	@Override
	public List<RadialNode> children(StatsData stats) {
		String skill = racialSkill(stats);
		String signature = skill + "|" + BioAndroidEvolution.resolveTier(stats) + "|"
				+ ("majin".equals(skill) ? stats.getRacialData().getAbsorptions().size() : 0);
		if (!signature.equals(lastChildSignature)) {
			lastChildSignature = signature;
			invalidate();
		}
		return super.children(stats);
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		String skill = racialSkill(stats);
		if ("bioandroid".equals(skill) && BioAndroidEvolution.resolveTier(stats).equals("semi")) {
			return List.of(new BioSkillChoiceNode(false), new BioSkillChoiceNode(true));
		}
		if (!"majin".equals(skill)) return List.of();

		List<RacialData.AbsorptionSlot> absorptions = stats.getRacialData().getAbsorptions();
		List<RadialNode> out = new ArrayList<>();
		for (int i = 0; i < absorptions.size(); i++) {
			out.add(new AbsorptionEjectNode(i, absorptions.get(i)));
		}
		return out;
	}
}
