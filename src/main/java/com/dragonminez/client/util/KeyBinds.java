package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Modifier;

public class KeyBinds {

	private static final String DMZ_CATEGORY = "key.categories." + Reference.MOD_ID;
	private static final String MINIGAMES_CATEGORY = "key.categories.minigames." + Reference.MOD_ID;

	public static final KeyMapping STATS_MENU = registerDMZKey("stats_menu", GLFW.GLFW_KEY_V);
	public static final KeyMapping KI_CHARGE = registerDMZKey("ki_charge", GLFW.GLFW_KEY_C);
	public static final KeyMapping SECOND_FUNCTION_KEY = registerDMZKey("second_function_key", GLFW.GLFW_KEY_LEFT_ALT);
	public static final KeyMapping ACTION_KEY = registerDMZKey("action_key", GLFW.GLFW_KEY_G);
	public static final KeyMapping INSTANT_TRANSMISSION = registerDMZKey("instant_transmission", GLFW.GLFW_KEY_H);
	public static final KeyMapping SPACEPOD_MENU = registerDMZKey("spacepod_menu", GLFW.GLFW_KEY_H);
	public static final KeyMapping UTILITY_MENU = registerDMZKey("utility_menu", GLFW.GLFW_KEY_X);
	public static final KeyMapping LOCK_ON = registerDMZKey("lock_on", GLFW.GLFW_KEY_Z);
	public static final KeyMapping KI_SENSE = registerDMZKey("ki_sense", GLFW.GLFW_KEY_F4);
	public static final KeyMapping FLY_KEY = registerDMZKey("fly_key", GLFW.GLFW_KEY_F);
	public static final KeyMapping DASH_KEY = registerDMZKey("dash_key", GLFW.GLFW_KEY_R);
	public static final KeyMapping BLOCK_KEY = registerDMZMouse("block_key", GLFW.GLFW_MOUSE_BUTTON_RIGHT);

	public static final KeyMapping RHYTHM_LEFT = registerDMZKey("rhythm_left", GLFW.GLFW_KEY_LEFT, true);
	public static final KeyMapping RHYTHM_DOWN = registerDMZKey("rhythm_down", GLFW.GLFW_KEY_DOWN, true);
	public static final KeyMapping RHYTHM_UP = registerDMZKey("rhythm_up", GLFW.GLFW_KEY_UP, true);
	public static final KeyMapping RHYTHM_RIGHT = registerDMZKey("rhythm_right", GLFW.GLFW_KEY_RIGHT, true);

	private static KeyMapping registerDMZKey(String name, int keyCode) {
		return registerDMZKey(name, keyCode, false);
	}

    private static KeyMapping registerDMZKey(String name, int keyCode, boolean minigame) {
        return new KeyMapping(
                "key." + Reference.MOD_ID + "." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                keyCode,
				minigame ? MINIGAMES_CATEGORY : DMZ_CATEGORY
        );
    }

	private static KeyMapping registerDMZMouse(String name, int keyCode) {
		return registerDMZMouse(name, keyCode, false);
	}

	private static KeyMapping registerDMZMouse(String name, int keyCode, boolean minigame) {
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
