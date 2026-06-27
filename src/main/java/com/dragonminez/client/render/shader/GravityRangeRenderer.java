package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Toggleable client-side visualization (shift + right-click on a Gravity Device) that draws the
 * bounds of the device's detected room. Green when the room is valid, red when it is not.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GravityRangeRenderer {

	private static final Set<BlockPos> SHOWN = new HashSet<>();

	private GravityRangeRenderer() {}

	public static void toggle(BlockPos pos) {
		BlockPos key = pos.immutable();
		if (!SHOWN.remove(key)) SHOWN.add(key);
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
		if (SHOWN.isEmpty()) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

		poseStack.pushPose();
		poseStack.translate(-cam.x, -cam.y, -cam.z);

		Iterator<BlockPos> it = SHOWN.iterator();
		while (it.hasNext()) {
			BlockPos pos = it.next();
			BlockEntity be = mc.level.getBlockEntity(pos);
			if (!(be instanceof GravityDeviceBlockEntity device)) {
				it.remove();
				continue;
			}

			boolean valid = device.isRoomValid();
			AABB box;
			if (valid) {
				BlockPos min = device.getRoomMin();
				BlockPos max = device.getRoomMax();
				box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
			} else {
				box = new AABB(pos).inflate(0.02);
			}

			float r = valid ? 0.2f : 1.0f;
			float g = valid ? 1.0f : 0.2f;
			float b = valid ? 0.4f : 0.2f;
			LevelRenderer.renderLineBox(poseStack, consumer, box, r, g, b, 0.9f);
		}

		poseStack.popPose();
		buffer.endBatch(RenderType.lines());
	}
}
