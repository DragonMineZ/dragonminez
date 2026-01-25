package com.dragonminez.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.ArrayList;
import java.util.List;

public class AuraRenderQueue {
	public record AuraRenderEntry(AbstractClientPlayer player, BakedGeoModel playerModel, Matrix4f poseMatrix, float partialTick ,int packedLight) {}

	private static final List<AuraRenderEntry> QUEUE = new ArrayList<>();

	public static void add(AbstractClientPlayer player, BakedGeoModel playerModel, PoseStack currentStack, float partialTick, int packedLight) {
		Matrix4f matrixCopy = new Matrix4f(currentStack.last().pose());
		QUEUE.add(new AuraRenderEntry(player, playerModel, matrixCopy, partialTick, packedLight));
	}

	public static List<AuraRenderEntry> getAndClear() {
		List<AuraRenderEntry> copy = new ArrayList<>(QUEUE);
		QUEUE.clear();
		return copy;
	}
}