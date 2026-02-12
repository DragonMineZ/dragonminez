package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public class MasterTextScreen extends Screen {
	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/buttons/characterbuttons.png");
	private static final ResourceLocation MENU_TEXT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID,
			"textures/gui/menu/textmenu.png");
	private final String masterName;
	private Component currentDialogue;
	private boolean secondFunc = false;
	private boolean thirdFunc = false;

	public MasterTextScreen(String masterName) {
		super(Component.literal(masterName));
		this.masterName = masterName;
		this.currentDialogue = Component.translatable("gui.dragonminez.lines." + masterName + ".main", Minecraft.getInstance().player.getName());
	}

	@Override
	protected void init() {
		super.init();
		int buttonX = this.width / 2 - 105;
		int buttonY = this.height - 23;

		StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(stats -> {
			switch (masterName) {
				case "karin" -> initKarin(buttonX, buttonY, stats);
				case "guru" -> initGuru(buttonX, buttonY, stats);
				case "dende" -> initDende(buttonX, buttonY, stats);
				case "enma" -> initEnma(buttonX, buttonY, stats);
				case "baba" -> initBaba(buttonX, buttonY, stats);
				case "popo" -> initPopo(buttonX, buttonY, stats);
				case "gero" -> initGero(buttonX, buttonY, stats);
				case "toribot" -> initToribot(buttonX, buttonY, stats);
			}
		});
	}

	private void initKarin(int x, int y, StatsData stats) {
		if (!Minecraft.getInstance().player.getInventory().contains(new ItemStack(MainItems.NUBE_ITEM.get())) &&
				!Minecraft.getInstance().player.getInventory().contains(new ItemStack(MainItems.NUBE_NEGRA_ITEM.get()))) {

			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.karin.nimbus"))
					.onPress(b -> {
						NetworkHandler.sendToServer(new NPCActionC2S("karin", 1));
						this.onClose();
					})
					.build());
		}

		if (!stats.getCooldowns().hasCooldown(Cooldowns.SENZU_KARIN)) {
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x + 150, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.karin.senzu"))
					.onPress(b -> {
						NetworkHandler.sendToServer(new NPCActionC2S("karin", 2));
						this.onClose();
					})
					.build());
		}
	}

	private void initGuru(int x, int y, StatsData stats) {
		this.addRenderableWidget(new TexturedTextButton.Builder()
				.position(x, y)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(Component.translatable("gui.dragonminez.button.guru.unlock_potential"))
				.onPress(b -> {
					if (stats.getResources().getAlignment() <= 50) {
						this.currentDialogue = Component.translatable("gui.dragonminez.lines.guru.evil");
					} else if (stats.getSkills().getSkillLevel("potentialunlock") < 10) {
						this.currentDialogue = Component.translatable("gui.dragonminez.lines.guru.level");
					} else if (stats.getSkills().getSkillLevel("potentialunlock") == 10){
						NetworkHandler.sendToServer(new NPCActionC2S("guru", 1));
						this.onClose();
					}
				})
				.build());
	}

	private void initDende(int x, int y, StatsData stats) {
		this.addRenderableWidget(new TexturedTextButton.Builder()
				.position(x, y)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(Component.translatable("gui.dragonminez.button.dende.heal"))
				.onPress(b -> {
					NetworkHandler.sendToServer(new NPCActionC2S("dende", 1));
					this.onClose();
				})
				.build());

		this.addRenderableWidget(new TexturedTextButton.Builder()
				.position(x + 150, y)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(Component.translatable("gui.dragonminez.button.dende.reset"))
				.onPress(b -> {
					NetworkHandler.sendToServer(new NPCActionC2S("dende", 2));
					this.onClose();
				})
				.build());
	}

	private void initEnma(int x, int y, StatsData stats) {
		boolean hasCd = stats.getCooldowns().hasCooldown(Cooldowns.REVIVE);
		this.currentDialogue = Component.translatable("gui.dragonminez.lines.enma.main", Minecraft.getInstance().player.getName());

		this.addRenderableWidget(new TexturedTextButton.Builder()
				.position(x, y)
				.size(74, 20)
				.texture(BUTTONS_TEXTURE)
				.textureCoords(0, 28, 0, 48)
				.textureSize(74, 20)
				.message(Component.translatable("gui.dragonminez.button.enma.earth"))
				.onPress(b -> {
					if (hasCd) {
						this.currentDialogue = Component.translatable("gui.dragonminez.lines.enma.revive", Minecraft.getInstance().player.getName());
					} else {
						NetworkHandler.sendToServer(new NPCActionC2S("enma", 1));
						this.onClose();
					}
				})
				.build());
	}

	private void initBaba(int x, int y, StatsData stats) {
		boolean hasCd = stats.getCooldowns().hasCooldown(Cooldowns.REVIVE);
		int cdTime = hasCd ? (int) stats.getCooldowns().getCooldown(Cooldowns.REVIVE) / 20 : 0;
		this.currentDialogue = Component.translatable("gui.dragonminez.lines.baba.main", Minecraft.getInstance().player.getName(), cdTime);

		if (!hasCd) {
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.baba.revive"))
					.onPress(b -> {
						NetworkHandler.sendToServer(new NPCActionC2S("baba", 1));
						this.onClose();
					})
					.build());
		}
	}

	private void initPopo(int x, int y, StatsData stats) {
		boolean HTC = Minecraft.getInstance().player.level().dimension().equals(HTCDimension.HTC_KEY);

		if (secondFunc) {
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.popo.shadow"))
					.onPress(btn -> {
						NetworkHandler.sendToServer(new NPCActionC2S("popo", 1));
						secondFunc = false;
						this.onClose();
					})
					.build());
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x + 150, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.popo.rythm"))
					.onPress(btn -> {
						if (Minecraft.getInstance().player.level().isClientSide()) {
							Minecraft.getInstance().setScreen(new TrainingScreen());
						}
					})
					.build());
		} else {
			if (HTC) {
				this.addRenderableWidget(new TexturedTextButton.Builder()
						.position(x, y)
						.size(74, 20)
						.texture(BUTTONS_TEXTURE)
						.textureCoords(0, 28, 0, 48)
						.textureSize(74, 20)
						.message(Component.translatable("gui.dragonminez.button.popo.train"))
						.onPress(btn -> {
							secondFunc = true;
							this.currentDialogue = Component.translatable("gui.dragonminez.lines.popo.training", Minecraft.getInstance().player.getName());
							refreshButtons();
						})
						.build());
			} else {
				this.addRenderableWidget(new TexturedTextButton.Builder()
						.position(x, y)
						.size(74, 20)
						.texture(BUTTONS_TEXTURE)
						.textureCoords(0, 28, 0, 48)
						.textureSize(74, 20)
						.message(Component.translatable("gui.dragonminez.button.popo.haircut"))
						.onPress(btn -> {
							if (Minecraft.getInstance().player.level().isClientSide()) {
								Minecraft.getInstance().setScreen(new HairEditorScreen(null, stats.getCharacter()));
							}
						})
						.build());
			}
		}
	}

	private void initGero(int x, int y, StatsData stats) {
		if (thirdFunc) {
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x + 150, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.gero.cancel"))
					.onPress(btn -> {
						thirdFunc = false;
						secondFunc = false;
						this.onClose();
					})
					.build());
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.gero.confirm"))
					.onPress(btn -> {
						NetworkHandler.sendToServer(new NPCActionC2S("gero", 1));
						thirdFunc = false;
						secondFunc = false;
						this.onClose();
					})
					.build());
		} else if (secondFunc) {
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x + 150, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.gero.not_interested"))
					.onPress(btn -> {
						thirdFunc = false;
						secondFunc = false;
						this.onClose();
					})
					.build());
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.gero.interest"))
					.onPress(btn -> {
						thirdFunc = true;
						secondFunc = false;
						this.currentDialogue = Component.translatable("gui.dragonminez.lines.gero.confirm", Minecraft.getInstance().player.getName());
						refreshButtons();
					})
					.build());
		} else {
			this.addRenderableWidget(new TexturedTextButton.Builder()
					.position(x + 150, y)
					.size(74, 20)
					.texture(BUTTONS_TEXTURE)
					.textureCoords(0, 28, 0, 48)
					.textureSize(74, 20)
					.message(Component.translatable("gui.dragonminez.button.gero.accept"))
					.onPress(btn -> {
						secondFunc = true;
						this.currentDialogue = Component.translatable("gui.dragonminez.lines.gero.offer", Minecraft.getInstance().player.getName());
						refreshButtons();
					})
					.build());
		}
	}

	private void initToribot(int x, int y, StatsData stats) {}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int centerX = (this.width / 2);
		int centerY = (this.height);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, MENU_TEXT);


		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

		buffer.vertex(centerX - 140, centerY + 250, 0.0D).uv(0.0F, 1.0F).endVertex();
		buffer.vertex(centerX + 140, centerY + 250, 0.0D).uv(1.0F, 1.0F).endVertex();
		buffer.vertex(centerX + 140, centerY - 90, 0.0D).uv(1.0F, 0.0F).endVertex();
		buffer.vertex(centerX - 140, centerY - 90, 0.0D).uv(0.0F, 0.0F).endVertex();
		Tesselator.getInstance().end();

		RenderSystem.disableBlend();

		drawStringWithBorder(graphics, Component.translatable("gui.dragonminez.lines." + masterName + ".name").withStyle(ChatFormatting.BOLD), centerX - 120, centerY - 87, 0xFFFFFF);

		int maxTextWidth = 230;
		int textY = centerY - 74;
		var splitLines = this.font.split(currentDialogue, maxTextWidth);
		for (var line : splitLines) {
			drawStringWithBorder(graphics, line, centerX - 120, textY, 0xFFFFFF);
			textY += this.font.lineHeight + 2;
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void refreshButtons() {
		this.clearWidgets();
		this.init();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawStringWithBorder(GuiGraphics graphics, FormattedCharSequence text, int x, int y, int textColor) {
		int borderColor = 0xFF000000;
		graphics.drawString(this.font, text, x + 1, y, borderColor, false);
		graphics.drawString(this.font, text, x - 1, y, borderColor, false);
		graphics.drawString(this.font, text, x, y + 1, borderColor, false);
		graphics.drawString(this.font, text, x, y - 1, borderColor, false);
		graphics.drawString(this.font, text, x, y, textColor, false);
	}

	private void drawStringWithBorder(GuiGraphics graphics, Component text, int x, int y, int textColor) {
		int borderColor = 0xFF000000;
		graphics.drawString(this.font, text, x + 1, y, borderColor, false);
		graphics.drawString(this.font, text, x - 1, y, borderColor, false);
		graphics.drawString(this.font, text, x, y + 1, borderColor, false);
		graphics.drawString(this.font, text, x, y - 1, borderColor, false);
		graphics.drawString(this.font, text, x, y, textColor, false);
	}
}