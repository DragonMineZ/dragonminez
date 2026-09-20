package com.dragonminez.client.gui.tutorial;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralUserConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class TutorialManager {
	public static final String RACE_SELECTION = "race_selection";
	public static final String CUSTOMIZATION_CLASS = "customization_class";
	public static final String HUD = "hud";
	public static final String STATS_MENU = "stats_menu";
	public static final String SKILLS_GENERAL = "skills_general";
	public static final String SKILLS_TECHNIQUES = "skills_techniques";
	public static final String SKILLS_FORMS = "skills_forms";
	public static final String QUEST_DIFFICULTY = "quest_difficulty";
	public static final String QUEST_MENU = "quest_menu";

	private static final int KEY_ESCAPE = 256;
	private static final int KEY_ENTER = 257;
	private static final int KEY_KEYPAD_ENTER = 335;
	private static final int KEY_SPACE = 32;

	private static Screen host;
	private static String activeId;
	private static List<TutorialStep> steps;
	private static int index;
	private static Runnable onEnd;
	private static boolean hoverBlocked;

	private TutorialManager() {}

	public static boolean isEnabled() {
		return ConfigManager.getUserConfig().getTutorialsEnabled();
	}

	public static boolean hasSeen(String id) {
		return ConfigManager.getUserConfig().getTutorialsSeen().contains(id);
	}

	public static boolean shouldRun(String id) {
		return isEnabled() && !hasSeen(id);
	}

	public static void markSeen(String id) {
		GeneralUserConfig config = ConfigManager.getUserConfig();
		if (config.getTutorialsSeen().contains(id)) return;
		config.getTutorialsSeen().add(id);
		ConfigManager.saveGeneralUserConfig();
	}

	public static void setEnabled(boolean enabled) {
		ConfigManager.getUserConfig().setTutorialsEnabled(enabled);
		ConfigManager.saveGeneralUserConfig();
		if (!enabled) stop();
	}

	public static void resetSeen() {
		ConfigManager.getUserConfig().getTutorialsSeen().clear();
		ConfigManager.saveGeneralUserConfig();
	}

	public static boolean request(Screen screen, String id, List<TutorialStep> tutorialSteps) {
		return request(screen, id, tutorialSteps, null);
	}

	public static boolean request(Screen screen, String id, List<TutorialStep> tutorialSteps, Runnable endCallback) {
		if (screen == null || tutorialSteps == null || tutorialSteps.isEmpty()) return false;
		if (!shouldRun(id)) return false;
		if (host == screen) return id.equals(activeId);
		if (host != null) stop();

		int first = nextApplicable(tutorialSteps, 0);
		if (first < 0) return false;

		host = screen;
		activeId = id;
		steps = tutorialSteps;
		index = first;
		onEnd = endCallback;
		TutorialOverlay.reset();
		steps.get(index).enter();
		return true;
	}

	public static boolean isActive() {
		return host != null;
	}

	public static boolean isActive(Screen screen) {
		return host != null && host == screen;
	}

	public static boolean isActive(String id) {
		return host != null && id.equals(activeId);
	}

	public static boolean isHoverBlocked(Screen screen) {
		return hoverBlocked && host != null && host == screen;
	}

	public static void stop() {
		if (host == null) return;
		TutorialStep step = currentStep();
		clear();
		if (step != null) step.exit();
	}

	static TutorialStep currentStep() {
		if (steps == null || index < 0 || index >= steps.size()) return null;
		return steps.get(index);
	}

	static int position() {
		int position = 0;
		for (int i = 0; i <= index && i < steps.size(); i++) if (steps.get(i).applies() || i == index) position++;
		return position;
	}

	static int total() {
		int total = 0;
		for (int i = 0; i < steps.size(); i++) if (steps.get(i).applies() || i == index) total++;
		return total;
	}

	static boolean isLastStep() {
		return nextApplicable(steps, index + 1) < 0;
	}

	static void advance() {
		TutorialStep step = currentStep();
		if (step == null) return;
		int next = nextApplicable(steps, index + 1);
		if (next < 0) {
			finish();
			return;
		}
		step.exit();
		if (host == null) return;
		index = next;
		TutorialOverlay.onStepChanged();
		steps.get(index).enter();
	}

	static void finish() {
		if (host == null) return;
		String id = activeId;
		TutorialStep step = currentStep();
		Runnable callback = onEnd;
		clear();
		if (step != null) step.exit();
		markSeen(id);
		if (callback != null) callback.run();
	}

	static void press(TutorialButton button) {
		if (button.action() != null) button.action().run();
		if (host == null) return;
		switch (button.behavior()) {
			case NEXT -> advance();
			case FINISH -> finish();
			default -> {}
		}
	}

	private static void clear() {
		host = null;
		activeId = null;
		steps = null;
		index = 0;
		onEnd = null;
		hoverBlocked = false;
		TutorialOverlay.reset();
	}

	private static int nextApplicable(List<TutorialStep> list, int from) {
		for (int i = Math.max(0, from); i < list.size(); i++) if (list.get(i).applies()) return i;
		return -1;
	}

	private static float scaleOf(Screen screen) {
		return screen instanceof TutorialHost tutorialHost ? Math.max(0.05f, tutorialHost.tutorialScale()) : 1.0f;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRenderPre(ScreenEvent.Render.Pre event) {
		if (host == null || event.getScreen() != host) {
			hoverBlocked = false;
			return;
		}
		float scale = scaleOf(host);
		hoverBlocked = !TutorialOverlay.insideHighlight(event.getMouseX() / scale, event.getMouseY() / scale);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderPost(ScreenEvent.Render.Post event) {
		if (host == null || event.getScreen() != host) return;
		TutorialStep step = currentStep();
		if (step == null) return;
		if (!step.applies()) {
			advance();
			step = currentStep();
			if (step == null) return;
		}

		float scale = scaleOf(host);
		int width = host instanceof TutorialHost tutorialHost ? tutorialHost.tutorialWidth() : host.width;
		int height = host instanceof TutorialHost tutorialHost ? tutorialHost.tutorialHeight() : host.height;
		TutorialOverlay.render(event.getGuiGraphics(), step, scale, width, height, event.getMouseX() / scale, event.getMouseY() / scale);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
		if (host == null || event.getScreen() != host) return;
		TutorialStep step = currentStep();
		if (step == null) return;

		float scale = scaleOf(host);
		double mouseX = event.getMouseX() / scale;
		double mouseY = event.getMouseY() / scale;

		if (TutorialOverlay.mouseClicked(mouseX, mouseY, event.getButton())) {
			event.setCanceled(true);
			return;
		}
		if (step.passthrough() && TutorialOverlay.insideHighlight(mouseX, mouseY)) return;
		event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
		if (blocksPointer(event.getScreen(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
		if (blocksPointer(event.getScreen(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
		if (blocksPointer(event.getScreen(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
	}

	private static boolean blocksPointer(Screen screen, double rawMouseX, double rawMouseY) {
		if (host == null || screen != host) return false;
		TutorialStep step = currentStep();
		if (step == null) return false;
		float scale = scaleOf(host);
		return !(step.passthrough() && TutorialOverlay.insideHighlight(rawMouseX / scale, rawMouseY / scale));
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
		if (host == null || event.getScreen() != host) return;
		TutorialStep step = currentStep();
		if (step == null) return;
		event.setCanceled(true);

		int key = event.getKeyCode();
		if (key == KEY_ESCAPE) {
			if (step.skippable()) {
				TutorialOverlay.playClick();
				finish();
			}
		} else if (key == KEY_ENTER || key == KEY_KEYPAD_ENTER || key == KEY_SPACE) {
			TutorialOverlay.playClick();
			advance();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
		if (host != null && event.getScreen() == host) event.setCanceled(true);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
		if (host != null && event.getScreen() == host) event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || host == null) return;
		if (Minecraft.getInstance().screen != host) stop();
	}
}
