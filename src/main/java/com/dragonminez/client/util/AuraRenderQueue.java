package com.dragonminez.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.ArrayList;
import java.util.List;

public class AuraRenderQueue {
	public record AuraRenderEntry(AbstractClientPlayer player, BakedGeoModel playerModel, Matrix4f poseMatrix, float partialTick ,int packedLight) {}
    public record WeaponRenderEntry(AbstractClientPlayer player, BakedGeoModel playerModel, Matrix4f poseMatrix, String weaponType, float[] color, float partialTick, int packedLight) {}

	private static final List<AuraRenderEntry> AURA_QUEUE = new ArrayList<>();
    private static final List<WeaponRenderEntry> WEAPON_QUEUE = new ArrayList<>();

	public static void addAura(AbstractClientPlayer player, BakedGeoModel playerModel, PoseStack currentStack, float partialTick, int packedLight) {
		Matrix4f matrixCopy = new Matrix4f(currentStack.last().pose());
		AURA_QUEUE.add(new AuraRenderEntry(player, playerModel, matrixCopy, partialTick, packedLight));
	}

    public static void addWeapon(AbstractClientPlayer player, BakedGeoModel playerModel, PoseStack currentStack, String weaponType, float[] color, float partialTick, int packedLight) {
        WEAPON_QUEUE.add(new WeaponRenderEntry(player, playerModel, new Matrix4f(currentStack.last().pose()), weaponType, color, partialTick, packedLight));
    }

	public static List<AuraRenderEntry> getAndClearAuras() {
		List<AuraRenderEntry> copy = new ArrayList<>(AURA_QUEUE);
		AURA_QUEUE.clear();
		return copy;
	}

    public static List<WeaponRenderEntry> getAndClearWeapons() {
        List<WeaponRenderEntry> copy = new ArrayList<>(WEAPON_QUEUE);
        WEAPON_QUEUE.clear();
        return copy;
    }
}