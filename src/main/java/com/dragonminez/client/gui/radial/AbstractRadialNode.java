package com.dragonminez.client.gui.radial;

import com.dragonminez.Reference;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.Locale;

public abstract class AbstractRadialNode implements RadialNode {

	public static final ResourceLocation PLACEHOLDER = icon("radial_placeholder");
	public static final int GREEN = 0x2BFF00;
	public static final int RED = 0xFF1B00;

	public float animScale = 0.0f;
	public float animExpand = 0.0f;
	public float animHighlight = 0.0f;

	private List<RadialNode> cachedChildren;

	@Override
	public List<RadialNode> children(StatsData stats) {
		if (cachedChildren == null) cachedChildren = buildChildren(stats);
		return cachedChildren;
	}

	protected List<RadialNode> buildChildren(StatsData stats) {
		return List.of();
	}

	public void invalidate() {
		cachedChildren = null;
	}

	protected static ResourceLocation icon(String name) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/icons/" + name + ".png");
	}

	protected static ResourceLocation iconForFormType(String type) {
		if (type == null) return PLACEHOLDER;
		String t = type.toLowerCase(Locale.ROOT);
		if (t.contains("legendary")) return icon("legendaryforms");
		if (t.contains("super")) return icon("superforms");
		if (t.contains("god")) return icon("godforms");
		if (t.contains("android")) return icon("androidforms");
		if (t.contains("kaioken")) return icon("kaioken");
		if (t.contains("ultimate")) return icon("ultimate");
		return PLACEHOLDER;
	}

	protected static int tintOf(FormConfig.FormData formData) {
		if (formData == null) return -1;
		float[] rgb = formData.getRgbAuraColor();
		if (rgb == null || rgb.length < 3) return -1;
		int r = Math.round(rgb[0] * 255.0f);
		int g = Math.round(rgb[1] * 255.0f);
		int b = Math.round(rgb[2] * 255.0f);
		return (r << 16) | (g << 8) | b;
	}

	protected void playToggle(boolean turnedOn) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		mc.player.playSound(turnedOn ? MainSounds.SWITCH_ON.get() : MainSounds.SWITCH_OFF.get(), 1.0F, 1.0F);
	}

	protected void playClick() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.0F));
	}
}
