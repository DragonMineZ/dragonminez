package com.dragonminez.client.gui.quest.preview;

import com.dragonminez.Reference;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.init.entities.ITextureVariant;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestUnlocks;
import com.dragonminez.common.quest.objectives.KillObjective;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rotating enemy preview for main (SAGA) quests with KILL objectives.
 * <p>
 * Renders a miniature, uncontrollable, auto-rotating client-side dummy of the target
 * enemy (like the character preview in the stat window), cycling through every kill
 * target of the selected quest. Hovering the model eases in a stats/threats card
 * showing Battle Power, Health and the enemy's attacks/threats.
 */
@OnlyIn(Dist.CLIENT)
public class QuestEnemyPreview {

	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final float YAW_DEG_PER_SEC = 55.0f;
	private static final int CYCLE_TICKS = 110; // ~5.5s per target before advancing
	private static final int FADE_TICKS = 8;    // cross-fade window when switching targets
	private static final float HOVER_SPEED = 6.0f;

	/**
	 * One kill target resolved from a quest objective. Health/melee/ki mirror the exact
	 * stats the enemy will spawn with: base objective values amplified by party scaling
	 * ({@code Quest#getScaledKill*}) and by the story difficulty multipliers, matching how
	 * {@code EntitiesEvents} configures the real entity on spawn.
	 */
	private static final class Target {
		final String entityId;
		final double health;
		final double meleeDamage;
		final double kiDamage;
		final int count;
		final int textureVariant;

		Target(Quest quest, KillObjective obj, Difficulty difficulty, int partySize) {
			this.entityId = obj.getEntityId();
			this.health = quest.getScaledKillHealth(obj, partySize) * difficulty.hpMultiplier();
			this.meleeDamage = quest.getScaledKillMeleeDamage(obj, partySize) * difficulty.damageMultiplier();
			this.kiDamage = quest.getScaledKillKiDamage(obj, partySize) * difficulty.damageMultiplier();
			this.count = obj.getCount();
			this.textureVariant = obj.getTextureVariant();
		}
	}

	private final List<Target> targets = new ArrayList<>();
	private final Map<String, LivingEntity> entityCache = new HashMap<>();
	private final Set<String> failedIds = new HashSet<>();

	private Quest boundQuest = null;
	private Difficulty boundDifficulty = Difficulty.NORMAL;
	private int boundPartySize = 1;
	private int currentIndex = 0;
	private int cycleTimer = 0;
	private float modelYaw = 0.0f;
	private float hoverProgress = 0.0f;

	private int hitX, hitY, hitW, hitH;

	public boolean isActive() {
		return !targets.isEmpty();
	}

	public boolean hasMultipleTargets() {
		return targets.size() > 1;
	}

	/**
	 * Manually advance to the next kill target. Used when the player clicks the model
	 * to flip between a quest's multiple enemies (e.g. Trunks ↔ Gohan). Resets the
	 * auto-cycle timer so the just-selected target gets its full dwell time.
	 *
	 * @return true if there was more than one target to cycle through.
	 */
	public boolean advanceTarget() {
		if (targets.size() <= 1) return false;
		currentIndex = (currentIndex + 1) % targets.size();
		cycleTimer = 0;
		hoverProgress = 0.0f;
		return true;
	}

	/**
	 * Rebind the preview to the given quest. Only SAGA (main) quests with at least one
	 * KILL objective produce an active preview; everything else clears it.
	 * <p>
	 * Difficulty and party size feed the stat amplification so the card always shows the
	 * enemy's real spawn stats; a change to either (with the same quest still selected)
	 * rebuilds the targets.
	 */
	public void setQuest(Quest quest, Difficulty difficulty, int partySize) {
		if (difficulty == null) difficulty = Difficulty.NORMAL;
		int safePartySize = Math.max(1, partySize);
		if (quest == boundQuest && difficulty == boundDifficulty && safePartySize == boundPartySize) return;
		boundQuest = quest;
		boundDifficulty = difficulty;
		boundPartySize = safePartySize;
		targets.clear();
		currentIndex = 0;
		cycleTimer = 0;
		hoverProgress = 0.0f;

		if (quest == null || !quest.isSagaQuest()) return;

		for (QuestObjective objective : quest.getObjectives()) {
			if (objective instanceof KillObjective kill) {
				targets.add(new Target(quest, kill, difficulty, safePartySize));
			}
		}
	}

	public void clientTick() {
		if (targets.isEmpty()) return;

		LivingEntity entity = getCurrentEntity();
		if (entity != null) {
			entity.tickCount++; // advance GeckoLib idle animation clock
		}

		if (targets.size() > 1) {
			cycleTimer++;
			if (cycleTimer >= CYCLE_TICKS) {
				cycleTimer = 0;
				currentIndex = (currentIndex + 1) % targets.size();
			}
		}
	}

	/**
	 * Render the preview into the given UI-space region.
	 *
	 * @param visibility 1 = fully shown, 0 = fully hidden (so the saga navigator can
	 *                   superpose / take over the same left area as it slides in).
	 */
	public void render(GuiGraphics graphics, Font font, int regionX, int regionY, int regionW, int regionH,
	                   int mouseX, int mouseY, float dt, float visibility) {
		if (targets.isEmpty() || visibility <= 0.02f) return;

		modelYaw += YAW_DEG_PER_SEC * dt;
		if (modelYaw >= 360.0f) modelYaw -= 360.0f;

		LivingEntity entity = getCurrentEntity();

		int centerX = regionX + regionW / 2;
		int feetY = regionY + (int) (regionH * 0.46f);

		hitW = (int) (regionW * 0.50f);
		hitH = (int) (regionH * 0.48f);
		hitX = centerX - hitW / 2;
		hitY = regionY + (int) (regionH * 0.08f);

		boolean hovered = isHovering(mouseX, mouseY) && visibility > 0.6f;
		float hoverTarget = hovered ? 1.0f : 0.0f;
		hoverProgress = approach(hoverProgress, hoverTarget, HOVER_SPEED * dt);

		int alpha = (int) (visibility * 255.0f) & 0xFF;

		// "Objective target" label + cycle indicator
		MutableComponent header = tr("gui.dragonminez.quest_tree.preview.target");
		TextUtil.drawCenteredStringWithBorder(graphics, font, header, centerX, regionY + 6,
				withAlpha(0xFFD9A441, alpha));

		if (targets.size() > 1) {
			Component counter = txt("‹ " + (currentIndex + 1) + "/" + targets.size() + " ›");
			int counterColor = hovered ? 0xFFFFE08A : 0xFFBBBBBB;
			TextUtil.drawStringWithBorder(graphics, font, counter,
					regionX + regionW - font.width(counter) - 82, regionY + 18, withAlpha(counterColor, alpha));
		}

		// rotating model
		if (entity != null && visibility > 0.4f) {
			int scale = computeModelScale(entity, regionH);
			renderRotatingEntity(graphics, centerX, feetY, scale, entity);
		} else if (entity == null) {
			TextUtil.drawCenteredStringWithBorder(graphics, font,
					tr("gui.dragonminez.quest_tree.preview.unknown"),
					centerX, regionY + regionH / 2, withAlpha(0xFFAA5555, alpha));
		}

		// target name
		Component name = getTargetName(entity);
		TextUtil.drawCenteredStringWithBorder(graphics, font, name, centerX,
				feetY + 6, withAlpha(0xFFFFFFFF, alpha));

		if (hoverProgress > 0.01f) {
			renderStatsCard(graphics, font, entity, regionX, regionY, regionW, regionH, alpha);
		}
	}

	private void renderRotatingEntity(GuiGraphics graphics, int x, int y, int scale, LivingEntity entity) {
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf cameraOrientation = new Quaternionf();

		float yaw = modelYaw;
		entity.yBodyRot = yaw;
		entity.yBodyRotO = yaw;
		entity.setYRot(yaw);
		entity.yRotO = yaw;
		entity.setXRot(0.0f);
		entity.xRotO = 0.0f;
		entity.yHeadRot = yaw;
		entity.yHeadRotO = yaw;

		graphics.pose().pushPose();
		graphics.pose().translate(0.0D, 0.0D, 150.0D);
		InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, cameraOrientation, entity);
		graphics.pose().popPose();
	}

	private void renderStatsCard(GuiGraphics graphics, Font font, LivingEntity entity,
	                             int regionX, int regionY, int regionW, int regionH, int baseAlpha) {
		float ease = easeOut(hoverProgress);
		int cardAlpha = (int) (baseAlpha * ease) & 0xFF;
		if (cardAlpha <= 4) return;

		Target target = targets.get(currentIndex);

		List<Component> lines = new ArrayList<>();
		if (entity instanceof DBSagasEntity dbz) {
			// Battle Power is hidden until Bulma calibrates the scouter; shown as "???" otherwise.
			String bpValue = QuestUnlocks.isCompleted(Minecraft.getInstance().player, QuestUnlocks.SCOUTER_CALIBRATION)
					? abbreviate(dbz.getBattlePower()) : "???";
			lines.add(stat("gui.dragonminez.quest_tree.preview.battle_power", bpValue, 0xFFFFC857));
		}
		lines.add(stat("gui.dragonminez.quest_tree.preview.health", formatNumber(target.health), 0xFFFF6B6B));
		if (target.meleeDamage > 0) {
			lines.add(stat("gui.dragonminez.quest_tree.preview.melee", formatNumber(target.meleeDamage), 0xFFE0A060));
		}
		if (target.kiDamage > 0) {
			lines.add(stat("gui.dragonminez.quest_tree.preview.ki", formatNumber(target.kiDamage), 0xFF6FB7FF));
		}

		List<Component> threats = collectThreats(entity);
		if (!threats.isEmpty()) {
			lines.add(tr("gui.dragonminez.quest_tree.preview.threats").withStyle(s -> s.withBold(true)));
			// The threat/ability breakdown is hidden until Bulma's scouter threat-database upgrade.
			if (QuestUnlocks.isCompleted(Minecraft.getInstance().player, QuestUnlocks.SCOUTER_THREAT_DB)) {
				lines.addAll(threats);
			} else {
				lines.add(tr("gui.dragonminez.quest_tree.preview.scan_required").withStyle(s -> s.withColor(0xFF888888)));
			}
		}

		int pad = 5;
		int lineH = font.lineHeight + 1;

		// size the card to its content so the text always fits
		int contentW = 0;
		for (Component line : lines) {
			contentW = Math.max(contentW, font.width(line));
		}
		int cardW = contentW + pad * 2;
		int cardH = lines.size() * lineH + pad * 2;

		int shift = (int) ((1.0f - ease) * 12);
		int cardRight = regionX + regionW - 4 + shift;
		int cardX = Math.max(regionX + 2, cardRight - cardW);
		int cardY = Math.max(regionY + 2, regionY + regionH - cardH - 4);

		int bg = withAlpha(0xFF0A0A14, (int) (cardAlpha * 0.88f));
		int border = withAlpha(0xFFD9A441, cardAlpha);
		graphics.fill(cardX, cardY, cardRight, cardY + cardH, bg);
		graphics.fill(cardX, cardY, cardRight, cardY + 1, border);
		graphics.fill(cardX, cardY + cardH - 1, cardRight, cardY + cardH, border);

		int ty = cardY + pad;
		for (Component line : lines) {
			// right-aligned within the card
			int lineX = cardRight - pad - font.width(line);
			TextUtil.drawStringWithBorder(graphics, font, line, lineX, ty, withAlpha(0xFFFFFFFF, cardAlpha));
			ty += lineH;
		}
	}

	private List<Component> collectThreats(LivingEntity entity) {
		List<Component> out = new ArrayList<>();
		if (!(entity instanceof DBSagasEntity dbz)) return out;

		Set<String> seen = new HashSet<>();
		for (DBSagasEntity.KiSkill skill : dbz.getSkillPool()) {
			DBSagasEntity.KiSkillType type = DBSagasEntity.KiSkillType.fromId(skill.id);
			if (type == null) continue;
			String key = "gui.dragonminez.quest_tree.preview.skill." + type.name().toLowerCase();
			if (seen.add(key)) {
				out.add(txt("• ").append(tr(key)).withStyle(s -> s.withColor(0xFF8AB4F8)));
			}
		}
		int[] combos = dbz.getAllowedCombos();
		if (combos != null) {
			for (int id : combos) {
				DBSagasEntity.ComboType type = DBSagasEntity.ComboType.fromId(id);
				if (type == null) continue;
				String key = "gui.dragonminez.quest_tree.preview.combo." + type.name().toLowerCase();
				if (seen.add(key)) {
					out.add(txt("• ").append(tr(key)).withStyle(s -> s.withColor(0xFFF08A8A)));
				}
			}
		}
		return out;
	}

	private Component getTargetName(LivingEntity entity) {
		if (entity != null) {
			return entity.getType().getDescription().copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));
		}
		return txt(targets.get(currentIndex).entityId);
	}

	private LivingEntity getCurrentEntity() {
		if (targets.isEmpty()) return null;
		Target target = targets.get(currentIndex);
		String id = target.entityId;

		LivingEntity entity;
		if (entityCache.containsKey(id)) {
			entity = entityCache.get(id);
		} else if (failedIds.contains(id)) {
			return null;
		} else {
			entity = createDummy(id);
			if (entity != null) {
				entityCache.put(id, entity);
			} else {
				failedIds.add(id);
				return null;
			}
		}
		if (entity instanceof ITextureVariant variantEntity) {
			int desiredVariant = target.textureVariant >= 0 ? target.textureVariant : 0;
			if (variantEntity.getTextureVariant() != desiredVariant) {
				variantEntity.setTextureVariant(desiredVariant);
			}
		}
		return entity;
	}

	private LivingEntity createDummy(String id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		EntityType<?> type = resolveType(id);
		if (type == null) return null;
		try {
			Entity created = type.create(mc.level);
			if (created instanceof LivingEntity living) {
				return living;
			}
		} catch (Exception ignored) {
			// some entities may fail to construct client-side; fall through
		}
		return null;
	}

	private static EntityType<?> resolveType(String id) {
		if (id != null && id.startsWith("#")) {
			ResourceLocation tagId = ResourceLocation.tryParse(id.substring(1));
			if (tagId == null) return null;
			TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
			var tags = ForgeRegistries.ENTITY_TYPES.tags();
			if (tags != null) {
				for (EntityType<?> type : tags.getTag(tag)) {
					return type;
				}
			}
			return null;
		}
		ResourceLocation rl = ResourceLocation.tryParse(id);
		return rl == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(rl);
	}

	/** Discard cached dummy entities (call on screen close). */
	public void clear() {
		for (LivingEntity entity : entityCache.values()) {
			if (entity != null) {
				entity.discard();
			}
		}
		entityCache.clear();
		failedIds.clear();
		targets.clear();
		boundQuest = null;
		boundDifficulty = Difficulty.NORMAL;
		boundPartySize = 1;
	}

	public boolean isHovering(int mouseX, int mouseY) {
		return mouseX >= hitX && mouseX <= hitX + hitW && mouseY >= hitY && mouseY <= hitY + hitH;
	}

	private int computeModelScale(LivingEntity entity, int regionH) {
		float bbHeight = Math.max(0.8f, entity.getBbHeight());
		int scale = (int) (regionH * 0.20f / bbHeight);
		return Math.max(5, Math.min(28, scale));
	}

	private MutableComponent stat(String key, String value, int valueColor) {
		return tr(key).append(txt(": ").withStyle(s -> s.withColor(0xFFAAAAAA)))
				.append(txt(value).withStyle(s -> s.withColor(valueColor)));
	}

	private static String formatNumber(double v) {
		if (v == Math.floor(v) && !Double.isInfinite(v)) {
			return abbreviate((long) v);
		}
		return String.format("%.1f", v);
	}

	private static String abbreviate(long v) {
		if (v >= 1_000_000_000L) return trim(v / 1_000_000_000.0) + "B";
		if (v >= 1_000_000L) return trim(v / 1_000_000.0) + "M";
		if (v >= 10_000L) return trim(v / 1_000.0) + "K";
		return String.format("%,d", v);
	}

	private static String trim(double d) {
		String s = String.format("%.1f", d);
		return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
	}

	private static int withAlpha(int argb, int alpha) {
		return (Math.min(alpha, 255) << 24) | (argb & 0x00FFFFFF);
	}

	private static float approach(float current, float target, float step) {
		if (current < target) return Math.min(target, current + step);
		if (current > target) return Math.max(target, current - step);
		return current;
	}

	private static float easeOut(float t) {
		if (t <= 0f) return 0f;
		if (t >= 1f) return 1f;
		return 1.0f - (1.0f - t) * (1.0f - t);
	}

	private MutableComponent tr(String key, Object... args) {
		return Component.translatable(key, args).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	private MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}
