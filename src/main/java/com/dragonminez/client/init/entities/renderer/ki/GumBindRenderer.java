package com.dragonminez.client.init.entities.renderer.ki;

import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.ki.GumBindEntity;
import com.dragonminez.common.network.C2S.GumStruggleC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GumBindRenderer extends GeoEntityRenderer<GumBindEntity> {

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/ki/gum_bind.png");
	private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/skills/gum_bind.geo.json");
	private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/skills/gum_bind.animation.json");
	private static final String TOP_COIL = "coil_1";
	private static final float GLOSS_LIFT = 0.55F;
	private static final float REACH_TICKS = 3.0F;
	private static final double STRAND_HALF_WIDTH = 0.13D;
	private static final float STRAND_U0 = 4.0F / 16.0F;
	private static final float STRAND_U1 = 8.0F / 16.0F;
	private static final float STRAND_V0 = 0.0F;
	private static final float STRAND_V1 = 4.0F / 16.0F;

	public GumBindRenderer(EntityRendererProvider.Context context) {
		super(context, new Model());
		this.shadowRadius = 0.0F;
	}

	@Override
	public boolean shouldRender(GumBindEntity gum, Frustum frustum, double camX, double camY, double camZ) {
		if (gum.tickCount < GumBindEntity.STRAND_TICKS) return true;
		LivingEntity victim = gum.victim();
		return frustum.isVisible((victim != null ? victim.getBoundingBox() : gum.getBoundingBox()).inflate(1.5D));
	}

	@Override
	public void render(GumBindEntity gum, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		LivingEntity victim = gum.victim();
		Vec3 origin = gum.getPosition(partialTick);
		Vec3 anchor = victim != null ? victim.getPosition(partialTick) : origin;
		float height = victim != null ? victim.getBbHeight() : 1.8F;
		float width = victim != null ? victim.getBbWidth() : 0.6F;
		float scaleY = height / 1.8F;
		float scaleXZ = Mth.clamp(Math.max(scaleY, width / 0.6F * 0.85F), scaleY, scaleY * 1.6F);

		poseStack.pushPose();
		poseStack.translate(anchor.x - origin.x, anchor.y - origin.y, anchor.z - origin.z);
		poseStack.scale(scaleXZ, scaleY, scaleXZ);
		super.render(gum, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		poseStack.popPose();

		if (victim != null && !gum.isReleasing() && gum.tickCount < GumBindEntity.STRAND_TICKS) {
			this.renderStrand(gum, victim, origin, partialTick, poseStack, bufferSource, packedLight);
		}
	}

	@Override
	protected void applyRotations(GumBindEntity gum, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
		LivingEntity victim = gum.victim();
		float yaw = victim != null ? Mth.rotLerp(partialTick, victim.yBodyRotO, victim.yBodyRot) : gum.getYRot();
		super.applyRotations(gum, poseStack, ageInTicks, yaw, partialTick);
	}

	@Override
	public void preRender(PoseStack poseStack, GumBindEntity gum, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
						  boolean isReRender, float partialTick, int packedLight, int packedOverlay, int packedColor) {
		Minecraft mc = Minecraft.getInstance();
		boolean firstPersonVictim = mc.player != null && gum.victimId() == mc.player.getId() && mc.options.getCameraType().isFirstPerson();
		model.getBone(TOP_COIL).ifPresent(bone -> bone.setHidden(firstPersonVictim));
		super.preRender(poseStack, gum, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, packedColor);
	}

	@Override
	public void renderRecursively(PoseStack poseStack, GumBindEntity gum, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
								  VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
								  int packedColor) {
		if (bone.getName().endsWith("_gloss")) {
			int alpha = FastColor.ARGB32.alpha(packedColor);
			int red = (int) (FastColor.ARGB32.red(packedColor) + (255 - FastColor.ARGB32.red(packedColor)) * GLOSS_LIFT);
			int green = (int) (FastColor.ARGB32.green(packedColor) + (255 - FastColor.ARGB32.green(packedColor)) * GLOSS_LIFT);
			int blue = (int) (FastColor.ARGB32.blue(packedColor) + (255 - FastColor.ARGB32.blue(packedColor)) * GLOSS_LIFT);
			packedColor = FastColor.ARGB32.color(alpha, red, green, blue);
		}
		super.renderRecursively(poseStack, gum, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, packedColor);
	}

	@Override
	public Color getRenderColor(GumBindEntity gum, float partialTick, int packedLight) {
		int color = gum.color();
		return Color.ofRGBA(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
	}

	@Override
	public RenderType getRenderType(GumBindEntity gum, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityCutout(texture);
	}

	private void renderStrand(GumBindEntity gum, LivingEntity victim, Vec3 origin, float partialTick, PoseStack poseStack,
							  MultiBufferSource bufferSource, int packedLight) {
		if (!(gum.level().getEntity(gum.casterId()) instanceof LivingEntity caster)) return;

		float age = gum.tickCount + partialTick;
		float reach = Mth.clamp(age / REACH_TICKS, 0.0F, 1.0F);
		float pull = Mth.clamp((age - REACH_TICKS) / (GumBindEntity.STRAND_TICKS - REACH_TICKS), 0.0F, 1.0F);
		Vec3 hand = hand(caster, partialTick).subtract(origin);
		Vec3 chest = victim.getPosition(partialTick).add(0.0D, victim.getBbHeight() * 0.55D, 0.0D).subtract(origin);
		Vec3 from = hand.lerp(chest, pull);
		Vec3 to = hand.lerp(chest, reach);

		int color = gum.color();
		float r = ((color >> 16) & 0xFF) / 255.0F;
		float g = ((color >> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		PoseStack.Pose last = poseStack.last();
		tube(consumer, last, from, to, STRAND_HALF_WIDTH * (1.0D - 0.5D * pull), r, g, b, packedLight);
	}

	private static Vec3 hand(LivingEntity caster, float partialTick) {
		return GumBindEntity.hand(caster.getPosition(partialTick), Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot),
				caster.getBbHeight() / 1.8F);
	}

	private static void tube(VertexConsumer consumer, PoseStack.Pose pose, Vec3 from, Vec3 to, double half,
							 float r, float g, float b, int light) {
		Vec3 dir = to.subtract(from);
		if (dir.lengthSqr() < 1.0E-6D) return;
		dir = dir.normalize();
		Vec3 up = Math.abs(dir.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 a = dir.cross(up).normalize().scale(half);
		Vec3 c = dir.cross(a).normalize().scale(half);
		Vec3[] corners = {a.add(c), c.subtract(a), a.scale(-1.0D).subtract(c), a.subtract(c)};
		for (int side = 0; side < 4; side++) {
			Vec3 c0 = corners[side];
			Vec3 c1 = corners[(side + 1) % 4];
			Vec3 n = c0.add(c1).normalize();
			float shade = side == 0 ? 1.0F : side == 2 ? 0.7F : 0.85F;
			vertex(consumer, pose, from.add(c0), STRAND_U0, STRAND_V0, r * shade, g * shade, b * shade, light, n);
			vertex(consumer, pose, from.add(c1), STRAND_U1, STRAND_V0, r * shade, g * shade, b * shade, light, n);
			vertex(consumer, pose, to.add(c1), STRAND_U1, STRAND_V1, r * shade, g * shade, b * shade, light, n);
			vertex(consumer, pose, to.add(c0), STRAND_U0, STRAND_V1, r * shade, g * shade, b * shade, light, n);
		}
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v,
							   float r, float g, float b, int light, Vec3 n) {
		consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z).setColor(r, g, b, 1.0F).setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
	}

	public static class BallRenderer extends EntityRenderer<GumBindEntity.Ball> {

		private static final float RADIUS = 0.22F;
		private static final float CORE = 0.7F;
		private static final float STRETCH = 1.35F;
		private static final float SIDE_U0 = 4.0F / 16.0F;
		private static final float SIDE_U1 = 8.0F / 16.0F;
		private static final float SIDE_V0 = 4.0F / 16.0F;
		private static final float SIDE_V1 = 1.0F;
		private static final float TOP_U0 = 12.0F / 16.0F;
		private static final float SHADE_U0 = 8.0F / 16.0F;
		private static final float PATCH_V1 = 4.0F / 16.0F;

		public BallRenderer(EntityRendererProvider.Context context) {
			super(context);
			this.shadowRadius = 0.0F;
		}

		@Override
		public boolean shouldRender(GumBindEntity.Ball ball, Frustum frustum, double camX, double camY, double camZ) {
			return true;
		}

		@Override
		public void render(GumBindEntity.Ball ball, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
			int color = ball.color();
			float r = ((color >> 16) & 0xFF) / 255.0F;
			float g = ((color >> 8) & 0xFF) / 255.0F;
			float b = (color & 0xFF) / 255.0F;
			Vec3 origin = ball.getPosition(partialTick);
			Vec3 center = new Vec3(0.0D, ball.getBbHeight() * 0.5D, 0.0D);
			VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

			if (ball.getOwner() instanceof LivingEntity caster) {
				PoseStack.Pose last = poseStack.last();
				tube(consumer, last, hand(caster, partialTick).subtract(origin), center, STRAND_HALF_WIDTH * 0.8D,
						r, g, b, packedLight);
			}

			Vec3 motion = ball.getDeltaMovement();
			float age = ball.tickCount + partialTick;
			float wobble = Mth.sin(age * 1.3F) * 0.12F;
			poseStack.pushPose();
			poseStack.translate(center.x, center.y, center.z);
			poseStack.mulPose(Axis.YP.rotation((float) Math.atan2(motion.x, motion.z)));
			poseStack.mulPose(Axis.XP.rotation((float) -Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z))));
			poseStack.scale(1.0F - wobble * 0.5F, 1.0F + wobble * 0.5F, STRETCH + wobble);
			poseStack.mulPose(Axis.ZP.rotation(age * 0.4F));
			PoseStack.Pose last = poseStack.last();
			box(consumer, last, RADIUS, RADIUS * CORE, RADIUS * CORE, r, g, b, packedLight);
			box(consumer, last, RADIUS * CORE, RADIUS, RADIUS * CORE, r, g, b, packedLight);
			box(consumer, last, RADIUS * CORE, RADIUS * CORE, RADIUS, r, g, b, packedLight);
			poseStack.popPose();
			super.render(ball, entityYaw, partialTick, poseStack, bufferSource, packedLight);
		}

		private static void box(VertexConsumer consumer, PoseStack.Pose pose, float hx, float hy, float hz,
								float r, float g, float b, int light) {
			quad(consumer, pose, -hx, hy, -hz, -hx, hy, hz, hx, hy, hz, hx, hy, -hz, 0, 1, 0, TOP_U0, 0.0F, 1.0F, PATCH_V1, r, g, b, light);
			quad(consumer, pose, -hx, -hy, hz, -hx, -hy, -hz, hx, -hy, -hz, hx, -hy, hz, 0, -1, 0, SHADE_U0, 0.0F, TOP_U0, PATCH_V1, r, g, b, light);
			quad(consumer, pose, hx, hy, -hz, hx, -hy, -hz, -hx, -hy, -hz, -hx, hy, -hz, 0, 0, -1, SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1, r, g, b, light);
			quad(consumer, pose, -hx, hy, hz, -hx, -hy, hz, hx, -hy, hz, hx, hy, hz, 0, 0, 1, SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1, r, g, b, light);
			quad(consumer, pose, -hx, hy, -hz, -hx, -hy, -hz, -hx, -hy, hz, -hx, hy, hz, -1, 0, 0, SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1, r, g, b, light);
			quad(consumer, pose, hx, hy, hz, hx, -hy, hz, hx, -hy, -hz, hx, hy, -hz, 1, 0, 0, SIDE_U0, SIDE_V0, SIDE_U1, SIDE_V1, r, g, b, light);
		}

		private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
								 float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
								 float nx, float ny, float nz, float u0, float v0, float u1, float v1, float r, float g, float b, int light) {
			corner(consumer, pose, x1, y1, z1, u0, v0, nx, ny, nz, r, g, b, light);
			corner(consumer, pose, x2, y2, z2, u0, v1, nx, ny, nz, r, g, b, light);
			corner(consumer, pose, x3, y3, z3, u1, v1, nx, ny, nz, r, g, b, light);
			corner(consumer, pose, x4, y4, z4, u1, v0, nx, ny, nz, r, g, b, light);
		}

		private static void corner(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
								   float nx, float ny, float nz, float r, float g, float b, int light) {
			consumer.addVertex(pose, x, y, z).setColor(r, g, b, 1.0F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
					.setNormal(pose, nx, ny, nz);
		}

		@Override
		public ResourceLocation getTextureLocation(GumBindEntity.Ball ball) {
			return TEXTURE;
		}
	}

	public static class Model extends GeoModel<GumBindEntity> {

		@Override
		public ResourceLocation getModelResource(GumBindEntity gum) {
			return MODEL;
		}

		@Override
		public ResourceLocation getTextureResource(GumBindEntity gum) {
			return TEXTURE;
		}

		@Override
		public ResourceLocation getAnimationResource(GumBindEntity gum) {
			return ANIMATION;
		}
	}

	@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
	public static final class Input {

		private Input() {}

		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			Minecraft mc = Minecraft.getInstance();
			int presses = 0;
			while (mc.options.keyShift.consumeClick()) presses++;
			if (presses == 0 || mc.player == null || mc.level == null || mc.screen != null) return;
			LocalPlayer player = mc.player;
			boolean bound = !mc.level.getEntitiesOfClass(GumBindEntity.class, player.getBoundingBox().inflate(4.0D),
					gum -> gum.victimId() == player.getId() && !gum.isReleasing()).isEmpty();
			if (bound) NetworkHandler.sendToServer(new GumStruggleC2S());
		}
	}
}
