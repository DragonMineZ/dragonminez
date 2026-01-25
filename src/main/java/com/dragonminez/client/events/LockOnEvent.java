package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class LockOnEvent {
	private static final ResourceLocation LOCK_ICON = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/lock_on.png");
	private static LivingEntity lockedTarget = null;

	public static void toggleLock() {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null) return;

		if (lockedTarget != null) {
			unlock();
			return;
		}

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getSkills().hasSkill("kisense")) return;

			int level = data.getSkills().getSkillLevel("kisense");
			if (level <= 0) return;

			double range = 5.0 + 3.0 * level;

			findTargetInFront(player, range).ifPresent(target -> {
				lockedTarget = target;
				player.playSound(MainSounds.LOCKON.get());
			});
		});
	}

	public static void unlock() {
		lockedTarget = null;
	}

	@SubscribeEvent
	public static void onRenderTick(TickEvent.RenderTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;

		if (player == null || lockedTarget == null) return;

		if (!lockedTarget.isAlive()) {
			unlock();
			return;
		}

		AtomicBoolean shouldUnlock = new AtomicBoolean(false);

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			int level = data.getSkills().getSkillLevel("kisense");

			if (level <= 0 || data.getSkills().getSkill("kisense") == null) {
				shouldUnlock.set(true);
				return;
			}

			double maxRange = 5 + 3.0 * level;

			if (player.distanceTo(lockedTarget) > maxRange) {
				shouldUnlock.set(true);
			}
		});

		if (shouldUnlock.get()) {
			unlock();
			return;
		}

		float partialTick = event.renderTickTime;
		double targetX = Mth.lerp(partialTick, lockedTarget.xo, lockedTarget.getX());
		double targetY = Mth.lerp(partialTick, lockedTarget.yo, lockedTarget.getY()) + lockedTarget.getBbHeight() * 0.5;
		double targetZ = Mth.lerp(partialTick, lockedTarget.zo, lockedTarget.getZ());
		Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
		Vec3 playerPos = player.getEyePosition(partialTick);

		double dX = targetPos.x - playerPos.x;
		double dY = targetPos.y - playerPos.y;
		double dZ = targetPos.z - playerPos.z;
		double dist = Math.sqrt(dX * dX + dZ * dZ);

		float targetYaw = (float) (Mth.atan2(dZ, dX) * (180 / Math.PI)) - 90.0F;
		float targetPitch = (float) -(Mth.atan2(dY, dist) * (180 / Math.PI));

		float smoothFactor = 0.15F;

		float newYaw = rotlerp(player.getYRot(), targetYaw, smoothFactor);
		float newPitch = rotlerp(player.getXRot(), targetPitch, smoothFactor);

		player.setYRot(newYaw);
		player.setXRot(newPitch);
	}

	@SubscribeEvent
	public static void onRenderWorldLast(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
		if (lockedTarget == null || !lockedTarget.isAlive()) return;

		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick();

		double lerpX = Mth.lerp(partialTick, lockedTarget.xo, lockedTarget.getX());
		double lerpY = Mth.lerp(partialTick, lockedTarget.yo, lockedTarget.getY());
		double lerpZ = Mth.lerp(partialTick, lockedTarget.zo, lockedTarget.getZ());

		Vec3 cameraPos = event.getCamera().getPosition();

		poseStack.pushPose();
		poseStack.translate(lerpX - cameraPos.x, (lerpY - cameraPos.y) + lockedTarget.getBbHeight() * 0.5, lerpZ - cameraPos.z);

		poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

		float scale = 0.04F;
		poseStack.scale(-scale, -scale, scale);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, LOCK_ICON);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.depthFunc(GL11.GL_ALWAYS);

		long time = System.currentTimeMillis();
		float angle1 = (time % 3600L) / 10.0f;
		float angle2 = -((time % 7200L) / 20.0f);

		float size = 16.0f;

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(angle1));
		drawTextureQuad(poseStack, size, 0.0F, 1.0F, 1.0F, 0.9F);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.mulPose(Axis.ZP.rotationDegrees(angle2));
		poseStack.scale(1.5f, 1.5f, 1.5f);
		poseStack.translate(0, 0, 0.05f);
		drawTextureQuad(poseStack, size, 0.0F, 1.0F, 1.0F, 0.5F);
		poseStack.popPose();

		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		poseStack.popPose();
	}

	private static void drawTextureQuad(PoseStack poseStack, float size, float r, float g, float b, float a) {
		RenderSystem.setShaderColor(r, g, b, a);

		Matrix4f matrix = poseStack.last().pose();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.getBuilder();

		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		builder.vertex(matrix, -size, size, 0).uv(0, 1).endVertex();
		builder.vertex(matrix, size, size, 0).uv(1, 1).endVertex();
		builder.vertex(matrix, size, -size, 0).uv(1, 0).endVertex();
		builder.vertex(matrix, -size, -size, 0).uv(0, 0).endVertex();

		tesselator.end();
	}

	private static Optional<LivingEntity> findTargetInFront(Player player, double range) {
		Vec3 eyePos = player.getEyePosition();
		Vec3 viewVec = player.getViewVector(1.0F);
		Vec3 endPos = eyePos.add(viewVec.scale(range));
		AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0D);

		List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
				e -> e != player && e.isAlive() && e.isPickable());

		LivingEntity closest = null;
		double closestDist = range * range;

		for (LivingEntity e : list) {
			AABB axisalignedbb = e.getBoundingBox().inflate(e.getPickRadius());
			Optional<Vec3> hit = axisalignedbb.clip(eyePos, endPos);

			if (axisalignedbb.contains(eyePos)) {
				if (closestDist >= 0.0D) {
					closest = e;
					closestDist = 0.0D;
				}
			} else if (hit.isPresent()) {
				double dist = eyePos.distanceToSqr(hit.get());
				if (dist < closestDist) {
					closest = e;
					closestDist = dist;
				}
			}
		}
		return Optional.ofNullable(closest);
	}

	private static float rotlerp(float start, float target, float amount) {
		float f = Mth.wrapDegrees(target - start);
		if (f > 180.0F) f -= 360.0F;
		if (f < -180.0F) f += 360.0F;
		return start + amount * f;
	}
}
