package com.dragonminez.client.gui.radial.nodes;

import com.dragonminez.client.gui.radial.RadialNode;
import com.dragonminez.common.stats.StatsData;

import java.util.ArrayList;
import java.util.List;

public class FormGroupHeadNode extends FormSelectNode {

	private final String group;
	private final boolean stack;
	private final List<RadialNode> rest;

	public FormGroupHeadNode(String race, String group, String firstForm, boolean stack, List<RadialNode> rest) {
		super(race, group, firstForm, stack);
		this.group = group;
		this.stack = stack;
		this.rest = rest != null ? rest : new ArrayList<>();
	}

	@Override
	protected List<RadialNode> buildChildren(StatsData stats) {
		return rest;
	}

	@Override
	public String orderKey() {
		return "group:" + (stack ? "stack:" : "") + group;
	}
}
