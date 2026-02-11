package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.TrainingRewardC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class TrainingScreen extends Screen {
	private static final ResourceLocation MENU_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/menu/menubig.png");
	private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/menubuttons.png");

	private static final int PANEL_WIDTH = 141;
	private static final int PANEL_HEIGHT = 213;

	private static final int TARGET_Y = 165;
	private static final int HIT_WINDOW = 25;
	private static final int GOAL_PER_ROUND = 25;
	private static final int REWARD_PER_ROUND = 4;
	private static final int MAX_TOTAL_MISSES = 5;

	private static final int[] LANE_ORDER = {1, 2, 0, 3};

	private int[] laneCooldowns = new int[4];

	private int guiLeft, guiTop;
	private boolean inGame = false;
	private String selectedStat = "";

	private final List<FallingArrow> arrows = new ArrayList<>();
	private int score = 0;
	private int totalMisses = 0;
	private int totalHitsSession = 0;
	private float spawnTimer = 0;
	private final Random random = new Random();

	public TrainingScreen() {
		super(Component.translatable("gui.dragonminez.training.title"));
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - PANEL_WIDTH) / 2;
		this.guiTop = (this.height - PANEL_HEIGHT) / 2;

		if (!inGame) {
			initMenuButtons();
		}
	}

	private void initMenuButtons() {
		String[] stats = {"str", "skp", "res", "vit", "pwr", "ene"};
		int startY = guiTop + 40;

		Player player = Minecraft.getInstance().player;
		if (player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			for (int i = 0; i < stats.length; i++) {
				String statKey = stats[i];
				int currentPoints = data.getTraining().getTrainingPoints(statKey);
				int limitPoints = 20;
				boolean canTrain = data.getTraining().canTrain(statKey);

				Component statName = Component.translatable("gui.dragonminez.character_stats." + statKey);
				Component btnText;

				if (canTrain) btnText = Component.translatable("gui.dragonminez.training.stats_format", statName, currentPoints, limitPoints);
				else btnText = Component.translatable("gui.dragonminez.training.wait", statName);

				TexturedTextButton btn = new TexturedTextButton.Builder()
						.position(guiLeft + (PANEL_WIDTH - 105) / 2, startY + (i * 25))
						.size(105, 20)
						.texture(BUTTON_TEXTURE)
						.textureCoords(0, 50 + (i * 21), 0, 50 + (i * 21))
						.textureSize(105, 20)
						.message(btnText)
						.onPress(b -> startGame(statKey))
						.build();

				System.out.println("Texture coords for " + statKey + ": X = " + (0) + ", Y = " + (50 + (i * 21)) + ", Width = " + 80 + ", Height = " + 20);

				btn.active = canTrain;
				this.addRenderableWidget(btn);
			}
		});
	}

	private void startGame(String stat) {
		this.selectedStat = stat;
		this.inGame = true;
		this.score = 0;
		this.totalMisses = 0;
		this.totalHitsSession = 0;
		this.arrows.clear();
		this.clearWidgets();
		for(int i=0; i<4; i++) laneCooldowns[i] = 0;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.blit(MENU_TEXTURE, guiLeft, guiTop, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

		if (inGame) renderGame(graphics, partialTick);
		else {
			drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.training.select"), this.width / 2, guiTop + 18, 0xFFFFD700);
			super.render(graphics, mouseX, mouseY, partialTick);
		}
	}

	private void renderGame(GuiGraphics graphics, float partialTick) {
		Component statName = Component.translatable("gui.dragonminez.character_stats." + selectedStat);
		drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.training.ingame_title", statName), this.width / 2, guiTop + 14, 0xFFFFD700);
		drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.training.progress", score, GOAL_PER_ROUND), guiLeft +70, guiTop + 27, 0x00FF00);
		drawCenteredStringWithBorder(graphics, Component.translatable("gui.dragonminez.training.misses", totalMisses, MAX_TOTAL_MISSES), guiLeft + 70, guiTop + 192, 0xFF5555);

		int gameAreaLeft = guiLeft + 20;
		int gameAreaTop = guiTop + 40;
		int gameAreaWidth = PANEL_WIDTH - 40;
		int gameAreaHeight = 150;
		int laneWidth = gameAreaWidth / 4;

		graphics.fill(gameAreaLeft, gameAreaTop, gameAreaLeft + gameAreaWidth, gameAreaTop + gameAreaHeight, 0x90000000);
		graphics.enableScissor(gameAreaLeft, gameAreaTop, gameAreaLeft + gameAreaWidth, gameAreaTop + gameAreaHeight);
		int targetYAbs = gameAreaTop + (TARGET_Y - 40);

		for (int i = 0; i < 4; i++) {
			int dir = LANE_ORDER[i];
			int cx = gameAreaLeft + (i * laneWidth) + (laneWidth / 2);
			int color = (laneCooldowns[dir] > 0) ? 0xFF999999 : 0xFF444444;
			renderDiamond(graphics, cx, targetYAbs, 8, color, getSymbolForDirection(dir), 0xFFAAAAAA);
		}

		for (FallingArrow arrow : arrows) {
			int laneIndex = getLaneIndex(arrow.direction);
			int arrowX = gameAreaLeft + (laneIndex * laneWidth) + (laneWidth / 2);
			int arrowY = gameAreaTop + (int)arrow.y;

			int color = getColorForDirection(arrow.direction);
			String symbol = getSymbolForDirection(arrow.direction);

			if (arrow.isHold) {
				int tailHeight = (int)arrow.holdLength;
				int tailWidth = 6;
				int drawYBase = arrow.isHolding ? targetYAbs : arrowY;
				int drawYTop = drawYBase - tailHeight;
				int tailColor = color & 0x80FFFFFF;
				graphics.fill(arrowX - tailWidth/2, drawYTop, arrowX + tailWidth/2, drawYBase, tailColor);
			}

			int headY = arrow.isHolding ? targetYAbs : arrowY;
			float scale = arrow.isHolding ? 1.1f : 1.0f;

			renderDiamond(graphics, arrowX, headY, (int)(8 * scale), color, symbol, 0xFFFFFFFF);
		}

		graphics.disableScissor();
	}

	private void renderDiamond(GuiGraphics graphics, int x, int y, int radius, int color, String letter, int textColor) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);

		graphics.pose().pushPose();
		graphics.pose().mulPose(Axis.ZP.rotationDegrees(45));
		graphics.fill(-radius, -radius, radius, radius, color);
		graphics.renderOutline(-radius, -radius, radius*2, radius*2, 0xFF000000);
		graphics.pose().popPose();
		drawCenteredStringWithBorder(graphics, Component.literal(letter), 0, -4, textColor);
		graphics.pose().popPose();
	}

	private int getLaneIndex(int direction) {
		for (int i = 0; i < LANE_ORDER.length; i++) {
			if (LANE_ORDER[i] == direction) return i;
		}
		return 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (!inGame) return;

		for (int i = 0; i < 4; i++) if (laneCooldowns[i] > 0) laneCooldowns[i]--;

		float difficultyMult = 1.0f + (totalHitsSession * 0.01f);
		float currentSpeed = 3.0f * difficultyMult;

		int baseSpawnRate = Math.max(8, 18 - (int)(totalHitsSession * 0.2));

		if (spawnTimer-- <= 0) {
			spawnNotes(currentSpeed);
			spawnTimer = baseSpawnRate;
		}

		int gameAreaHeight = 150;
		Iterator<FallingArrow> it = arrows.iterator();

		while (it.hasNext()) {
			FallingArrow arrow = it.next();

			if (arrow.isHolding) {
				arrow.holdLength -= currentSpeed;

				arrow.holdTickCounter++;
				if (arrow.holdTickCounter >= 8) {
					arrow.holdTickCounter = 0;
					playSuccessSound(true);
				}

				if (arrow.holdLength <= 0) {
					it.remove();
					handleHit(true);
					laneCooldowns[arrow.direction] = 8;
				}
			} else {
				arrow.y += currentSpeed;

				if (arrow.y > gameAreaHeight + 20) {
					it.remove();
					handleMiss();
				}
			}
		}

		if (totalMisses >= MAX_TOTAL_MISSES) endGame(false);
		else if (score >= GOAL_PER_ROUND) {
			boolean holdingAny = arrows.stream().anyMatch(a -> a.isHolding);
			if (!holdingAny) completeRound();
		}
	}

	private void spawnNotes(float currentSpeed) {
		boolean isChord = totalHitsSession > 10 && random.nextInt(100) < 30;
		int notesToSpawn = isChord ? 2 : 1;

		List<Integer> availableLanes = new ArrayList<>();
		for (int i = 0; i < 4; i++) availableLanes.add(i);
		Collections.shuffle(availableLanes);

		for (int i = 0; i < notesToSpawn; i++) {
			int dir = availableLanes.get(i);

			boolean isHold = !isChord && random.nextFloat() < 0.15f;
			float length = isHold ? 40 + random.nextInt(60) : 0;

			arrows.add(new FallingArrow(dir, isHold, length));

			if (isHold) spawnTimer += 5;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!inGame) return super.keyPressed(keyCode, scanCode, modifiers);

		int direction = getDirectionFromKey(keyCode);
		if (direction != -1) {
			if (laneCooldowns[direction] > 0) return true;

			for (FallingArrow arrow : arrows) {
				if (arrow.direction == direction && arrow.isHolding) return true;
			}

			checkInput(direction, true);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (!inGame) return super.keyReleased(keyCode, scanCode, modifiers);

		int direction = getDirectionFromKey(keyCode);
		if (direction != -1) {
			checkInput(direction, false);
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	private int getDirectionFromKey(int keyCode) {
		if (keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_UP) return 0;
		if (keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_LEFT) return 1;
		if (keyCode == GLFW.GLFW_KEY_S || keyCode == GLFW.GLFW_KEY_DOWN) return 2;
		if (keyCode == GLFW.GLFW_KEY_D || keyCode == GLFW.GLFW_KEY_RIGHT) return 3;
		return -1;
	}

	private void checkInput(int direction, boolean isPress) {
		double targetLineRelative = TARGET_Y - 40;

		FallingArrow target = null;
		double minDist = Double.MAX_VALUE;

		for (FallingArrow arrow : arrows) {
			if (arrow.direction == direction) {
				if (isPress && !arrow.isHolding) {
					double dist = Math.abs(arrow.y - targetLineRelative);
					if (dist < HIT_WINDOW && dist < minDist) {
						minDist = dist;
						target = arrow;
					}
				} else if (!isPress && arrow.isHolding) {
					target = arrow;
					break;
				}
			}
		}

		if (isPress) {
			if (target != null) {
				if (target.isHold) {
					target.isHolding = true;
					playSuccessSound(false);
				} else {
					arrows.remove(target);
					handleHit(false);
					laneCooldowns[direction] = 3;
				}
			} else {
				handleMiss();
			}
		} else {
			if (target != null && target.isHolding) {
				arrows.remove(target);
				handleMiss();
				laneCooldowns[direction] = 10;
			}
		}
	}

	private void handleHit(boolean isFinishBonus) {
		score += isFinishBonus ? 2 : 1;
		totalHitsSession++;
		playSuccessSound(false);
	}

	private void handleMiss() {
		score = Math.max(0, score - 2);
		totalMisses++;
		Minecraft.getInstance().getSoundManager().play(
				SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5F, 0.4f)
		);
	}

	private void playSuccessSound(boolean isTick) {
		float pitch = isTick ? 1.5f : 1.0f;
		float vol = isTick ? 0.1f : 0.3f;
		Minecraft.getInstance().getSoundManager().play(
				SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, vol)
		);
	}

	private void completeRound() {
		NetworkHandler.sendToServer(new TrainingRewardC2S(selectedStat, REWARD_PER_ROUND));
		score = 0;

		Player player = Minecraft.getInstance().player;
		if (player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				data.getTraining().addTrainingPoints(selectedStat, REWARD_PER_ROUND);
				if (!data.getTraining().canTrain(selectedStat)) endGame(true);
			});
		}
	}

	private void endGame(boolean success) {
		this.inGame = false;
		this.init();
		Component msg = success ?
				Component.translatable("gui.dragonminez.training.complete") :
				Component.translatable("gui.dragonminez.training.failed");
		Minecraft.getInstance().player.displayClientMessage(msg, true);
	}

	private int getColorForDirection(int dir) {
		return switch (dir) {
			case 0 -> 0x9555FF55;
			case 1 -> 0x9555FFFF;
			case 2 -> 0x95FF5555;
			case 3 -> 0x95FFFF55;
			default -> 0x95FFFFFF;
		};
	}

	private String getSymbolForDirection(int dir) {
		return switch (dir) {
			case 0 -> "W"; case 1 -> "A"; case 2 -> "S"; case 3 -> "D"; default -> "?";
		};
	}

	private void drawStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int textColor) {
		int borderColor = 0xFF000000;
		graphics.drawString(this.font, text, x + 1, y, borderColor, false);
		graphics.drawString(this.font, text, x - 1, y, borderColor, false);
		graphics.drawString(this.font, text, x, y + 1, borderColor, false);
		graphics.drawString(this.font, text, x, y - 1, borderColor, false);
		graphics.drawString(this.font, text, x, y, textColor, false);
	}

	private void drawCenteredStringWithBorder(GuiGraphics graphics, Component text, int centerX, int y, int textColor) {
		int textWidth = this.font.width(text);
		int x = centerX - (textWidth / 2);
		drawStringWithBorder(graphics, text, x, y, textColor);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static class FallingArrow {
		float y = 0;
		int direction;
		boolean isHold;
		float holdLength;
		boolean isHolding = false;
		int holdTickCounter = 0;

		public FallingArrow(int direction, boolean isHold, float length) {
			this.direction = direction;
			this.isHold = isHold;
			this.holdLength = length;
			this.y = -50 - (isHold ? length : 0);
		}
	}
}