package com.dragonminez.client.gui.quest;

import com.dragonminez.Reference;
import com.dragonminez.client.util.LocalizationUtil;
import com.dragonminez.common.network.C2S.DialogueChoiceC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenDialogueNodeS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class DialogueScreen extends Screen {

	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private final String npcId;
	private final int entityId;
	private String nodeId;
	private String line;
	private List<String> choiceTexts;
	private List<Integer> choiceIndices;

	private DialogueScreen(OpenDialogueNodeS2C msg) {
		super(Component.literal("Dialogue"));
		this.npcId = msg.getNpcId();
		this.entityId = msg.getEntityId();
		applyNode(msg);
	}

	public static void handleNodePacket(OpenDialogueNodeS2C msg) {
		Minecraft mc = Minecraft.getInstance();
		if (msg.isClose()) {
			if (mc.screen instanceof DialogueScreen) mc.setScreen(null);
			return;
		}
		if (mc.screen instanceof DialogueScreen current && current.npcId.equals(msg.getNpcId())) {
			current.applyNode(msg);
			current.rebuildWidgets();
		} else {
			mc.setScreen(new DialogueScreen(msg));
		}
	}

	private void applyNode(OpenDialogueNodeS2C msg) {
		this.nodeId = msg.getNodeId();
		this.line = msg.getLine();
		this.choiceTexts = msg.getChoiceTexts();
		this.choiceIndices = msg.getChoiceIndices();
	}

	@Override
	protected void init() {
		int buttonWidth = Math.min(300, this.width - 40);
		int buttonHeight = 18;
		int spacing = 4;
		int count = choiceTexts.size();
		int panelTop = panelTop();
		int startY = panelTop - (count * (buttonHeight + spacing)) - 6;

		for (int i = 0; i < count; i++) {
			int originalIndex = choiceIndices.get(i);
			Component label = dmz(LocalizationUtil.localizedOrReadable(choiceTexts.get(i)).copy());
			this.addRenderableWidget(Button.builder(label, button ->
							NetworkHandler.sendToServer(new DialogueChoiceC2S(npcId, entityId, nodeId, originalIndex)))
					.bounds((this.width - buttonWidth) / 2, startY + i * (buttonHeight + spacing), buttonWidth, buttonHeight)
					.build());
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(guiGraphics);

		int panelTop = panelTop();
		guiGraphics.fill(0, panelTop, this.width, this.height, 0xC0101018);
		guiGraphics.fill(0, panelTop, this.width, panelTop + 1, 0xCC6D8CFF);

		int textX = 20;
		int textWidth = this.width - 40;
		guiGraphics.drawString(this.font, npcName(), textX, panelTop + 8, 0xFFD700, true);

		Component lineComponent = dmz(LocalizationUtil.localizedOrReadable(line).copy());
		int lineY = panelTop + 22;
		for (FormattedCharSequence sequence : this.font.split(lineComponent, textWidth)) {
			guiGraphics.drawString(this.font, sequence, textX, lineY, 0xE8F0FF, false);
			lineY += this.font.lineHeight + 2;
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private int panelTop() {
		return this.height - 70;
	}

	private Component npcName() {
		String questNpcKey = "entity.dragonminez.questnpc." + npcId;
		if (I18n.exists(questNpcKey)) return dmz(Component.translatable(questNpcKey));
		String masterKey = "gui.dragonminez.lines." + npcId + ".name";
		if (I18n.exists(masterKey)) return dmz(Component.translatable(masterKey));
		return dmz(Component.literal(npcId));
	}

	private static Component dmz(MutableComponent component) {
		return component.withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
