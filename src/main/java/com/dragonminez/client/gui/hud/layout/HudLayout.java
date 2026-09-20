package com.dragonminez.client.gui.hud.layout;

import com.dragonminez.client.gui.hud.HudSprites;
import com.dragonminez.client.gui.hud.HudStyle;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.HudPlacement;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudLayout {
	public static final float MIN_SCALE = 0.5f;
	public static final float MAX_SCALE = 5.0f;
	public static final float HOTBAR_HALF_WIDTH = 91.0f;
	public static final float HOTBAR_HEIGHT = 22.0f;
	public static final float REFERENCE_HEIGHT = 360.0f;
	private static final float DOCK_DISTANCE = 8.0f;
	private static final float LINK_DISTANCE = 8.0f;
	private static final float MAX_SCREEN_SHARE = 0.95f;

	private static final float BORDER = HudSprites.DEFAULT_HP.border();
	private static final float[] SIZE_DEFAULT_MAIN = {
			BORDER + HudSprites.DEFAULT_PORTRAIT_BACK.guiWidth() - BORDER + HudSprites.DEFAULT_HP.width() + BORDER,
			HudSprites.DEFAULT_PORTRAIT_BACK.guiHeight() + HudSprites.DEFAULT_RELEASE.height() + BORDER + 7.0f};
	private static final float[] SIZE_LEGACY_1 = {173.0f, 38.0f};
	private static final float[] SIZE_LEGACY_2_BAR = {83.0f, 9.0f};
	private static final float[] SIZE_LEGACY_2_KI = {125.0f, 40.0f};
	private static final float[] SIZE_MINECRAFT_SIDE = {
			HudSprites.MINECRAFT_CAPSULE.guiWidth() + HudSprites.MINECRAFT_HP.width() + BORDER,
			HudSprites.MINECRAFT_HP.height() * 2.0f + BORDER * 3.0f};
	private static final float[] SIZE_PARTY = {
			HudSprites.PARTY_HEAD_BACK.guiWidth() + HudSprites.PARTY_HP.width() + BORDER + 22.0f,
			HudSprites.PARTY_HEAD_BACK.guiHeight() * 3.0f + 4.0f * 2.0f + 1.0f};
	private static final float[] SIZE_METER = {
			HudSprites.RESERVE_CAPSULE.guiWidth(),
			HudSprites.RESERVE_CAPSULE.guiHeight() + HudSprites.RESERVE_BAR.height() + BORDER + 7.0f};

	private static final float[] SIZE_SKILL = {120.0f, 13.0f};
	private static final float[] SIZE_TRACKED_QUEST = {180.0f, 56.0f};
	private static final float[] SIZE_QUEST_NOTICE = {220.0f, 52.0f};
	private static final float[] SIZE_SCOUTER = {70.0f, 41.0f};
	private static final float[] SIZE_BABA_TIMER = {145.0f, 40.0f};
	private static final String EXTRAS_KEY = "extras";
	private static final float SKILL_SCALE = 1.25f;
	private static final float SKILL_LADDER_STEP = 8.0f;
	private static final float SKILL_MARGIN_X = 12.0f;
	private static final float SKILL_MARGIN_BOTTOM = 66.0f;

	private static final Map<HudElement, Float> METER_VISIBILITY = new EnumMap<>(HudElement.class);
	private static boolean preview;

	private HudLayout() {}

	public record Box(float x, float y, float width, float height, float scale, boolean visible, boolean hotbar, boolean mirrored) {
		public float right() { return x + width; }
		public float bottom() { return y + height; }
		public float centerX() { return x + width / 2.0f; }
		public float centerY() { return y + height / 2.0f; }

		public boolean contains(double px, double py) {
			return px >= x && px < right() && py >= y && py < bottom();
		}

		public Box moved(float dx, float dy) {
			return new Box(x + dx, y + dy, width, height, scale, visible, hotbar, mirrored);
		}
	}

	public static void setPreview(boolean active) {
		preview = active;
	}

	public static boolean isPreview() {
		return preview;
	}

	public static void setMeterVisibility(HudElement element, float visibility) {
		METER_VISIBILITY.put(element, Mth.clamp(visibility, 0.0f, 1.0f));
	}

	public static List<HudElement> elements(HudStyle style) {
		List<HudElement> list = new ArrayList<>();
		if (style == HudStyle.MINECRAFT) {
			list.add(HudElement.MC_LEFT);
			list.add(HudElement.MC_RIGHT);
		} else if (style == HudStyle.LEGACY_2) {
			list.add(HudElement.L2_HEALTH);
			list.add(HudElement.L2_KI);
			list.add(HudElement.L2_STAMINA);
		} else {
			list.add(HudElement.MAIN);
		}
		list.add(HudElement.RESERVE);
		list.add(HudElement.RAGE);
		list.add(HudElement.PARTY);
		for (HudElement skill : HudElement.skills()) list.add(skill);
		return list;
	}

	public static List<HudElement> extraElements(HudStyle style) {
		List<HudElement> list = new ArrayList<>();
		list.add(HudElement.TRACKED_QUEST);
		list.add(HudElement.QUEST_NOTICE);
		if (style != HudStyle.LEGACY_2) list.add(HudElement.SCOUTER);
		list.add(HudElement.BABA_TIMER);
		return list;
	}

	private static String layoutKey(HudStyle style, HudElement element) {
		return element.isExtra() ? EXTRAS_KEY : style.configName();
	}

	public static float anchorY(HudElement element) {
		return placement(HudStyle.current(), element).getAnchorY();
	}

	public static float[] baseSize(HudStyle style, HudElement element) {
		return switch (element) {
			case MAIN -> style == HudStyle.LEGACY_1 ? SIZE_LEGACY_1 : SIZE_DEFAULT_MAIN;
			case L2_HEALTH, L2_STAMINA -> SIZE_LEGACY_2_BAR;
			case L2_KI -> SIZE_LEGACY_2_KI;
			case MC_LEFT, MC_RIGHT -> SIZE_MINECRAFT_SIDE;
			case PARTY -> SIZE_PARTY;
			case RESERVE, RAGE -> SIZE_METER;
			case SKILL_1, SKILL_2, SKILL_3, SKILL_4 -> SIZE_SKILL;
			case TRACKED_QUEST -> SIZE_TRACKED_QUEST;
			case QUEST_NOTICE -> SIZE_QUEST_NOTICE;
			case SCOUTER -> SIZE_SCOUTER;
			case BABA_TIMER -> SIZE_BABA_TIMER;
		};
	}

	public static boolean canMirror(HudStyle style, HudElement element) {
		return element == HudElement.PARTY || element == HudElement.SCOUTER || element.isSkill() || (element == HudElement.MAIN && (style == HudStyle.DEFAULT || style == HudStyle.LEGACY_1));
	}

	public static float unit(HudElement element, int screenHeight) {
		if (element == HudElement.MC_LEFT || element == HudElement.MC_RIGHT) return 1.0f;
		if (element == HudElement.L2_HEALTH || element == HudElement.L2_KI || element == HudElement.L2_STAMINA) return 1.0f;
		return Math.max(0.25f, screenHeight / REFERENCE_HEIGHT);
	}

	public static HudPlacement defaultPlacement(HudStyle style, HudElement element) {
		float meterWidth = SIZE_METER[0];
		return switch (element) {
			case MAIN -> style == HudStyle.LEGACY_1 ? new HudPlacement(0.0f, 0.0f, 3.0f, 3.0f, 1.2f)
					: new HudPlacement(0.0f, 0.0f, 3.0f, 3.0f, 1.0f);
			case L2_HEALTH -> new HudPlacement(0.5f, 1.0f, -56.875f, -30.0f, 1.25f);
			case L2_KI -> new HudPlacement(0.5f, 1.0f, -83.125f, -2.5f, 1.25f);
			case L2_STAMINA -> new HudPlacement(0.5f, 1.0f, 55.625f, -41.25f, 1.25f);
			case MC_LEFT -> new HudPlacement(0.5f, 1.0f, -HOTBAR_HALF_WIDTH - SIZE_MINECRAFT_SIDE[0] / 2.0f, 0.0f, 1.0f).hotbar();
			case MC_RIGHT -> new HudPlacement(0.5f, 1.0f, HOTBAR_HALF_WIDTH + SIZE_MINECRAFT_SIDE[0] / 2.0f, 0.0f, 1.0f).hotbar();
			case RESERVE -> new HudPlacement(0.0f, 0.5f, 2.0f, 0.0f, 1.0f);
			case RAGE -> new HudPlacement(0.0f, 0.5f, 2.0f + meterWidth + 2.0f, 0.0f, 1.0f);
			case PARTY -> new HudPlacement(0.0f, 0.5f, 2.0f + (meterWidth + 2.0f) * 2.0f, 0.0f, 0.67f);
			case SKILL_1, SKILL_2, SKILL_3, SKILL_4 -> new HudPlacement(0.0f, 1.0f,
					SKILL_MARGIN_X + SKILL_LADDER_STEP * element.skillRow(),
					-(SKILL_MARGIN_BOTTOM + (3 - element.skillRow()) * SIZE_SKILL[1] * SKILL_SCALE), SKILL_SCALE);
			case TRACKED_QUEST -> new HudPlacement(1.0f, 0.0f, -6.0f, 6.0f, 1.0f);
			case QUEST_NOTICE -> new HudPlacement(1.0f, 0.5f, -6.0f, -40.0f, 0.8f);
			case SCOUTER -> new HudPlacement(0.0f, 0.5f, 0.0f, -82.0f, 2.0f);
			case BABA_TIMER -> new HudPlacement(0.5f, 0.0f, 0.0f, 4.0f, 1.0f);
		};
	}

	public static HudPlacement placement(HudStyle style, HudElement element) {
		Map<String, HudPlacement> styleLayout = ConfigManager.getUserConfig().getHudLayout().get(layoutKey(style, element));
		HudPlacement stored = styleLayout != null ? styleLayout.get(element.id()) : null;
		return stored != null ? stored : defaultPlacement(style, element);
	}

	public static void setPlacement(HudStyle style, HudElement element, HudPlacement placement) {
		ConfigManager.getUserConfig().getHudLayout()
				.computeIfAbsent(layoutKey(style, element), key -> new LinkedHashMap<>())
				.put(element.id(), placement);
	}

	public static void resetPlacement(HudStyle style, HudElement element) {
		Map<String, HudPlacement> styleLayout = ConfigManager.getUserConfig().getHudLayout().get(layoutKey(style, element));
		if (styleLayout != null) styleLayout.remove(element.id());
	}

	public static Box box(HudStyle style, HudElement element, HudPlacement placement, int screenWidth, int screenHeight) {
		float[] size = baseSize(style, element);
		float unit = unit(element, screenHeight);
		float scale = Mth.clamp(placement.getScale(), MIN_SCALE, MAX_SCALE) * unit;
		scale = Math.min(scale, Math.min(screenWidth * MAX_SCREEN_SHARE / size[0], screenHeight * MAX_SCREEN_SHARE / size[1]));
		float width = size[0] * scale;
		float height = size[1] * scale;

		if (placement.getHotbar() && (element == HudElement.MC_LEFT || element == HudElement.MC_RIGHT)) {
			float centerX = screenWidth / 2.0f;
			float x = element == HudElement.MC_LEFT ? centerX - HOTBAR_HALF_WIDTH - size[0] : centerX + HOTBAR_HALF_WIDTH;
			return new Box(x, screenHeight - HOTBAR_HEIGHT, size[0], size[1], 1.0f, placement.getVisible(), true, false);
		}

		float x = screenWidth * placement.getAnchorX() + placement.getOffsetX() * unit - width * placement.getAnchorX();
		float y = screenHeight * placement.getAnchorY() + placement.getOffsetY() * unit - height * placement.getAnchorY();
		x = Mth.clamp(x, 0.0f, Math.max(0.0f, screenWidth - width));
		y = Mth.clamp(y, 0.0f, Math.max(0.0f, screenHeight - height));
		return new Box(x, y, width, height, scale, placement.getVisible(), false, placement.getMirrored() && canMirror(style, element));
	}

	public static Box baseBox(HudStyle style, HudElement element, int screenWidth, int screenHeight) {
		return box(style, element, placement(style, element), screenWidth, screenHeight);
	}

	public static HudPlacement toPlacement(HudElement element, float x, float y, float width, float height, float scale, int screenWidth, int screenHeight, boolean visible) {
		float unit = unit(element, screenHeight);
		float anchorX = nearestAnchor((x + width / 2.0f) / Math.max(1, screenWidth));
		float anchorY = nearestAnchor((y + height / 2.0f) / Math.max(1, screenHeight));
		float offsetX = (x + width * anchorX - screenWidth * anchorX) / unit;
		float offsetY = (y + height * anchorY - screenHeight * anchorY) / unit;
		HudPlacement placement = new HudPlacement(anchorX, anchorY, offsetX, offsetY, scale / unit);
		placement.setVisible(visible);
		return placement;
	}

	private static float nearestAnchor(float fraction) {
		return fraction < 1.0f / 3.0f ? 0.0f : fraction > 2.0f / 3.0f ? 1.0f : 0.5f;
	}

	public static Box resolve(HudElement element, int screenWidth, int screenHeight) {
		HudStyle style = HudStyle.current();
		Box base = baseBox(style, element, screenWidth, screenHeight);
		if (preview) return base;
		float[] shift = shifts(style, screenWidth, screenHeight).getOrDefault(element, ZERO);
		return base.moved(shift[0], shift[1]);
	}

	private static final float[] ZERO = {0.0f, 0.0f};

	public static boolean isCollapsible(HudElement element, int screenWidth, int screenHeight) {
		return shifts(HudStyle.current(), screenWidth, screenHeight).containsKey(element);
	}

	private static float visibility(HudElement element) {
		return element.isMeter() ? METER_VISIBILITY.getOrDefault(element, 0.0f) : 1.0f;
	}

	private static Map<HudElement, float[]> shifts(HudStyle style, int screenWidth, int screenHeight) {
		Map<HudElement, float[]> result = new EnumMap<>(HudElement.class);
		Map<HudElement, Box> boxes = new EnumMap<>(HudElement.class);
		for (HudElement element : elements(style)) {
			Box box = baseBox(style, element, screenWidth, screenHeight);
			if (box.visible() && !box.hotbar() && !element.isSkill()) boxes.put(element, box);
		}

		for (Map.Entry<HudElement, Box> entry : boxes.entrySet()) {
			if (!entry.getKey().isMeter() || result.containsKey(entry.getKey())) continue;
			int edge = dockedEdge(entry.getValue(), screenWidth, screenHeight);
			if (edge < 0) continue;
			walkChain(entry.getKey(), edge, boxes, result, screenWidth, screenHeight);
		}
		return result;
	}

	private static int dockedEdge(Box box, int screenWidth, int screenHeight) {
		float[] gaps = {box.x(), screenWidth - box.right(), box.y(), screenHeight - box.bottom()};
		float reach = DOCK_DISTANCE * Math.max(1.0f, screenHeight / REFERENCE_HEIGHT);
		int best = -1;
		for (int i = 0; i < gaps.length; i++) {
			if (gaps[i] <= reach && (best < 0 || gaps[i] < gaps[best])) best = i;
		}
		return best;
	}

	private static void walkChain(HudElement start, int edge, Map<HudElement, Box> boxes, Map<HudElement, float[]> result, int screenWidth, int screenHeight) {
		float directionX = edge == 0 ? -1.0f : edge == 1 ? 1.0f : 0.0f;
		float directionY = edge == 2 ? -1.0f : edge == 3 ? 1.0f : 0.0f;
		float accumulated = 0.0f;
		HudElement current = start;

		for (int guard = 0; current != null && guard < HudElement.values().length; guard++) {
			Box box = boxes.get(current);
			float hidden = 1.0f - visibility(current);
			HudElement next = linked(current, edge, boxes, result, screenHeight);

			float own = accumulated;
			if (current.isMeter()) own += hidden * (extent(box, edge) + DOCK_DISTANCE * Math.max(1.0f, screenHeight / REFERENCE_HEIGHT));
			result.put(current, new float[]{directionX * own, directionY * own});

			if (current.isMeter() && next != null) accumulated += hidden * Math.abs(near(boxes.get(next), edge) - near(box, edge));
			current = next;
		}
	}

	private static float extent(Box box, int edge) {
		return edge < 2 ? box.width() : box.height();
	}

	private static float near(Box box, int edge) {
		return switch (edge) {
			case 0 -> box.x();
			case 1 -> box.right();
			case 2 -> box.y();
			default -> box.bottom();
		};
	}

	private static float far(Box box, int edge) {
		return switch (edge) {
			case 0 -> box.right();
			case 1 -> box.x();
			case 2 -> box.bottom();
			default -> box.y();
		};
	}

	private static HudElement linked(HudElement from, int edge, Map<HudElement, Box> boxes, Map<HudElement, float[]> done, int screenHeight) {
		Box origin = boxes.get(from);
		float reach = LINK_DISTANCE * Math.max(1.0f, screenHeight / REFERENCE_HEIGHT);
		HudElement best = null;
		float bestGap = Float.MAX_VALUE;
		for (Map.Entry<HudElement, Box> entry : boxes.entrySet()) {
			if (entry.getKey() == from || done.containsKey(entry.getKey())) continue;
			Box other = entry.getValue();
			boolean overlaps = edge < 2
					? other.y() < origin.bottom() && other.bottom() > origin.y()
					: other.x() < origin.right() && other.right() > origin.x();
			if (!overlaps) continue;
			float gap = Math.abs(near(other, edge) - far(origin, edge));
			boolean beyond = edge == 0 || edge == 2 ? near(other, edge) >= far(origin, edge) - 1.0f : near(other, edge) <= far(origin, edge) + 1.0f;
			if (beyond && gap <= reach && gap < bestGap) {
				best = entry.getKey();
				bestGap = gap;
			}
		}
		return best;
	}
}
