package com.dragonminez.client.gui.tournament;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.dragonminez.common.network.TournamentPackets;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.TournamentPackets;
import com.dragonminez.server.world.tournament.Tournament;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class TournamentBracketScreen extends Screen {

	private static final ResourceLocation DMZ_FONT =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int SLOT_SIZE = 48;
	private static final int SLOT_GAP = 4;
	private static final int BUTTON_WIDTH = 74;
	private static final ResourceLocation BUTTONS_TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/characterbuttons.png");

	private static final ResourceLocation SLOT_FRAMES =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/tournament/slots.png");
	private static final int FRAME_SHEET = 256;

	private static final ResourceLocation CARD_BG =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menunpc.png");
	private static final int CARD_WIDTH = 345;
	private static final int CARD_HEIGHT = 94;
	private static final int CARD_SHEET = 512;
	private static final int CARD_TOP = 58;
	private static final int CARD_TEXT_TOP = 12;


	private enum SlotState {
		UPCOMING(0, 0),
		PLAYER(SLOT_SIZE, 0),
		CURRENT(0, SLOT_SIZE),
		DEFEATED(SLOT_SIZE, 0);

		private final int u;
		private final int v;

		SlotState(int u, int v) {
			this.u = u;
			this.v = v;
		}
	}

	private static Boolean framesTextureExists = null;

	private static final Map<String, LivingEntity> PORTRAIT_CACHE = new HashMap<>();

	private final TournamentPackets.OpenBracketS2C data;

	private float panX = 0;
	private float panY = 0;
	private boolean dragging = false;
	private double dragStartX;
	private double dragStartY;
	private float dragStartPanX;
	private float dragStartPanY;

	private TournamentBracketScreen(TournamentPackets.OpenBracketS2C data) {
		super(Component.translatable("tournament.dragonminez.title"));
		this.data = data;
	}

	public static void open(TournamentPackets.OpenBracketS2C data) {
		Minecraft.getInstance().setScreen(new TournamentBracketScreen(data));
	}

	@Override
	protected void init() {
		int buttonY = this.height - 34;

		if (data.isSignUp()) {
			if (data.getCooldownSeconds() <= 0) {
				addModButton(this.width / 2 - BUTTON_WIDTH - 4, buttonY, "tournament.dragonminez.enter",
						b -> send(TournamentPackets.ActionC2S.Action.SIGN_UP));
			}
		} else if (!data.isCompleted() && !data.isEliminated()) {
			addModButton(this.width / 2 - BUTTON_WIDTH - 4, buttonY, "tournament.dragonminez.fight_next", b -> {
				send(TournamentPackets.ActionC2S.Action.START_MATCH);
				this.onClose();
			});
		}

		addModButton(this.width / 2 + 4, buttonY, "gui.dragonminez.close", b -> this.onClose());
	}

	private void addModButton(int x, int y, String translationKey, Button.OnPress onPress) {
		addRenderableWidget(new TexturedTextButton.Builder()
				.position(x, y)
				.size(BUTTON_WIDTH, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(dmz(Component.translatable(translationKey)))
				.onPress(onPress)
				.build());
	}

	private void renderFormatCard(GuiGraphics graphics, int centreX) {
		List<Component> lines = new ArrayList<>();
		boolean gauntlet = data.isGauntlet();

		lines.add(Component.translatable(gauntlet
				? "tournament.dragonminez.format.gauntlet.title"
				: "tournament.dragonminez.format.bracket.title"));
		lines.add(Component.translatable(gauntlet
				? "tournament.dragonminez.format.gauntlet.line1"
				: "tournament.dragonminez.format.bracket.line1", data.getSeeds().size()));
		lines.add(Component.translatable("tournament.dragonminez.format.line2"));
		lines.add(Component.translatable("tournament.dragonminez.format.line3"));

		int x = centreX - CARD_WIDTH / 2;
		int y = CARD_TOP;
		graphics.blit(CARD_BG, x, y, 0, 0, CARD_WIDTH, CARD_HEIGHT, CARD_SHEET, CARD_SHEET);


		int lineY = y + CARD_TEXT_TOP;

		for (int i = 0; i < lines.size(); i++) {
			graphics.drawCenteredString(this.font, dmz(lines.get(i)), centreX, lineY,
					i == 0 ? 0xFFFFD700 : 0xFFE8F0FF);
			lineY += this.font.lineHeight + 2;
		}
	}

	private void send(TournamentPackets.ActionC2S.Action action) {
		NetworkHandler.sendToServer(new TournamentPackets.ActionC2S(action, data.getNpcEntityId()));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		if (button != 0) return false;

		dragging = true;
		dragStartX = mouseX;
		dragStartY = mouseY;
		dragStartPanX = panX;
		dragStartPanY = panY;
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (dragging && button == 0) {
			panX = dragStartPanX + (float) (mouseX - dragStartX);
			panY = dragStartPanY + (float) (mouseY - dragStartY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) dragging = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int centreX = this.width / 2;

		MutableComponent title = Component.translatable(data.getDisplayName()).copy();
		graphics.drawCenteredString(this.font, dmz(title), centreX, 16, 0xFFFFD700);
		graphics.drawCenteredString(this.font, dmz(stars()), centreX, 30, 0xFFFFAA00);
		graphics.drawCenteredString(this.font, dmz(statusLine()), centreX, 46, 0xFFE8F0FF);

		renderFormatCard(graphics, centreX);
		renderLadder(graphics, centreX, mouseX, mouseY);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private Component stars() {
		StringBuilder sb = new StringBuilder();
		int filled = data.getDifficultyStars();
		for (int i = 0; i < 5; i++) sb.append(i < filled ? "★" : "☆");
		return Component.literal(sb.toString());
	}

	private Component statusLine() {
		if (data.getCooldownSeconds() > 0) {
			return Component.translatable("tournament.dragonminez.cooldown", formatDuration(data.getCooldownSeconds()));
		}
		if (data.isCompleted()) return Component.translatable("tournament.dragonminez.status.champion");
		if (data.isEliminated()) return Component.translatable("tournament.dragonminez.status.eliminated");
		if (data.isSignUp()) return Component.translatable("tournament.dragonminez.status.open");

		int qRounds = Math.max(1, qualifierRounds());
		int round = data.getRound();
		if (round < qRounds) return Component.translatable("tournament.dragonminez.round.qualifier", round + 1);
		if (round == qRounds) return Component.translatable("tournament.dragonminez.round.semi");
		return Component.translatable("tournament.dragonminez.round.final");
	}




	private int qualifierRounds() {
		if (data.isGauntlet()) return data.getSeeds().size();

		int rounds = 0;
		for (int remaining = data.getSeeds().size(); remaining > 1; remaining /= 2) rounds++;
		return rounds;
	}

	private String entrantAt(int r, int position) {
		List<String> source = r == 0
				? data.getSeeds()
				: (r - 1 < data.getWinners().size() ? data.getWinners().get(r - 1) : List.of());
		return position >= 0 && position < source.size() ? source.get(position) : "";
	}

	private int columnX(int centreX, int column, int columns) {
		int step = SLOT_SIZE + 30;
		return centreX + Math.round((column - (columns - 1) / 2.0F) * step + panX) - SLOT_SIZE / 2;
	}

	private void renderLadder(GuiGraphics graphics, int centreX, int mouseX, int mouseY) {
		if (data.isGauntlet()) {
			renderGauntlet(graphics, centreX, mouseX, mouseY);
			return;
		}

		int qRounds = Math.max(1, qualifierRounds());
		int columns = data.isGauntlet() ? qRounds + 2 : qRounds + 3;
		int seedCount = data.isGauntlet() ? 1 : Math.max(2, data.getSeeds().size());
		int currentRound = data.isSignUp() ? -1 : data.getRound();

		int topRow = this.height / 2 - (seedCount * (SLOT_SIZE + 6)) / 2 + 24 + Math.round(panY);

		renderConnectors(graphics, centreX, columns, qRounds, seedCount, topRow);

		for (int column = 0; column < columns; column++) {
			int x = columnX(centreX, column, columns);
			int entries = data.isGauntlet() ? 1 : (column <= qRounds ? seedCount >> column : 1);
			int spacing = (seedCount * (SLOT_SIZE + 6)) / Math.max(1, entries);

			for (int i = 0; i < entries; i++) {
				int y = topRow + i * spacing + (spacing - SLOT_SIZE) / 2;
				String id = slotAt(column, i, qRounds);

				boolean isPlayerSlot = Tournament.Bracket.PLAYER_SLOT.equals(id);
				boolean isNext = !data.isSignUp() && !data.isEliminated() && !data.isCompleted()
						&& column == currentRound && isOpponentSlot(column, i, qRounds);

				SlotState state = isPlayerSlot ? SlotState.PLAYER
						: isNext ? SlotState.CURRENT
						: (column < currentRound ? SlotState.DEFEATED : SlotState.UPCOMING);

				drawSlotBackground(graphics, x, y, state);
				if (!id.isEmpty()) renderPortrait(graphics, id, x, y, isPlayerSlot, state == SlotState.DEFEATED);
				drawFrame(graphics, x, y, state);

				if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
					graphics.renderComponentTooltip(this.font, tooltipFor(id), mouseX, mouseY);
				}
			}

			graphics.drawCenteredString(this.font, dmz(columnLabel(column, qRounds)),
					x + SLOT_SIZE / 2, topRow - 16, column == currentRound ? 0xFFFFD700 : 0xFF9FB4FF);
		}
	}

	private void renderConnectors(GuiGraphics graphics, int centreX, int columns, int qRounds,
								  int seedCount, int topRow) {
		int colour = 0xFF6D8CFF;
		int block = seedCount * (SLOT_SIZE + 6);

		for (int column = 0; column < columns - 1; column++) {
			int fromX = columnX(centreX, column, columns) + SLOT_SIZE;
			int toX = columnX(centreX, column + 1, columns);
			int midX = (fromX + toX) / 2;

			int entries = data.isGauntlet() ? 1 : (column <= qRounds ? seedCount >> column : 1);
			int nextEntries = data.isGauntlet() ? 1 : (column + 1 <= qRounds ? seedCount >> (column + 1) : 1);

			int spacing = block / Math.max(1, entries);
			int nextSpacing = block / Math.max(1, nextEntries);

			for (int i = 0; i < entries; i++) {
				int y = topRow + i * spacing + (spacing - SLOT_SIZE) / 2 + SLOT_SIZE / 2;
				int target = entries > nextEntries
						? Math.min(i / 2, nextEntries - 1)
						: Math.min(i, nextEntries - 1);

				int nextY = topRow + target * nextSpacing + (nextSpacing - SLOT_SIZE) / 2 + SLOT_SIZE / 2;

				graphics.fill(fromX, y, midX, y + 1, colour);
				graphics.fill(midX, Math.min(y, nextY), midX + 1, Math.max(y, nextY) + 1, colour);
				graphics.fill(midX, nextY, toX, nextY + 1, colour);
			}
		}
	}

	private String slotAt(int column, int row, int qRounds) {
		if (data.isGauntlet()) {
			if (column < qRounds) {
				List<String> seeds = data.getSeeds();
				return column < seeds.size() ? seeds.get(column) : "";
			}
			return column == qRounds ? data.getSemifinalist() : data.getChampion();
		}

		if (column < qRounds) return entrantAt(column, row);
		if (column == qRounds) {
			List<List<String>> winners = data.getWinners();
			List<String> last = winners.size() >= qRounds ? winners.get(qRounds - 1) : List.of();
			return last.isEmpty() ? "" : last.get(0);
		}
		return column == qRounds + 1 ? data.getSemifinalist() : data.getChampion();
	}

	private boolean isOpponentSlot(int column, int row, int qRounds) {
		if (column >= qRounds) return true;
		int playerPos = playerPosition(column);
		return playerPos >= 0 && row == (playerPos ^ 1);
	}

	private int playerPosition(int round) {
		int seed = data.getSeeds().indexOf(Tournament.Bracket.PLAYER_SLOT);
		return seed < 0 ? -1 : seed >> round;
	}

	private Component columnLabel(int column, int qRounds) {
		if (column < qRounds) {
			return Component.translatable("tournament.dragonminez.round.qualifier", column + 1);
		}
		if (data.isGauntlet()) {
			return column == qRounds
					? Component.translatable("tournament.dragonminez.round.semi")
					: Component.translatable("tournament.dragonminez.round.final");
		}
		if (column == qRounds) return Component.translatable("tournament.dragonminez.round.qualified");
		return column == qRounds + 1
				? Component.translatable("tournament.dragonminez.round.semi")
				: Component.translatable("tournament.dragonminez.round.final");
	}

	private void drawSlotBackground(GuiGraphics graphics, int x, int y, SlotState state) {
		graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE,
				state == SlotState.DEFEATED ? 0xD0080810 : 0xD0101018);
	}

	private void drawFrame(GuiGraphics graphics, int x, int y, SlotState state) {
		if (framesTextureExists == null) {
			framesTextureExists = Minecraft.getInstance().getResourceManager()
					.getResource(SLOT_FRAMES).isPresent();
		}

		graphics.pose().pushPose();
		graphics.pose().translate(0.0F, 0.0F, 300.0F);

		if (framesTextureExists) {
			if (state == SlotState.DEFEATED) graphics.setColor(0.45F, 0.45F, 0.50F, 1.0F);
			graphics.blit(SLOT_FRAMES, x, y, state.u, state.v, SLOT_SIZE, SLOT_SIZE, FRAME_SHEET, FRAME_SHEET);
			if (state == SlotState.DEFEATED) graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		} else {
			graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, switch (state) {
				case PLAYER -> 0xFF55FF55;
				case CURRENT -> 0xFFFF5555;
				case DEFEATED -> 0xFF555565;
				default -> 0xFF6D8CFF;
			});
		}

		graphics.pose().popPose();
	}

	private void renderGauntlet(GuiGraphics graphics, int centreX, int mouseX, int mouseY) {
		List<String> card = new ArrayList<>(data.getSeeds());
		card.add(data.getSemifinalist());
		card.add(data.getChampion());

		int currentRound = data.isSignUp() ? 0 : data.getRound();
		boolean showPlayer = !data.isEliminated();

		List<String> cells = new ArrayList<>();
		List<SlotState> states = new ArrayList<>();
		int versusAfter = -1;

		for (int i = 0; i < card.size(); i++) {
			if (showPlayer && i == currentRound && !data.isCompleted()) {
				versusAfter = cells.size();
				cells.add(Tournament.Bracket.PLAYER_SLOT);
				states.add(SlotState.PLAYER);
			}
			cells.add(card.get(i));
			states.add(i < currentRound ? SlotState.DEFEATED
					: (i == currentRound && !data.isCompleted() ? SlotState.CURRENT : SlotState.UPCOMING));
		}
		if (showPlayer && data.isCompleted()) {
			cells.add(Tournament.Bracket.PLAYER_SLOT);
			states.add(SlotState.PLAYER);
		}

		int step = SLOT_SIZE + 34;
		int top = this.height / 2 - SLOT_SIZE / 2 + Math.round(panY);
		int left = centreX - (cells.size() * step - (step - SLOT_SIZE)) / 2 + Math.round(panX);

		for (int i = 0; i < cells.size(); i++) {
			int x = left + i * step;
			String id = cells.get(i);
			boolean isPlayer = Tournament.Bracket.PLAYER_SLOT.equals(id);

			// A short connector to the next cell, except across the VS gap which reads better bare.
			if (i < cells.size() - 1 && i != versusAfter) {
				int lineY = top + SLOT_SIZE / 2;
				graphics.fill(x + SLOT_SIZE + 4, lineY, x + step - 4, lineY + 1, 0xFF6D8CFF);
			}

			drawSlotBackground(graphics, x, top, states.get(i));
			if (!id.isEmpty()) renderPortrait(graphics, id, x, top, isPlayer, states.get(i) == SlotState.DEFEATED);
			drawFrame(graphics, x, top, states.get(i));

			graphics.drawCenteredString(this.font, dmz(cellLabel(i, versusAfter, card.size())),
					x + SLOT_SIZE / 2, top - 14, states.get(i) == SlotState.CURRENT ? 0xFFFFD700 : 0xFF9FB4FF);

			if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= top && mouseY <= top + SLOT_SIZE) {
				graphics.renderComponentTooltip(this.font, tooltipFor(id), mouseX, mouseY);
			}
		}

		if (versusAfter >= 0) {
			int vsX = left + versusAfter * step + SLOT_SIZE + (step - SLOT_SIZE) / 2;
			graphics.drawCenteredString(this.font, dmz(Component.translatable("tournament.dragonminez.vs")),
					vsX, top + SLOT_SIZE / 2 - 4, 0xFFFF5555);
		}
	}

	private Component cellLabel(int cellIndex, int versusAfter, int cardSize) {
		if (cellIndex == versusAfter) return Component.empty();

		int round = versusAfter >= 0 && cellIndex > versusAfter ? cellIndex - 1 : cellIndex;
		if (round >= cardSize - 1) return Component.translatable("tournament.dragonminez.round.final");
		if (round == cardSize - 2) return Component.translatable("tournament.dragonminez.round.semi");
		return Component.translatable("tournament.dragonminez.round.qualifier", round + 1);
	}

	private List<Component> tooltipFor(String id) {
		List<Component> lines = new ArrayList<>();
		lines.add(dmz(slotName(id)));

		TournamentPackets.OpenBracketS2C.FighterStats stats = data.getStats().get(id);
		if (stats != null) {
			lines.add(Component.translatable("tournament.dragonminez.stat.health", stats.health())
					.withStyle(ChatFormatting.RED));
			lines.add(Component.translatable("tournament.dragonminez.stat.melee", stats.melee())
					.withStyle(ChatFormatting.GOLD));
			lines.add(Component.translatable("tournament.dragonminez.stat.ki", stats.ki())
					.withStyle(ChatFormatting.AQUA));
		}
		return lines;
	}

	private Component slotName(String id) {
		if (Tournament.Bracket.PLAYER_SLOT.equals(id)) {
			return Minecraft.getInstance().player != null
					? Minecraft.getInstance().player.getName().copy()
					: Component.translatable("tournament.dragonminez.you");
		}
		if (id.isEmpty()) return Component.literal("-");

		ResourceLocation location = ResourceLocation.tryParse(id);
		if (location == null) return Component.literal(id);
		EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(location);
		return type != null ? type.getDescription().copy() : Component.literal(id);
	}

	private void renderPortrait(GuiGraphics graphics, String id, int boxX, int boxY, boolean isPlayer, boolean greyed) {
		LivingEntity entity = isPlayer ? Minecraft.getInstance().player : portraitFor(id);
		if (entity == null) return;

		float height = Math.max(0.6F, entity.getBbHeight());
		int scale = Mth.clamp(Math.round((SLOT_SIZE * 1.6F) / height), 8, 140);
		int feetY = boxY + Math.round(height * 0.82F * scale) + SLOT_SIZE / 4;

		graphics.enableScissor(boxX + 1, boxY + 1, boxX + SLOT_SIZE - 1, boxY + SLOT_SIZE - 1);
		if (greyed) {
			renderGreyedEntity(graphics, boxX + SLOT_SIZE / 2, feetY, scale, entity);
		} else {
			InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, boxX + SLOT_SIZE / 2, feetY,
					scale, 0.0F, 0.0F, entity);
		}
		graphics.disableScissor();
	}

	private static void renderGreyedEntity(GuiGraphics graphics, int x, int y, int scale, LivingEntity entity) {
		Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);

		float bodyRot = entity.yBodyRot;
		float yRot = entity.getYRot();
		float xRot = entity.getXRot();
		float headRotO = entity.yHeadRotO;
		float headRot = entity.yHeadRot;

		entity.yBodyRot = 180.0F;
		entity.setYRot(180.0F);
		entity.setXRot(0.0F);
		entity.yHeadRot = 180.0F;
		entity.yHeadRotO = 180.0F;

		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 50.0D);
		graphics.pose().mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
		graphics.pose().mulPose(pose);

		Lighting.setupForEntityInInventory();
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		dispatcher.setRenderShadow(false);

		MultiBufferSource greyed = renderType -> new GreyingVertexConsumer(graphics.bufferSource().getBuffer(renderType));
		RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F,
				graphics.pose(), greyed, 15728880));

		graphics.flush();
		dispatcher.setRenderShadow(true);
		graphics.pose().popPose();
		Lighting.setupFor3DItems();

		entity.yBodyRot = bodyRot;
		entity.setYRot(yRot);
		entity.setXRot(xRot);
		entity.yHeadRotO = headRotO;
		entity.yHeadRot = headRot;
	}

	private static final class GreyingVertexConsumer extends VertexConsumerWrapper {
		private static final float TINT = 0.32F;

		private GreyingVertexConsumer(VertexConsumer parent) {
			super(parent);
		}

		@Override
		public VertexConsumer color(int r, int g, int b, int a) {
			int grey = (int) ((r + g + b) / 3.0F * TINT);
			parent.color(grey, grey, Math.min(255, grey + 8), a);
			return this;
		}
	}

	private static LivingEntity portraitFor(String id) {
		if (id == null || id.isEmpty()) return null;
		if (PORTRAIT_CACHE.containsKey(id)) return PORTRAIT_CACHE.get(id);

		LivingEntity created = null;
		ResourceLocation location = ResourceLocation.tryParse(id);
		Minecraft mc = Minecraft.getInstance();
		if (location != null && mc.level != null) {
			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(location);
			if (type != null) {
				Entity entity = type.create(mc.level);
				if (entity instanceof LivingEntity living) created = living;
				else if (entity != null) entity.discard();
			}
		}
		PORTRAIT_CACHE.put(id, created);
		return created;
	}

	@Override
	public void onClose() {
		PORTRAIT_CACHE.clear();
		super.onClose();
	}

	private static String formatDuration(long seconds) {
		long minutes = seconds / 60L;
		long rest = seconds % 60L;
		return minutes > 0L ? minutes + "m " + rest + "s" : rest + "s";
	}

	private static Component dmz(Component component) {
		return component.copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
