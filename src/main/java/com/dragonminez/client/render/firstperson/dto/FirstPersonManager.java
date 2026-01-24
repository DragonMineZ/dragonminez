package com.dragonminez.client.render.firstperson.dto;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public class FirstPersonManager {

    private final static Vector3f ZERO = new Vector3f(0, 0, 0);

    public static boolean shouldRenderFirstPerson(Player player) {
        // Placeholder for actual implementation
        return Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    public static Vector3f offsetFirstPersonView(Player player) {
        // Placeholder for actual implementation
        return new Vector3f(0, 0.1F, 0.5F);
    }
}
