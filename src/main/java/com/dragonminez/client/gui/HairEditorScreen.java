package com.dragonminez.client.gui;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.CustomHair.HairFace;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.hair.HairStrand;
import com.dragonminez.common.stats.Character;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HairEditorScreen extends Screen {
    private final Screen previousScreen;
    private final Character character;
    private final CustomHair editingHair;
    private final CustomHair backupHair;
    private HairFace currentFace = HairFace.TOP;
    private int selectedStrandIndex = 0;
    private EditBox codeField;
    private float editStep = 1.0f;

    private EditMode editMode = EditMode.LENGTH;

    public enum EditMode {
        LENGTH,
        POSITION,
        ROTATION,
        CURVE,
        SCALE
    }

    public HairEditorScreen(Screen previousScreen, Character character) {
        super(Component.literal("Hair Editor"));
        this.previousScreen = previousScreen;
        this.character = character;

        if (character.getCustomHair() == null) {
            character.setCustomHair(new CustomHair());
        }

        this.editingHair = character.getCustomHair();

        this.backupHair = editingHair.copy();
    }

    @Override
    protected void init() {
        super.init();
        
        int y = 10;
        int buttonH = 20;
        int smallBtn = 50;
        int spacing = 2;

        int faceX = 10;
        for (HairFace face : HairFace.values()) {
            final HairFace f = face;
            addRenderableWidget(Button.builder(
                Component.literal(face.name()),
                btn -> selectFace(f)
            ).bounds(faceX, y, smallBtn, buttonH).build());
            faceX += smallBtn + spacing;
        }
        
        y += buttonH + 10;
        y += 90;

        int modeX = 10;
        for (EditMode mode : EditMode.values()) {
            final EditMode m = mode;
            addRenderableWidget(Button.builder(
                Component.literal(mode.name().substring(0, 3)),
                btn -> editMode = m
            ).bounds(modeX, y, 45, buttonH).build());
            modeX += 47;
        }

        y += buttonH + 5;

        addRenderableWidget(Button.builder(
            Component.literal("-"),
            btn -> applyEdit(-editStep)
        ).bounds(10, y, 30, buttonH).build());

        addRenderableWidget(Button.builder(
            Component.literal("+"),
            btn -> applyEdit(editStep)
        ).bounds(45, y, 30, buttonH).build());

        addRenderableWidget(Button.builder(
            Component.literal("X-"),
            btn -> applyEditAxis(-editStep, 0, 0)
        ).bounds(90, y, 30, buttonH).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("X+"),
            btn -> applyEditAxis(editStep, 0, 0)
        ).bounds(122, y, 30, buttonH).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Y-"),
            btn -> applyEditAxis(0, -editStep, 0)
        ).bounds(160, y, 30, buttonH).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Y+"),
            btn -> applyEditAxis(0, editStep, 0)
        ).bounds(192, y, 30, buttonH).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Z-"),
            btn -> applyEditAxis(0, 0, -editStep)
        ).bounds(230, y, 30, buttonH).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Z+"),
            btn -> applyEditAxis(0, 0, editStep)
        ).bounds(262, y, 30, buttonH).build());

        y += buttonH + 5;

        addRenderableWidget(Button.builder(
            Component.literal("0.5"),
            btn -> editStep = 0.5f
        ).bounds(10, y, 35, buttonH).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("1"),
            btn -> editStep = 1.0f
        ).bounds(47, y, 25, buttonH).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("5"),
            btn -> editStep = 5.0f
        ).bounds(74, y, 25, buttonH).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("15"),
            btn -> editStep = 15.0f
        ).bounds(101, y, 30, buttonH).build());

        y += buttonH + 10;

        // ===== FILA 6: IMPORT/EXPORT =====
        codeField = new EditBox(this.font, 10, y, 180, buttonH, Component.literal("Code"));
        codeField.setMaxLength(65536);
        addRenderableWidget(codeField);

        addRenderableWidget(Button.builder(
            Component.literal("Export"),
            btn -> exportCode()
        ).bounds(195, y, 50, buttonH).build());

        addRenderableWidget(Button.builder(
            Component.literal("Import"),
            btn -> importCode()
        ).bounds(247, y, 50, buttonH).build());

        int bottomY = this.height - 30;
        int centerX = this.width / 2;

        addRenderableWidget(Button.builder(
            Component.literal("Save & Close"),
            btn -> saveAndClose()
        ).bounds(centerX - 110, bottomY, 100, buttonH).build());

        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            btn -> cancelAndClose()
        ).bounds(centerX + 10, bottomY, 100, buttonH).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        renderStrandGrid(graphics, mouseX, mouseY);
        renderStrandInfo(graphics);
        renderGeneralInfo(graphics);
    }

    private void renderStrandGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int gridX = 10;
        int gridY = 55;
        int cellSize = 20;
        int spacing = 2;

        HairStrand[] strands = editingHair.getStrands(currentFace);
        if (strands == null) return;

        int rows = currentFace.rows;
        int cols = currentFace.cols;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                if (index >= strands.length) continue;

                HairStrand strand = strands[index];
                int x = gridX + col * (cellSize + spacing);
                int y = gridY + row * (cellSize + spacing);

                int bgColor;
                if (index == selectedStrandIndex) {
                    bgColor = 0xFF00FF00;
                } else if (strand.isVisible()) {
                    bgColor = 0xFFFFFF00;
                } else {
                    bgColor = 0xFF444444;
                }

                graphics.fill(x, y, x + cellSize, y + cellSize, bgColor);
                graphics.fill(x + 1, y + 1, x + cellSize - 1, y + cellSize - 1, 0xFF222222);

                String lengthStr = String.valueOf(strand.getLength());
                int textX = x + (cellSize - font.width(lengthStr)) / 2;
                int textY = y + (cellSize - font.lineHeight) / 2;
                graphics.drawString(font, lengthStr, textX, textY, strand.isVisible() ? 0xFFFFFF : 0x666666);

                if (mouseX >= x && mouseX < x + cellSize && mouseY >= y && mouseY < y + cellSize) {
                    if (Minecraft.getInstance().mouseHandler.isLeftPressed()) {
                        selectedStrandIndex = index;
                    }
                }
            }
        }

        graphics.drawString(font, "Face: " + currentFace.name(), gridX, gridY - 12, 0xFFFFFF);
    }

    private void renderStrandInfo(GuiGraphics graphics) {
        int infoX = this.width - 140;
        int infoY = 10;

        HairStrand strand = getSelectedStrand();
        if (strand == null) {
            graphics.drawString(font, "No strand selected", infoX, infoY, 0xFF5555);
            return;
        }

        graphics.drawString(font, "Strand #" + selectedStrandIndex, infoX, infoY, 0x00FFFF);
        infoY += 12;

        graphics.drawString(font, "Length: " + strand.getLength(), infoX, infoY, 0xFFFFFF);
        infoY += 12;

        graphics.drawString(font, String.format("Pos: %.1f, %.1f, %.1f", 
            strand.getOffsetX(), strand.getOffsetY(), strand.getOffsetZ()), infoX, infoY, 0xAAAAAA);
        infoY += 12;

        graphics.drawString(font, String.format("Rot: %.1f, %.1f, %.1f",
            strand.getRotationX(), strand.getRotationY(), strand.getRotationZ()), infoX, infoY, 0xAAAAAA);
        infoY += 12;

        graphics.drawString(font, String.format("Curve: %.1f, %.1f, %.1f",
            strand.getCurveX(), strand.getCurveY(), strand.getCurveZ()), infoX, infoY, 0xAAAAAA);
        infoY += 12;

        graphics.drawString(font, String.format("Scale: %.2f, %.2f, %.2f",
            strand.getScaleX(), strand.getScaleY(), strand.getScaleZ()), infoX, infoY, 0xAAAAAA);
        infoY += 20;

        graphics.drawString(font, "Mode: " + editMode.name(), infoX, infoY, 0x00FF00);
        infoY += 12;

        graphics.drawString(font, "Step: " + editStep, infoX, infoY, 0x00FF00);
    }

    private void renderGeneralInfo(GuiGraphics graphics) {
        int infoX = this.width - 140;
        int infoY = this.height - 80;

        graphics.drawString(font, "Visible: " + editingHair.getVisibleStrandCount(), infoX, infoY, 0xAAAAAA);
        infoY += 12;

        graphics.drawString(font, "Cubes: " + editingHair.getTotalCubeCount(), infoX, infoY, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gridX = 10;
        int gridY = 55;
        int cellSize = 20;
        int spacing = 2;

        int rows = currentFace.rows;
        int cols = currentFace.cols;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                int x = gridX + col * (cellSize + spacing);
                int y = gridY + row * (cellSize + spacing);

                if (mouseX >= x && mouseX < x + cellSize && mouseY >= y && mouseY < y + cellSize) {
                    selectedStrandIndex = index;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectFace(HairFace face) {
        currentFace = face;
        selectedStrandIndex = 0;
    }

    private HairStrand getSelectedStrand() {
        return editingHair.getStrand(currentFace, selectedStrandIndex);
    }

    private void applyEdit(float delta) {
        HairStrand strand = getSelectedStrand();
        if (strand == null) return;

        switch (editMode) {
            case LENGTH -> {
                if (delta > 0) strand.addCube();
                else strand.removeCube();
            }
            case SCALE -> strand.setScale(
                strand.getScaleX() + delta * 0.1f,
                strand.getScaleY() + delta * 0.1f,
                strand.getScaleZ() + delta * 0.1f
            );
            default -> {}
        }
    }

    private void applyEditAxis(float dx, float dy, float dz) {
        HairStrand strand = getSelectedStrand();
        if (strand == null) return;

        switch (editMode) {
            case POSITION -> strand.addOffset(dx, dy, dz);
            case ROTATION -> strand.addRotation(dx * 10, dy * 10, dz * 10);
            case CURVE -> strand.setCurve(
                strand.getCurveX() + dx,
                strand.getCurveY() + dy,
                strand.getCurveZ() + dz
            );
            case SCALE -> strand.setScale(
                strand.getScaleX() + dx * 0.1f,
                strand.getScaleY() + dy * 0.1f,
                strand.getScaleZ() + dz * 0.1f
            );
            default -> {}
        }
    }

    private void exportCode() {
        String code = HairManager.toCode(editingHair);
        if (code != null) {
            codeField.setValue(code);
            Minecraft.getInstance().keyboardHandler.setClipboard(code);
        }
    }

    private void importCode() {
        String code = codeField.getValue();
        if (code.isEmpty()) {
            code = Minecraft.getInstance().keyboardHandler.getClipboard();
        }

        CustomHair imported = HairManager.fromCode(code);
        if (imported != null) {
            copyHairData(imported, editingHair);
            selectedStrandIndex = 0;
        }
    }

    private void copyHairData(CustomHair source, CustomHair dest) {
        dest.setGlobalColor(source.getGlobalColor());
        dest.setName(source.getName());
        
        for (HairFace face : HairFace.values()) {
            HairStrand[] srcStrands = source.getStrands(face);
            HairStrand[] dstStrands = dest.getStrands(face);
            for (int i = 0; i < srcStrands.length && i < dstStrands.length; i++) {
                HairStrand src = srcStrands[i];
                HairStrand dst = dstStrands[i];
                dst.setLength(src.getLength());
                dst.setOffset(src.getOffsetX(), src.getOffsetY(), src.getOffsetZ());
                dst.setRotation(src.getRotationX(), src.getRotationY(), src.getRotationZ());
                dst.setCurve(src.getCurveX(), src.getCurveY(), src.getCurveZ());
                dst.setScale(src.getScaleX(), src.getScaleY(), src.getScaleZ());
                dst.setColor(src.getColor());
            }
        }
    }

    private void saveAndClose() {
        character.setHairId(0);
        Minecraft.getInstance().setScreen(previousScreen);
    }

    private void cancelAndClose() {
        copyHairData(backupHair, editingHair);
        Minecraft.getInstance().setScreen(previousScreen);
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
