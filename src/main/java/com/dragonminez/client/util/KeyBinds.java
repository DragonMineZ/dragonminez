package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Modifier;

public class KeyBinds {

	private static final String DMZ_CATEGORY = "key.categories." + Reference.MOD_ID;
	private static final String MINIGAMES_CATEGORY = "key.categories.minigames." + Reference.MOD_ID;

	public static final KeyMapping STATS_MENU = registerKey("stats_menu", GLFW.GLFW_KEY_V);
	public static final KeyMapping KI_CHARGE = registerKey("ki_charge", GLFW.GLFW_KEY_C);
	public static final KeyMapping SECOND_FUNCTION_KEY = registerKey("second_function_key", GLFW.GLFW_KEY_LEFT_ALT);
	public static final KeyMapping ACTION_KEY = registerKey("action_key", GLFW.GLFW_KEY_G);
	public static final KeyMapping INSTANT_TRANSMISSION = registerKey("instant_transmission", GLFW.GLFW_KEY_H);
	public static final KeyMapping SPACEPOD_MENU = registerKey("spacepod_menu", GLFW.GLFW_KEY_H);
	public static final KeyMapping UTILITY_MENU = registerKey("utility_menu", GLFW.GLFW_KEY_X);
	public static final KeyMapping LOCK_ON = registerKey("lock_on", GLFW.GLFW_KEY_Z);
	public static final KeyMapping KI_SENSE = registerKey("ki_sense", GLFW.GLFW_KEY_F4);
	public static final KeyMapping FLY_KEY = registerKey("fly_key", GLFW.GLFW_KEY_F);
	public static final KeyMapping DASH_KEY = registerKey("dash_key", GLFW.GLFW_KEY_R);
	public static final KeyMapping BLOCK_KEY = registerMouse("block_key", GLFW.GLFW_MOUSE_BUTTON_RIGHT);

	public static final KeyMapping TECHNIQUE_SLOT_1 = registerKeyAlt("technique_slot_1", GLFW.GLFW_KEY_1);
	public static final KeyMapping TECHNIQUE_SLOT_2 = registerKeyAlt("technique_slot_2", GLFW.GLFW_KEY_2);
	public static final KeyMapping TECHNIQUE_SLOT_3 = registerKeyAlt("technique_slot_3", GLFW.GLFW_KEY_3);
	public static final KeyMapping TECHNIQUE_SLOT_4 = registerKeyAlt("technique_slot_4", GLFW.GLFW_KEY_4);
	public static final KeyMapping TECHNIQUE_SLOT_5 = registerKeyAlt("technique_slot_5", GLFW.GLFW_KEY_5);

	public static final KeyMapping[] TECHNIQUE_SLOTS = {
			TECHNIQUE_SLOT_1, TECHNIQUE_SLOT_2, TECHNIQUE_SLOT_3, TECHNIQUE_SLOT_4, TECHNIQUE_SLOT_5
	};

	public static final KeyMapping RHYTHM_LEFT = registerKey("rhythm_left", GLFW.GLFW_KEY_LEFT, true);
	public static final KeyMapping RHYTHM_DOWN = registerKey("rhythm_down", GLFW.GLFW_KEY_DOWN, true);
	public static final KeyMapping RHYTHM_UP = registerKey("rhythm_up", GLFW.GLFW_KEY_UP, true);
	public static final KeyMapping RHYTHM_RIGHT = registerKey("rhythm_right", GLFW.GLFW_KEY_RIGHT, true);

	private static KeyMapping registerKey(String name, int keyCode) {
		return registerKey(name, keyCode, false);
	}

    private static KeyMapping registerKey(String name, int keyCode, boolean minigame) {
        return new KeyMapping(
                "key." + Reference.MOD_ID + "." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                keyCode,
				minigame ? MINIGAMES_CATEGORY : DMZ_CATEGORY
        );
    }

	private static KeyMapping registerKeyAlt(String name, int keyCode) {
		return new KeyMapping(
				"key." + Reference.MOD_ID + "." + name,
				KeyConflictContext.IN_GAME,
				KeyModifier.ALT,
				InputConstants.Type.KEYSYM.getOrCreate(keyCode),
				DMZ_CATEGORY
		);
	}

	private static KeyMapping registerMouse(String name, int keyCode) {
		return registerMouse(name, keyCode, false);
	}

	private static KeyMapping registerMouse(String name, int keyCode, boolean minigame) {
		return new KeyMapping(
				"key." + Reference.MOD_ID + "." + name,
				KeyConflictContext.IN_GAME,
				InputConstants.Type.MOUSE,
				keyCode,
				minigame ? MINIGAMES_CATEGORY : DMZ_CATEGORY
		);
	}

    public static void registerAll(RegisterKeyMappingsEvent event) {
        try {
            for (var field : KeyBinds.class.getDeclaredFields()) {
                if (field.getType() == KeyMapping.class && Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    KeyMapping keyMapping = (KeyMapping) field.get(null);
                    if (keyMapping != null) {
                        event.register(keyMapping);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to register key bindings", e);
        }
    }
}
