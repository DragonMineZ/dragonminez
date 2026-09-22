package com.dragonminez.client.gui.worldboss;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.util.NumberFormattingUtil;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.quest.QuestParser;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.quest.rewards.GenericItemReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.worldboss.WorldBossResults;
import com.dragonminez.common.worldboss.WorldBossRewardText;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class WorldBossResultsScreen extends ScaledScreen {
	private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menusmall.png");
	private static final ResourceLocation BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation REWARD_GENERIC_ICON = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/quest/reward_generic.png");

	private static final int PANEL_WIDTH = 340;
	private static final int PANEL_HEIGHT = 216;
	private static final int PADDING = 12;
	private static final int DIVIDER_X = 186;
	private static final int LIST_TOP = 44;
	private static final int LIST_HEIGHT = 138;
	private static final int ROW_HEIGHT = 24;
	private static final int MAX_PLAYERS_SHOWN = 10;
	private static final int BADGE_WIDTH = 34;
	private static final int BADGE_HEIGHT = 12;
	private static final int BAR_MAX_WIDTH = 96;
	private static final int BAR_HEIGHT = 7;
	private static final int REWARD_ROW_HEIGHT = 24;

	private static final int TITLE_COLOR = 0xFFD54F;
	private static final int DURATION_COLOR = 0xFF6B6B;
	private static final int NAME_COLOR = 0xFFE066;
	private static final int TEXT_COLOR = 0xE8ECF2;
	private static final int MUTED_COLOR = 0x9FB8AE;
	private static final int HOVER_PLATE = 0x1CFFFFFF;
	private static final int SELECTED_PLATE = 0x30FFFFFF;
	private static final int RECEIVED_COLOR = 0x7CFDD6;
	private static final int DAMAGE_COLOR = 0xE53935;
	private static final int MITIGATED_COLOR = 0x3F8CFF;
	private static final int RECEIVED_DAMAGE_COLOR = 0xFF8F2B;
	private static final int HEALED_COLOR = 0x5BD64A;

	private enum Segment { DAMAGE, MITIGATED, RECEIVED, HEALED }

	private record SegmentHit(int x, int y, int width, int height, Segment segment, WorldBossResults.PlayerEntry entry) {
		boolean contains(double px, double py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private record RowHit(int x, int y, int width, int height, WorldBossResults.PlayerEntry entry) {
		boolean contains(double px, double py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private record RewardHit(int x, int y, int width, int height, int index) {
		boolean contains(double px, double py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	private final WorldBossResults results;
	private final List<QuestReward> rewards = new ArrayList<>();
	private final List<WorldBossResults.PlayerEntry> shown;
	private final List<SegmentHit> segmentHits = new ArrayList<>();
	private final List<RowHit> rowHits = new ArrayList<>();
	private final List<RewardHit> rewardHits = new ArrayList<>();

	private UUID selected;
	private int guiLeft;
	private int guiTop;
	private float targetScroll;
	private float currentScroll;
	private float maxScroll;
	private boolean draggingScroll;

	public static void open(WorldBossResults results) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || results == null) return;
		mc.setScreen(new WorldBossResultsScreen(results));
	}

	public WorldBossResultsScreen(WorldBossResults results) {
		super(Component.translatable("gui.dragonminez.worldboss.results.title", Component.translatable(results.bossNameKey()))
				.withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		this.results = results;
		this.shown = results.players().size() > MAX_PLAYERS_SHOWN
				? new ArrayList<>(results.players().subList(0, MAX_PLAYERS_SHOWN))
				: new ArrayList<>(results.players());
		for (WorldBossResults.Reward reward : results.rewards()) rewards.add(parseReward(reward.json()));

		Minecraft mc = Minecraft.getInstance();
		UUID local = mc.player != null ? mc.player.getUUID() : null;
		if (local != null && results.find(local) != null) selected = local;
		else if (!shown.isEmpty()) selected = shown.get(0).id();
	}

	private static QuestReward parseReward(String json) {
		try {
			return QuestParser.parseReward(JsonParser.parseString(json).getAsJsonObject());
		} catch (RuntimeException e) {
			return null;
		}
	}

	@Override
	protected int getMinGuiWidth() {
		return PANEL_WIDTH + 20;
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (getUiWidth() - PANEL_WIDTH) / 2;
		this.guiTop = (getUiHeight() - PANEL_HEIGHT) / 2;

		TexturedTextButton close = new TexturedTextButton.Builder()
				.position(guiLeft + PANEL_WIDTH - PADDING - 74, guiTop + PANEL_HEIGHT - PADDING - 20)
				.size(74, 20)
				.texture(BUTTONS)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(tr("gui.dragonminez.worldboss.results.close"))
				.onPress(btn -> onClose())
				.build();
		addRenderableWidget(close);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int uiMouseX = (int) toUiX(mouseX);
		int uiMouseY = (int) toUiY(mouseY);

		beginUiScale(graphics);
		segmentHits.clear();
		rowHits.clear();
		rewardHits.clear();

		HudRender.nineSlice(graphics, PANEL, guiLeft, guiTop, PANEL_WIDTH, PANEL_HEIGHT, 0.0f, 0.0f, 141.0f, 94.0f, 8.0f, 256, 256);
		renderTitle(graphics);
		renderContribution(graphics, uiMouseX, uiMouseY);
		HudRender.rect(graphics, guiLeft + DIVIDER_X, guiTop + 30, 1.0f, PANEL_HEIGHT - 44, 0x99FFFFFF);
		renderRewards(graphics, uiMouseX, uiMouseY);

		super.render(graphics, uiMouseX, uiMouseY, partialTick);
		renderTooltips(graphics, uiMouseX, uiMouseY);
		endUiScale(graphics);
	}

	private void renderTitle(GuiGraphics graphics) {
		MutableComponent title = tr("gui.dragonminez.worldboss.results.title", Component.translatable(results.bossNameKey()));
		TextUtil.drawCenteredStringWithBorder(graphics, font, title, guiLeft + PANEL_WIDTH / 2, guiTop + 10, TITLE_COLOR);
		MutableComponent duration = txt(QuestTextFormatter.formatGameTimeDuration(results.durationTicks()));
		TextUtil.drawStringWithBorder(graphics, font, duration, guiLeft + PANEL_WIDTH - PADDING - font.width(duration), guiTop + 10, DURATION_COLOR);
	}

	private void renderContribution(GuiGraphics graphics, int mouseX, int mouseY) {
		int left = guiLeft + PADDING;
		int width = DIVIDER_X - PADDING * 2;
		TextUtil.drawStringWithBorder(graphics, font, tr("gui.dragonminez.worldboss.results.contribution"), left, guiTop + 30, TITLE_COLOR);

		int listTop = guiTop + LIST_TOP;
		int totalHeight = shown.size() * ROW_HEIGHT;
		maxScroll = Math.max(0, totalHeight - LIST_HEIGHT);
		targetScroll = Mth.clamp(targetScroll, 0.0f, maxScroll);
		currentScroll = Mth.lerp(frameEase(), currentScroll, targetScroll);
		if (Math.abs(currentScroll - targetScroll) < 0.05f) currentScroll = targetScroll;

		float best = shown.isEmpty() ? 1.0f : Math.max(1.0e-6f, shown.get(0).points());
		boolean mouseInList = mouseX >= left && mouseX < left + width && mouseY >= listTop && mouseY < listTop + LIST_HEIGHT;

		graphics.enableScissor(toScreenCoord(left), toScreenCoord(listTop), toScreenCoord(left + width), toScreenCoord(listTop + LIST_HEIGHT));
		graphics.pose().pushPose();
		graphics.pose().translate(0.0f, -currentScroll, 0.0f);
		for (int i = 0; i < shown.size(); i++) {
			WorldBossResults.PlayerEntry entry = shown.get(i);
			int rowY = listTop + i * ROW_HEIGHT;
			int visibleY = Math.round(rowY - currentScroll);
			if (visibleY + ROW_HEIGHT < listTop || visibleY > listTop + LIST_HEIGHT) continue;
			boolean selectedRow = entry.id().equals(selected);
			boolean hovered = mouseInList && mouseY >= visibleY && mouseY < visibleY + ROW_HEIGHT;
			if (selectedRow || hovered) HudRender.rect(graphics, left - 2, rowY, width + 4, ROW_HEIGHT - 1, selectedRow ? SELECTED_PLATE : HOVER_PLATE);
			renderRow(graphics, entry, left, rowY, visibleY, best, width);
			rowHits.add(new RowHit(left, visibleY, width, ROW_HEIGHT, entry));
		}
		graphics.pose().popPose();
		graphics.disableScissor();

		if (maxScroll > 0.0f) {
			int barX = left + width + 3;
			graphics.fill(barX, listTop, barX + 3, listTop + LIST_HEIGHT, 0xFF333333);
			int indicatorHeight = Math.max(14, Math.round(LIST_HEIGHT * (LIST_HEIGHT / (float) totalHeight)));
			int indicatorY = listTop + Math.round((LIST_HEIGHT - indicatorHeight) * (currentScroll / maxScroll));
			graphics.fill(barX, indicatorY, barX + 3, indicatorY + indicatorHeight, 0xFFAAAAAA);
		}
	}

	private void renderRow(GuiGraphics graphics, WorldBossResults.PlayerEntry entry, int left, int rowY, int visibleY, float best, int width) {
		int badgeY = rowY + 2;
		HudRender.rect(graphics, left, badgeY, BADGE_WIDTH, BADGE_HEIGHT, 0xAA101018);
		graphics.renderOutline(left, badgeY, BADGE_WIDTH, BADGE_HEIGHT, 0xFFD9DEE6);
		TextUtil.drawCenteredStringWithBorder(graphics, font, rankLabel(entry.rank()), left + BADGE_WIDTH / 2, badgeY + 2, 0xFFFFFF);

		int nameX = left + BADGE_WIDTH + 6;
		MutableComponent name = txt(entry.name());
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && entry.id().equals(mc.player.getUUID())) {
			name.append(Component.literal(" ")).append(tr("gui.dragonminez.worldboss.results.you").withStyle(ChatFormatting.GRAY));
		}
		TextUtil.drawStringWithBorder(graphics, font, name, nameX, rowY + 1, NAME_COLOR);

		int barY = rowY + 13;
		int barWidth = Math.max(2, Math.round(BAR_MAX_WIDTH * Mth.clamp(entry.points() / best, 0.0f, 1.0f)));
		HudRender.rect(graphics, nameX - 1, barY - 1, BAR_MAX_WIDTH + 2, BAR_HEIGHT + 2, 0x66000000);
		GeneralServerConfig.WorldBossConfig config = ConfigManager.getServerConfig().getWorldBoss();
		float[] weighted = {
				entry.damage() * (float) config.getDamageWeight(),
				entry.mitigated() * (float) config.getMitigatedWeight(),
				entry.receivedTotal() * (float) config.getReceivedWeight(),
				entry.healed() * (float) config.getHealedWeight()};
		float weightedTotal = weighted[0] + weighted[1] + weighted[2] + weighted[3];
		int[] colors = {DAMAGE_COLOR, MITIGATED_COLOR, RECEIVED_DAMAGE_COLOR, HEALED_COLOR};
		Segment[] segments = Segment.values();
		float cursor = nameX;
		for (int s = 0; s < 4; s++) {
			if (weightedTotal <= 0.0f || weighted[s] <= 0.0f) continue;
			float segmentWidth = barWidth * (weighted[s] / weightedTotal);
			HudRender.rect(graphics, cursor, barY, segmentWidth, BAR_HEIGHT, HudRender.argb(1.0f, colors[s]));
			segmentHits.add(new SegmentHit(Math.round(cursor), visibleY + 13, Math.max(1, Math.round(segmentWidth)), BAR_HEIGHT, segments[s], entry));
			cursor += segmentWidth;
		}

		MutableComponent percent = txt(NumberFormattingUtil.formatUpToOneDecimal(entry.share() * 100.0f) + "%");
		TextUtil.drawStringWithBorder(graphics, font, percent, left + width - font.width(percent), barY - 1, TEXT_COLOR);
	}

	private void renderRewards(GuiGraphics graphics, int mouseX, int mouseY) {
		int left = guiLeft + DIVIDER_X + PADDING;
		int width = PANEL_WIDTH - DIVIDER_X - PADDING * 2;
		TextUtil.drawStringWithBorder(graphics, font, tr("gui.dragonminez.worldboss.results.rewards"), left, guiTop + 30, TITLE_COLOR);

		WorldBossResults.PlayerEntry target = selected != null ? results.find(selected) : null;
		int y = guiTop + LIST_TOP;
		if (target != null) {
			TextUtil.drawStringWithBorder(graphics, font, tr("gui.dragonminez.worldboss.results.chances_for", target.name()), left, y, MUTED_COLOR);
			y += 10;
			TextUtil.drawStringWithBorder(graphics, font, tr("gui.dragonminez.worldboss.results.total_points",
					NumberFormattingUtil.formatLargeNumber(target.points()), NumberFormattingUtil.formatUpToOneDecimal(target.share() * 100.0f)), left, y, MUTED_COLOR);
			y += 14;
		}

		for (int j = 0; j < rewards.size(); j++) {
			QuestReward reward = rewards.get(j);
			if (reward == null) continue;
			float baseChance = results.rewards().get(j).baseChance();
			float chance = target != null && j < target.rewardChance().length ? target.rewardChance()[j] : baseChance;
			float amount = target != null && j < target.rewardAmount().length ? target.rewardAmount()[j] : 0.0f;
			boolean received = amount > 0.0f;

			ItemStack icon = rewardIcon(reward);
			if (icon != null) graphics.renderItem(icon, left, y);
			else HudRender.blit(graphics, REWARD_GENERIC_ICON, left, y, 0.0f, 0.0f, 16.0f, 16.0f, 16, 16);

			int textX = left + 20;
			MutableComponent name = received
					? WorldBossRewardText.describe(reward, amount).copy().withStyle(Style.EMPTY.withFont(DMZ_FONT))
					: WorldBossRewardText.name(reward).copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));
			TextUtil.drawStringWithBorder(graphics, font, name, textX, y, received ? RECEIVED_COLOR : TEXT_COLOR);
			TextUtil.drawStringWithBorder(graphics, font, chanceLine(baseChance, chance, received), textX, y + 10, received ? RECEIVED_COLOR : MUTED_COLOR);

			rewardHits.add(new RewardHit(left, y, width, REWARD_ROW_HEIGHT - 2, j));
			y += REWARD_ROW_HEIGHT;
		}
	}

	private MutableComponent chanceLine(float baseChance, float chance, boolean received) {
		MutableComponent line;
		if (baseChance >= 1.0f) {
			line = tr("gui.dragonminez.worldboss.results.guaranteed");
		} else if (Math.abs(chance - baseChance) < 0.0005f) {
			line = txt(percentText(baseChance));
		} else {
			line = txt(percentText(baseChance)).withStyle(ChatFormatting.STRIKETHROUGH)
					.append(txt(" -> " + percentText(chance)).withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		}
		if (received) line.append(txt(" ")).append(tr("gui.dragonminez.worldboss.results.received"));
		return line;
	}

	private static String percentText(float chance) {
		return NumberFormattingUtil.formatUpToOneDecimal(chance * 100.0f) + "%";
	}

	private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
		for (SegmentHit hit : segmentHits) {
			if (!hit.contains(mouseX, mouseY)) continue;
			TextUtil.renderAdvancedTooltip(graphics, font, mouseX, mouseY, getUiWidth(), getUiHeight(),
					segmentTitle(hit.segment()), segmentLines(hit.segment(), hit.entry()), null, 0xFFFFFF);
			return;
		}
		for (RewardHit hit : rewardHits) {
			if (!hit.contains(mouseX, mouseY)) continue;
			QuestReward reward = rewards.get(hit.index());
			if (reward == null) return;
			List<Component> lines = new ArrayList<>();
			lines.add(reward.getDescription().copy().withStyle(Style.EMPTY.withFont(DMZ_FONT)));
			TextUtil.renderAdvancedTooltip(graphics, font, mouseX, mouseY, getUiWidth(), getUiHeight(),
					WorldBossRewardText.name(reward).copy().withStyle(Style.EMPTY.withFont(DMZ_FONT)), lines, null, 0xFFFFFF);
			return;
		}
	}

	private MutableComponent segmentTitle(Segment segment) {
		return switch (segment) {
			case DAMAGE -> tr("gui.dragonminez.worldboss.results.damage").withStyle(s -> s.withColor(DAMAGE_COLOR));
			case MITIGATED -> tr("gui.dragonminez.worldboss.results.mitigated").withStyle(s -> s.withColor(MITIGATED_COLOR));
			case RECEIVED -> tr("gui.dragonminez.worldboss.results.received_damage").withStyle(s -> s.withColor(RECEIVED_DAMAGE_COLOR));
			case HEALED -> tr("gui.dragonminez.worldboss.results.healed").withStyle(s -> s.withColor(HEALED_COLOR));
		};
	}

	private List<Component> segmentLines(Segment segment, WorldBossResults.PlayerEntry entry) {
		List<Component> lines = new ArrayList<>();
		switch (segment) {
			case DAMAGE -> {
				addLine(lines, "gui.dragonminez.worldboss.results.damage.melee", entry.damageMelee());
				addLine(lines, "gui.dragonminez.worldboss.results.damage.strike", entry.damageStrike());
				addLine(lines, "gui.dragonminez.worldboss.results.damage.ki", entry.damageKi());
				addLine(lines, "gui.dragonminez.worldboss.results.damage.other", entry.damageOther());
			}
			case MITIGATED -> {
				addLine(lines, "gui.dragonminez.worldboss.results.mitigated.defense", entry.mitigatedDefense());
				addLine(lines, "gui.dragonminez.worldboss.results.mitigated.block", entry.mitigatedBlock());
				addLine(lines, "gui.dragonminez.worldboss.results.mitigated.shield", entry.mitigatedShield());
			}
			case RECEIVED -> {
				for (WorldBossResults.NamedAmount amount : entry.received()) {
					if (amount.amount() <= 0.0f) continue;
					Component source = amount.nameKey().isEmpty() ? tr("gui.dragonminez.worldboss.results.unknown_source") : Component.translatable(amount.nameKey());
					lines.add(txt("").append(source.copy().withStyle(Style.EMPTY.withFont(DMZ_FONT)))
							.append(txt(": " + NumberFormattingUtil.formatLargeNumber(amount.amount()))));
				}
			}
			case HEALED -> {
				addLine(lines, "gui.dragonminez.worldboss.results.healed.self", entry.healedSelf());
				addLine(lines, "gui.dragonminez.worldboss.results.healed.allies", entry.healedAllies());
			}
		}
		if (lines.isEmpty()) lines.add(tr("gui.dragonminez.worldboss.results.none").withStyle(ChatFormatting.GRAY));
		return lines;
	}

	private void addLine(List<Component> lines, String key, float amount) {
		if (amount <= 0.0f) return;
		lines.add(tr(key).append(txt(": " + NumberFormattingUtil.formatLargeNumber(amount))));
	}

	private MutableComponent rankLabel(int rank) {
		return switch (rank) {
			case 0 -> tr("gui.dragonminez.worldboss.results.rank.mvp");
			case 1 -> tr("gui.dragonminez.worldboss.results.rank.second");
			case 2 -> tr("gui.dragonminez.worldboss.results.rank.third");
			default -> tr("gui.dragonminez.worldboss.results.rank.nth", rank + 1);
		};
	}

	private static ItemStack rewardIcon(QuestReward reward) {
		return switch (reward.getType()) {
			case ITEM -> reward instanceof ItemReward item
					? new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item.getItemId())), Math.max(1, item.getCount()))
					: null;
			case GENERIC_ITEM -> reward instanceof GenericItemReward generic && !generic.getItemReward().getItemStack().isEmpty()
					? generic.getItemReward().getItemStack()
					: null;
			case TPS -> new ItemStack(MainItems.RED_CAPSULE.get());
			case SKILL -> new ItemStack(MainItems.GETE_BLUE_CAPSULE.get());
			case COMMAND -> new ItemStack(Items.COMMAND_BLOCK);
			case KI_TECHNIQUE -> new ItemStack(MainItems.MERUS_LASER.get());
			case TRANSFORMATION -> new ItemStack(MainItems.MIGHT_TREE_FRUIT.get());
			default -> null;
		};
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		double uiX = toUiX(mouseX);
		double uiY = toUiY(mouseY);

		int left = guiLeft + PADDING;
		int width = DIVIDER_X - PADDING * 2;
		int listTop = guiTop + LIST_TOP;
		if (maxScroll > 0.0f && uiX >= left + width && uiX <= left + width + 8 && uiY >= listTop && uiY <= listTop + LIST_HEIGHT) {
			draggingScroll = true;
			targetScroll = Mth.clamp((float) (uiY - listTop) / LIST_HEIGHT, 0.0f, 1.0f) * maxScroll;
			return true;
		}
		if (uiY < listTop || uiY > listTop + LIST_HEIGHT) return false;
		for (RowHit hit : rowHits) {
			if (!hit.contains(uiX, uiY)) continue;
			selected = hit.entry().id();
			Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
					net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		double uiX = toUiX(mouseX);
		int left = guiLeft + PADDING;
		if (maxScroll > 0.0f && uiX < guiLeft + DIVIDER_X && uiX >= left) {
			targetScroll = Mth.clamp(targetScroll - (float) (Math.signum(delta) * ROW_HEIGHT), 0.0f, maxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingScroll && maxScroll > 0.0f) {
			int listTop = guiTop + LIST_TOP;
			targetScroll = Mth.clamp((float) (toUiY(mouseY) - listTop) / LIST_HEIGHT, 0.0f, 1.0f) * maxScroll;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingScroll = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
