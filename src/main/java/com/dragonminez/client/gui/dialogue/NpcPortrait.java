package com.dragonminez.client.gui.dialogue;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.gui.hud.HudRender;
import com.dragonminez.client.init.entities.renderer.MasterEntityRenderer;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.mixin.client.GeoModelAccessor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
final class NpcPortrait {
	private static final float TURN_DEGREES = 24.0f;
	private static final Set<String> PROP_BONES = Set.of("chair");
	private static final float MIN_HEAD_SIZE = 0.15f;
	private static final Map<ResourceLocation, Head> MODEL_HEADS = new HashMap<>();

	private NpcPortrait() {
	}

	static void render(GuiGraphics graphics, LivingEntity entity, float clipX0, float clipY0, float clipX1, float clipY1,
					   float headX, float headY, float headSize) {
		Minecraft minecraft = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity> renderer = dispatcher.getRenderer(entity);
		Head head = head(entity, renderer);
		float scale = headSize / Math.max(MIN_HEAD_SIZE, head.height());

		HudRender.scissor(graphics, clipX0, clipY0, clipX1, clipY1);
		graphics.pose().pushPose();
		graphics.pose().translate(headX, headY, 150.0f);
		graphics.pose().mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
		graphics.pose().mulPose(Axis.ZP.rotation((float) Math.PI));
		graphics.pose().mulPose(Axis.YP.rotationDegrees(TURN_DEGREES));
		graphics.pose().translate(-head.x(), -head.y(), -head.z());

		float bodyRot = entity.yBodyRot;
		float bodyRotO = entity.yBodyRotO;
		float yRot = entity.getYRot();
		float yRotO = entity.yRotO;
		float xRot = entity.getXRot();
		float xRotO = entity.xRotO;
		float headRot = entity.yHeadRot;
		float headRotO = entity.yHeadRotO;
		boolean nameVisible = entity.isCustomNameVisible();
		Entity picked = dispatcher.crosshairPickEntity;

		entity.yBodyRot = 180.0f;
		entity.yBodyRotO = 180.0f;
		entity.setYRot(180.0f);
		entity.yRotO = 180.0f;
		entity.setXRot(0.0f);
		entity.xRotO = 0.0f;
		entity.yHeadRot = 180.0f;
		entity.yHeadRotO = 180.0f;
		if (nameVisible) entity.setCustomNameVisible(false);
		dispatcher.crosshairPickEntity = null;

		if (renderer instanceof GeoEntityRenderer<?> geoRenderer) {
			((GeoModelAccessor) (Object) geoRenderer.getGeoModel()).dmz$setLastRenderedInstance(-1L);
		}
		boolean[] propHidden = new boolean[head.props().size()];
		boolean[] propChildrenHidden = new boolean[head.props().size()];
		for (int i = 0; i < head.props().size(); i++) {
			GeoBone prop = head.props().get(i);
			propHidden[i] = prop.isHidden();
			propChildrenHidden[i] = prop.isHidingChildren();
			prop.setHidden(true);
		}
		Lighting.setupForEntityInInventory();
		dispatcher.setRenderShadow(false);
		RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, graphics.pose(),
				graphics.bufferSource(), LightTexture.FULL_BRIGHT));
		graphics.flush();
		dispatcher.setRenderShadow(true);
		Lighting.setupFor3DItems();
		for (int i = 0; i < head.props().size(); i++) {
			GeoBone prop = head.props().get(i);
			prop.setHidden(propHidden[i]);
			prop.setChildrenHidden(propChildrenHidden[i]);
		}

		dispatcher.crosshairPickEntity = picked;
		if (nameVisible) entity.setCustomNameVisible(true);
		entity.yBodyRot = bodyRot;
		entity.yBodyRotO = bodyRotO;
		entity.setYRot(yRot);
		entity.yRotO = yRotO;
		entity.setXRot(xRot);
		entity.xRotO = xRotO;
		entity.yHeadRot = headRot;
		entity.yHeadRotO = headRotO;

		graphics.pose().popPose();
		graphics.disableScissor();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Head head(LivingEntity entity, EntityRenderer<? super LivingEntity> renderer) {
		float renderScale = renderer instanceof MasterEntityRenderer<?> && entity instanceof MastersEntity master ? master.getScale() : 1.0f;
		if (renderer instanceof GeoEntityRenderer<?> geoRenderer && entity instanceof GeoAnimatable animatable) {
			GeoModel model = geoRenderer.getGeoModel();
			ResourceLocation modelId = model.getModelResource(animatable);
			if (modelId != null) {
				Head measured = MODEL_HEADS.computeIfAbsent(modelId, id -> measure(model, id));
				if (measured != null) return measured.scaled(renderScale);
			}
		}
		float size = Math.max(0.3f, entity.getBbWidth() * 0.65f);
		return new Head(0.0f, entity.getEyeHeight(), 0.0f, size, List.of());
	}

	@SuppressWarnings("rawtypes")
	private static Head measure(GeoModel model, ResourceLocation modelId) {
		BakedGeoModel baked;
		try {
			baked = model.getBakedModel(modelId);
		} catch (RuntimeException exception) {
			LogUtil.warn(Env.CLIENT, "Dialogue portrait could not read model {}: {}", modelId, exception.getMessage());
			return null;
		}
		if (baked == null) return null;
		GeoBone head = find(baked.topLevelBones(), "head");
		if (head == null) return null;

		Vector3f min = new Vector3f(Float.MAX_VALUE);
		Vector3f max = new Vector3f(-Float.MAX_VALUE);
		include(head, min, max, head.getCubes().isEmpty());
		if (min.x > max.x) return null;
		List<GeoBone> props = new ArrayList<>();
		collectProps(baked.topLevelBones(), props);
		return new Head((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f, (min.z + max.z) * 0.5f, max.y - min.y, props);
	}

	private static GeoBone find(List<GeoBone> bones, String name) {
		for (GeoBone bone : bones) {
			if (bone.getName().equalsIgnoreCase(name)) return bone;
			GeoBone child = find(bone.getChildBones(), name);
			if (child != null) return child;
		}
		return null;
	}

	private static void collectProps(List<GeoBone> bones, List<GeoBone> props) {
		for (GeoBone bone : bones) {
			if (PROP_BONES.contains(bone.getName().toLowerCase(Locale.ROOT))) props.add(bone);
			else collectProps(bone.getChildBones(), props);
		}
	}

	private static void include(GeoBone bone, Vector3f min, Vector3f max, boolean children) {
		for (GeoCube cube : bone.getCubes()) {
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) continue;
				for (GeoVertex vertex : quad.vertices()) {
					min.min(vertex.position());
					max.max(vertex.position());
				}
			}
		}
		if (!children) return;
		for (GeoBone child : bone.getChildBones()) include(child, min, max, true);
	}

	private record Head(float x, float y, float z, float height, List<GeoBone> props) {
		Head scaled(float scale) {
			return new Head(x * scale, y * scale, z * scale, height * scale, props);
		}
	}
}
