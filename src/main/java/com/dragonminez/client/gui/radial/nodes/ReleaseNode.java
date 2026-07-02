package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.network.C2S.SetReleaseLimitC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ReleaseNode extends AbstractRadialNode {

	private static int potentialUnlockLevel(StatsData stats) {
		return stats.getSkills().hasSkill("potentialunlock") ? stats.getSkills().getSkillLevel("potentialunlock") : 0;
	}

	public static int maxRelease(StatsData stats) {
		return 50 + potentialUnlockLevel(stats) * 5;
	}

	private int currentLimit(StatsData stats) {
		return stats.getResources().getReleaseLimit();
	}

	@Override
	public Component label(StatsData stats) {
		return Component.translatable("gui.action.dragonminez.release");
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return null;
	}

	@Override
	public String faceText(StatsData stats) {
		int limit = currentLimit(stats);
		int value = limit > 0 ? limit : maxRelease(stats);
		return value + "%";
	}

	@Override
	public boolean visible(StatsData stats) {
		if (stats.getCharacter() == null) return false;
		String race = stats.getCharacter().getRaceName();
		return race != null && !race.isEmpty();
	}

	@Override
	public boolean active(StatsData stats) {
		return currentLimit(stats) > 0;
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : 0xFFFFFF;
	}

	@Override
	public boolean expandable(StatsData stats) {
		return false;
	}

	@Override
	public void onSelect(StatsData stats) {
		NetworkHandler.sendToServer(new SetReleaseLimitC2S(0));
		playToggle(false);
	}

	public List<RadialNode> buildOptions(StatsData stats) {
		List<RadialNode> out = new ArrayList<>();
		for (int value = maxRelease(stats); value >= 5; value -= 5) {
			out.add(new ReleaseLimitOptionNode(value));
		}
		return out;
	}
}
