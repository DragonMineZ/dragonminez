package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.AbstractRadialNode;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SkillToggleNode extends AbstractRadialNode {

	private final Component label;
	private final ResourceLocation icon;
	private final Predicate<StatsData> has;
	private final Predicate<StatsData> active;
	private final BiConsumer<StatsData, Boolean> toggle;

	public SkillToggleNode(Component label, ResourceLocation icon, Predicate<StatsData> has, Predicate<StatsData> active, BiConsumer<StatsData, Boolean> toggle) {
		this.label = label;
		this.icon = icon;
		this.has = has;
		this.active = active;
		this.toggle = toggle;
	}

	@Override
	public Component label(StatsData stats) {
		return label;
	}

	@Override
	public ResourceLocation icon(StatsData stats) {
		return icon;
	}

	@Override
	public boolean visible(StatsData stats) {
		return has.test(stats);
	}

	@Override
	public boolean active(StatsData stats) {
		return active.test(stats);
	}

	@Override
	public int labelColor(StatsData stats) {
		return active(stats) ? GREEN : RED;
	}

	@Override
	public void onSelect(StatsData stats) {
		boolean wasActive = active.test(stats);
		toggle.accept(stats, wasActive);
		playToggle(!wasActive);
	}
}
