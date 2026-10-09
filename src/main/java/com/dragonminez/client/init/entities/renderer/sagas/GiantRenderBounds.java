package com.dragonminez.client.init.entities.renderer.sagas;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.Map;

public final class GiantRenderBounds {

    private static final float RADIUS_MARGIN = 1.25F;
    private static final float RADIUS_PER_HEIGHT = 0.5F;
    private static final float HEIGHT_MARGIN = 1.3F;

    private static final Map<ResourceLocation, Measured> CACHE = new HashMap<>();

    private record Measured(BakedGeoModel model, float[] extents) {}

    private GiantRenderBounds() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static float[] extentsFor(DBSagasEntity entity) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof GeoEntityRenderer geoRenderer)) return null;

        ResourceLocation location;
        try {
            GeoModel model = geoRenderer.getGeoModel();
            location = model.getModelResource(entity, geoRenderer);
        } catch (RuntimeException e) {
            return null;
        }
        if (location == null) return null;

        BakedGeoModel baked = GeckoLibCache.getBakedModels().get(location);
        if (baked == null) return null;

        Measured measured = CACHE.get(location);
        if (measured == null || measured.model() != baked) {
            measured = new Measured(baked, measure(baked));
            CACHE.put(location, measured);
        }
        return measured.extents();
    }

    private static float[] measure(BakedGeoModel model) {
        float[] bounds = {0.0F, 0.0F, 0.0F};
        PoseStack poseStack = new PoseStack();
        Vector4f point = new Vector4f();
        for (GeoBone bone : model.topLevelBones()) measureBone(poseStack, bone, point, bounds);

        float radius = Math.max(bounds[0] * RADIUS_MARGIN, bounds[1] * RADIUS_PER_HEIGHT);
        return new float[]{radius, bounds[1] * HEIGHT_MARGIN, bounds[2] * HEIGHT_MARGIN};
    }

    private static void measureBone(PoseStack poseStack, GeoBone bone, Vector4f point, float[] bounds) {
        poseStack.pushPose();

        BoneSnapshot rest = bone.getInitialSnapshot();
        float rotX = rest != null ? rest.getRotX() : bone.getRotX();
        float rotY = rest != null ? rest.getRotY() : bone.getRotY();
        float rotZ = rest != null ? rest.getRotZ() : bone.getRotZ();

        RenderUtils.translateToPivotPoint(poseStack, bone);
        if (rotZ != 0.0F) poseStack.mulPose(Axis.ZP.rotation(rotZ));
        if (rotY != 0.0F) poseStack.mulPose(Axis.YP.rotation(rotY));
        if (rotX != 0.0F) poseStack.mulPose(Axis.XP.rotation(rotX));
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        for (GeoCube cube : bone.getCubes()) {
            poseStack.pushPose();
            RenderUtils.translateToPivotPoint(poseStack, cube);
            RenderUtils.rotateMatrixAroundCube(poseStack, cube);
            RenderUtils.translateAwayFromPivotPoint(poseStack, cube);

            Matrix4f pose = poseStack.last().pose();
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;
                for (GeoVertex vertex : quad.vertices()) {
                    pose.transform(point.set(vertex.position(), 1.0F));
                    bounds[0] = Math.max(bounds[0], (float) Math.sqrt(point.x * point.x + point.z * point.z));
                    bounds[1] = Math.max(bounds[1], point.y);
                    bounds[2] = Math.min(bounds[2], point.y);
                }
            }
            poseStack.popPose();
        }

        for (GeoBone child : bone.getChildBones()) measureBone(poseStack, child, point, bounds);

        poseStack.popPose();
    }
}
