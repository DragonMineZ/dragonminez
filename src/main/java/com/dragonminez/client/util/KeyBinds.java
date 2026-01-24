package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBinds {

    private static final String CATEGORY = "key.categories." + Reference.MOD_ID;

    public static final KeyMapping STATS_MENU = registerKey("stats_menu", GLFW.GLFW_KEY_V);
    public static final KeyMapping KI_CHARGE = registerKey("ki_charge", GLFW.GLFW_KEY_C);
    public static final KeyMapping SECOND_FUNCTION_KEY = registerKey("second_function_key", GLFW.GLFW_KEY_LEFT_ALT);
	public static final KeyMapping ACTION_KEY = registerKey("action_key", GLFW.GLFW_KEY_G);
	public static final KeyMapping SPACEPOD_MENU = registerKey("spacepod_menu", GLFW.GLFW_KEY_H);
	public static final KeyMapping UTILITY_MENU = registerKey("utility_menu", GLFW.GLFW_KEY_X);
	public static final KeyMapping LOCK_ON = registerKey("lock_on", GLFW.GLFW_KEY_Z);
	public static final KeyMapping KI_SENSE = registerKey("ki_sense", GLFW.GLFW_KEY_F4);
	public static final KeyMapping FLY_KEY = registerKey("fly_key", GLFW.GLFW_KEY_F);
	public static final KeyMapping DASH_KEY = registerKey("dash_key", GLFW.GLFW_KEY_R);

    private static KeyMapping registerKey(String name, int keyCode) {
        return new KeyMapping(
                "key." + Reference.MOD_ID + "." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                keyCode,
                CATEGORY
        );
    }

    public static void registerAll(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        try {
            for (var field : KeyBinds.class.getDeclaredFields()) {
                if (field.getType() == KeyMapping.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
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

