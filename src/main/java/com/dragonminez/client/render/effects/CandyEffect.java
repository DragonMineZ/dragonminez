package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.common.init.MainEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class CandyEffect {

	private static final ResourceLocation CANDY_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/candy.geo.json");
	private static final ResourceLocation CANDY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/candy.png");
	private static final float[] BOLT_COLOR = new float[]{1.0F, 0.4F, 0.8F};
	private static final float BOLT_RADIUS = 0.3F;
	private static final float BOLT_SPEED = 1.8F;
	private static final float PLAYER_HEIGHT = 1.8F;

	private static final Map<Integer, Long> CANDIED = new HashMap<>();
	private static final List<Bolt> BOLTS = new ArrayList<>();

	private static final class Bolt {
		final int casterId;
		final int targetId;
		final int lifetime;
		int age;

		Bolt(int casterId, int targetId, int lifetime) {
			this.casterId = casterId;
			this.targetId = targetId;
			this.lifetime = lifetime;
		}
	}

	private CandyEffect() {}

	public static void apply(int casterId, int targetId, int candyTicks, int boltTicks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		if (candyTicks > 0) CANDIED.put(targetId, mc.level.getGameTime() + candyTicks);
		if (casterId >= 0 && boltTicks > 0) BOLTS.add(new Bolt(casterId, targetId, boltTicks));
	}

	public static boolean isCandy(LivingEntity entity) {
		if (entity == null) return false;
		if (entity.hasEffect(MainEffects.CANDY)) return true;
		Long until = CANDIED.get(entity.getId());
		return until != null && entity.level().getGameTime() < until;
	}

	public static boolean renderInstead(Entity entity, double x, double y, double z, float partialTick, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight) {
		if (!(entity instanceof LivingEntity living) || entity instanceof net.minecraft.world.entity.player.Player) return false;
		if (!isCandy(living)) return false;
		BakedGeoModel model = GeckoLibCache.getBakedModels().get(CANDY_MODEL);
		if (model == null) return false;

		float scale = Math.max(0.6F, living.getBbHeight() / PLAYER_HEIGHT);
		float bodyYaw = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
		VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(CANDY_TEXTURE));

		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
		poseStack.scale(scale, scale, scale);
		for (GeoBone bone : model.topLevelBones()) renderBone(poseStack, bone, consumer, packedLight);
		poseStack.popPose();
		return true;
	}

	private static void renderBone(PoseStack poseStack, GeoBone bone, VertexConsumer consumer, int packedLight) {
		poseStack.pushPose();
		RenderUtil.prepMatrixForBone(poseStack, bone);
		for (GeoCube cube : bone.getCubes()) renderCube(poseStack, cube, consumer, packedLight);
		for (GeoBone child : bone.getChildBones()) renderBone(poseStack, child, consumer, packedLight);
		poseStack.popPose();
	}

	private static void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer consumer, int packedLight) {
		poseStack.pushPose();
		RenderUtil.translateToPivotPoint(poseStack, cube);
		RenderUtil.rotateMatrixAroundCube(poseStack, cube);
		RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
		Matrix3f normalMatrix = poseStack.last().normal();
		Matrix4f pose = new Matrix4f(poseStack.last().pose());
		for (GeoQuad quad : cube.quads()) {
			if (quad == null) continue;
			Vector3f normal = normalMatrix.transform(new Vector3f(quad.normal()));
			RenderUtil.fixInvertedFlatCube(cube, normal);
			for (GeoVertex vertex : quad.vertices()) {
				Vector3f position = vertex.position();
				Vector4f transformed = pose.transform(new Vector4f(position.x(), position.y(), position.z(), 1.0F));
				consumer.addVertex(transformed.x(), transformed.y(), transformed.z()).setColor(255, 255, 255, 255)
						.setUv(vertex.texU(), vertex.texV()).setOverlay(OverlayTexture.NO_OVERLAY)
						.setLight(packedLight).setNormal(normal.x(), normal.y(), normal.z());
			}
		}
		poseStack.popPose();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			CANDIED.clear();
			BOLTS.clear();
			return;
		}
		if (mc.isPaused()) return;
		long now = mc.level.getGameTime();
		CANDIED.values().removeIf(until -> until <= now);
		BOLTS.removeIf(bolt -> ++bolt.age >= bolt.lifetime);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || BOLTS.isEmpty() || !LightningBoltRenderer.isAvailable()) return;
		float partialTick = mc.isPaused() ? 0.0F : event.getPartialTick().getGameTimeDeltaPartialTick(false);

		if (!IrisCompat.isShaderPackInUse(mc.level.getGameTime())) {
			if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
			PlayerEffectQueue.addKiAttack((stack, projection) -> drawBolts(stack, projection, partialTick));
			return;
		}

		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
		mc.getMainRenderTarget().bindWrite(false);
		drawBolts(viewStack(mc.gameRenderer.getMainCamera()), RenderSystem.getProjectionMatrix(), partialTick);
	}

	private static void drawBolts(PoseStack poseStack, Matrix4f projection, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

		for (Bolt bolt : BOLTS) {
			Entity caster = mc.level.getEntity(bolt.casterId);
			Entity target = mc.level.getEntity(bolt.targetId);
			if (caster == null || target == null) continue;

			Vec3 start = caster.getEyePosition(partialTick);
			Vec3 end = target.getPosition(partialTick).add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			Vec3 delta = end.subtract(start);
			float length = (float) delta.length();
			if (length < 0.01F) continue;

			Quaternionf toTarget = new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), delta.toVector3f().normalize());
			Matrix4f pose = new Matrix4f(poseStack.last().pose())
					.translate((float) (start.x - camPos.x), (float) (start.y - camPos.y), (float) (start.z - camPos.z))
					.rotate(toTarget);
			float age = bolt.age + partialTick;
			float alpha = Mth.clamp((bolt.lifetime - age) / 4.0F, 0.0F, 1.0F);
			LightningBoltRenderer.drawColumn(pose, projection, bolt.casterId * 31 + bolt.targetId, age, length, BOLT_RADIUS,
					BOLT_COLOR, true, BOLT_SPEED, alpha);
		}
	}

	private static PoseStack viewStack(Camera camera) {
		PoseStack stack = new PoseStack();
		stack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		stack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		return stack;
	}
}
