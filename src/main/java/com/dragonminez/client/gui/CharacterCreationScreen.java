package com.dragonminez.client.gui;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.CreateCharacterC2S;
import com.dragonminez.common.stats.Character;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CharacterCreationScreen extends Screen {

    private int selectedRace = 0;
    private String selectedClass = Character.CLASS_WARRIOR;
    private String selectedGender = Character.GENDER_MALE;

    private Button raceButton;
    private Button classButton;
    private Button genderButton;
    private Button confirmButton;

    public CharacterCreationScreen() {
        super(Component.translatable("gui.dragonminez.character_creation.title"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;

        raceButton = Button.builder(
                Component.translatable("gui.dragonminez.character_creation.race", getRaceName()),
                button -> cycleRace()
        ).bounds(centerX - 100, startY, 200, 20).build();

        classButton = Button.builder(
                Component.translatable("gui.dragonminez.character_creation.class", getClassName()),
                button -> cycleClass()
        ).bounds(centerX - 100, startY + 30, 200, 20).build();

        genderButton = Button.builder(
                Component.translatable("gui.dragonminez.character_creation.gender", getGenderName()),
                button -> cycleGender()
        ).bounds(centerX - 100, startY + 60, 200, 20).build();

        confirmButton = Button.builder(
                Component.translatable("gui.dragonminez.character_creation.confirm"),
                button -> confirmCreation()
        ).bounds(centerX - 100, startY + 100, 200, 20).build();

        addRenderableWidget(raceButton);
        addRenderableWidget(classButton);
        addRenderableWidget(genderButton);
        addRenderableWidget(confirmButton);

        updateGenderButton();
    }

    private void cycleRace() {
        selectedRace = (selectedRace + 1) % Character.RACE_NAMES.length;
        raceButton.setMessage(Component.translatable("gui.dragonminez.character_creation.race", getRaceName()));
        updateGenderButton();
    }

    private void cycleClass() {
        if (selectedClass.equals(Character.CLASS_WARRIOR)) {
            selectedClass = Character.CLASS_SPIRITUALIST;
        } else if (selectedClass.equals(Character.CLASS_SPIRITUALIST)) {
            selectedClass = Character.CLASS_MARTIALARTIST;
        } else {
            selectedClass = Character.CLASS_WARRIOR;
        }
        classButton.setMessage(Component.translatable("gui.dragonminez.character_creation.class", getClassName()));
    }

    private void cycleGender() {
        if (!canHaveGender()) return;

        selectedGender = selectedGender.equals(Character.GENDER_MALE)
                ? Character.GENDER_FEMALE
                : Character.GENDER_MALE;
        genderButton.setMessage(Component.translatable("gui.dragonminez.character_creation.gender", getGenderName()));
    }

    private void updateGenderButton() {
        genderButton.active = canHaveGender();
        genderButton.setMessage(Component.translatable("gui.dragonminez.character_creation.gender", getGenderName()));
    }

    private boolean canHaveGender() {
        return selectedRace >= 0 && selectedRace < Character.HAS_GENDER.length
                && Character.HAS_GENDER[selectedRace];
    }

    private String getRaceName() {
        return Component.translatable("race.dragonminez." + Character.RACE_NAMES[selectedRace]).getString();
    }

    private String getClassName() {
        return Component.translatable("class.dragonminez." + selectedClass).getString();
    }

    private String getGenderName() {
        if (!canHaveGender()) {
            return Component.translatable("gui.dragonminez.character_creation.gender_na").getString();
        }
        return Component.translatable("gender.dragonminez." + selectedGender).getString();
    }

    private void confirmCreation() {
        NetworkHandler.sendToServer(new CreateCharacterC2S(selectedRace, selectedClass, selectedGender));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

