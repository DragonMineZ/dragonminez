package com.dragonminez.client.gui.tutorial;

import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.config.HudEditorScreen;
import com.dragonminez.client.gui.hud.HudStyle;
import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HudTutorialScreen extends ScaledScreen {
	private static final int SYNC_TIMEOUT_TICKS = 200;
	private static final int SETTLE_TICKS = 12;

	private int waitedTicks;
	private int readyTicks;
	private boolean started;

	public HudTutorialScreen() {
		super(Component.empty());
	}

	@Override
	public void tick() {
		super.tick();
		if (this.minecraft == null || this.minecraft.player == null) return;

		if (started) {
			if (!TutorialManager.isActive(this)) close();
			return;
		}

		boolean created = StatsProvider.get(StatsCapability.INSTANCE, this.minecraft.player)
				.map(data -> data.getStatus().isHasCreatedCharacter()).orElse(false);
		readyTicks = created ? readyTicks + 1 : 0;

		if (readyTicks >= SETTLE_TICKS) {
			started = TutorialManager.request(this, TutorialManager.HUD, steps(), this::close);
			if (!started) close();
		} else if (++waitedTicks > SYNC_TIMEOUT_TICKS) {
			close();
		}
	}

	private List<TutorialStep> steps() {
		List<TutorialStep> steps = new ArrayList<>();
		steps.add(TutorialStep.of("gui.dragonminez.tutorial.hud.main")
				.title("gui.dragonminez.tutorial.hud.title")
				.highlight(this::mainHudRects)
				.padding(4.0f)
				.build());
		steps.add(TutorialStep.of("gui.dragonminez.tutorial.hud.edit")
				.title("gui.dragonminez.tutorial.hud.edit.title")
				.highlight(this::mainHudRects)
				.padding(4.0f)
				.noSkip()
				.button(TutorialButton.text("gui.dragonminez.tutorial.hud.edit.later", TutorialButton.Behavior.FINISH, null))
				.button(TutorialButton.text("gui.dragonminez.tutorial.hud.edit.open", TutorialButton.Behavior.FINISH,
						() -> this.minecraft.setScreen(new HudEditorScreen(null))))
				.build());
		return steps;
	}

	private List<TutorialRect> mainHudRects() {
		List<TutorialRect> rects = new ArrayList<>();
		if (this.minecraft == null) return rects;
		Window window = this.minecraft.getWindow();
		int guiWidth = window.getGuiScaledWidth();
		int guiHeight = window.getGuiScaledHeight();
		float scale = getUiScale();

		for (HudElement element : HudLayout.elements(HudStyle.current())) {
			if (element.isMeter() || element == HudElement.PARTY || element.isSkill()) continue;
			HudLayout.Box box = HudLayout.resolve(element, guiWidth, guiHeight);
			if (!box.visible()) continue;
			rects.add(TutorialRect.of(box.x() / scale, box.y() / scale, box.width() / scale, box.height() / scale));
		}
		return rects;
	}

	private void close() {
		if (this.minecraft != null && this.minecraft.screen == this) this.minecraft.setScreen(null);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
