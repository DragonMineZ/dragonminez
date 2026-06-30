package com.dragonminez.client.render.util;

import com.dragonminez.client.events.ModClientEvents;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.TransformationMaskRenderState;
import com.dragonminez.mixin.client.CompositeRenderTypeAccessor;
import com.dragonminez.mixin.client.CompositeStateAccessor;
import com.dragonminez.mixin.client.TextureStateShardInvoker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ModRenderTypes extends RenderType {
    public ModRenderTypes(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    public static final RenderStateShard.LayeringStateShard STENCIL_READ_NOTEQUAL = new RenderStateShard.LayeringStateShard("stencil_read_notequal", () -> {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }, () -> {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    });

    public static RenderType getCustomAura(ResourceLocation texture) {
        return RenderType.create("dragonminez_custom_aura",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> DMZShaders.auraShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setLayeringState(STENCIL_READ_NOTEQUAL)
                        .createCompositeState(false));
    }

    public static RenderType getCustomLightning(ResourceLocation texture) {
        return RenderType.create("dragonminez_custom_lightning",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> DMZShaders.lightningShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setLayeringState(STENCIL_READ_NOTEQUAL)
                        .createCompositeState(false));
    }

    public static RenderType getCustomAuraCompat(ResourceLocation texture) {
        return RenderType.create("dragonminez_custom_aura_compat",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> DMZShaders.auraShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    public static RenderType getCustomLightningCompat(ResourceLocation texture) {
        return RenderType.create("dragonminez_custom_lightning_compat",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> DMZShaders.lightningShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    private static final RenderStateShard.ShaderStateShard TRANSFORMATION_MASK_SHADER = new RenderStateShard.ShaderStateShard(() -> DMZShaders.outlineShader);
    private static final RenderStateShard.ShaderStateShard TRANSFORMATION_MASK_TEX_SHADER = new RenderStateShard.ShaderStateShard(() -> DMZShaders.outlineMaskTexShader);

    private static final Map<ResourceLocation, RenderType> TEXTURED_MASK_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> TEXTURED_MASK_VIEW_OFFSET_CACHE = new HashMap<>();

    private static final RenderStateShard.OutputStateShard TRANSFORMATION_MASK_TARGET = new RenderStateShard.OutputStateShard(
            "transformation_mask_target",
            TransformationMaskRenderState::bindMaskTarget,
            TransformationMaskRenderState::bindMainTarget
    );

    private static final String VIEW_OFFSET_LAYERING_TOKEN = "view_offset_z_layering";

    private static final RenderType TRANSFORMATION_MASK = create(
            "transformation_mask",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(TRANSFORMATION_MASK_SHADER)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(TRANSFORMATION_MASK_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType TRANSFORMATION_MASK_VIEW_OFFSET = create(
            "transformation_mask_view_offset",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(TRANSFORMATION_MASK_SHADER)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(TRANSFORMATION_MASK_TARGET)
                    .createCompositeState(false)
    );

    private static final Function<ResourceLocation, RenderType> GLOW = Util.memoize((pLocation) ->
            create("glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeItemEntityTranslucentCullShader))
                    .setTextureState(new TextureStateShard(pLocation, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)));

    private static final Function<ResourceLocation, RenderType> GLOW_KI = Util.memoize((pLocation) ->
            create("glow_ki", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new TextureStateShard(pLocation, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> ENERGY = Util.memoize((pLocation) ->
            create("energy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(pLocation, true, true))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> LIGHTNING = Util.memoize((pLocation) ->
            create("lightning", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(pLocation, true, true))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> KI_BLAST = Util.memoize((pLocation) ->
            create("ki_blastw", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                    .setTextureState(new TextureStateShard(pLocation, true, true))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> KI_RENDERTYPE = Util.memoize((pLocation) ->
            create("ki_rendertype", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(pLocation, true, true))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> ENERGY2 = Util.memoize((pLocation) ->
            create("energy2", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(pLocation, true, true))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> AURA_BILLBOARD = Util.memoize((pLocation) ->
            create("aura_billboard", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(pLocation, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false)));

    public static RenderType getKiLightning(ResourceLocation location) {
        return RenderType.create("ki_lightning",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setTextureState(new RenderStateShard.TextureStateShard(location, false, false))
                        .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setLayeringState(STENCIL_READ_NOTEQUAL)
                        .createCompositeState(false));
    }

    private static final RenderType GOO_BLOB = create(
            "dmz_goo_blob",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            4096,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false)
    );

    /** Opaque, vertex-coloured, double-sided geometry used for the Majin absorption gum blob. */
    public static RenderType gooBlob() { return GOO_BLOB; }

    public static RenderType glow(ResourceLocation pLocation) { return GLOW.apply(pLocation); }
    public static RenderType glow_ki(ResourceLocation pLocation) { return GLOW_KI.apply(pLocation); }
    public static RenderType energy(ResourceLocation pLocation) { return ENERGY.apply(pLocation); }
    public static RenderType energy2(ResourceLocation pLocation) { return ENERGY2.apply(pLocation); }
    public static RenderType auraBillboard(ResourceLocation pLocation) { return AURA_BILLBOARD.apply(pLocation); }
    public static RenderType lightning(ResourceLocation pLocation) { return LIGHTNING.apply(pLocation); }
    public static RenderType kiblast(ResourceLocation pLocation) { return KI_BLAST.apply(pLocation); }
    public static RenderType ki_rendertype(ResourceLocation pLocation) { return KI_RENDERTYPE.apply(pLocation); }

    public static boolean hasTransformationMaskShader() {
        return DMZShaders.outlineShader != null;
    }

    public static RenderType transformationMask(RenderType sourceRenderType) {
        ResourceLocation texture = resolveSourceTexture(sourceRenderType);
        boolean viewOffset = sourceRenderType != null && sourceRenderType.toString().contains(VIEW_OFFSET_LAYERING_TOKEN);
        if (texture != null) {
            Map<ResourceLocation, RenderType> cache = viewOffset ? TEXTURED_MASK_VIEW_OFFSET_CACHE : TEXTURED_MASK_CACHE;
            return cache.computeIfAbsent(texture, t -> buildTexturedMask(t, viewOffset));
        }
        if (viewOffset) {
            return TRANSFORMATION_MASK_VIEW_OFFSET;
        }
        return TRANSFORMATION_MASK;
    }

    @Nullable
    private static ResourceLocation resolveSourceTexture(@Nullable RenderType sourceRenderType) {
        if (!(sourceRenderType instanceof CompositeRenderTypeAccessor accessor)) return null;
        RenderType.CompositeState state = accessor.dmz$state();
        if (state == null) return null;
        RenderStateShard.EmptyTextureStateShard textureState = ((CompositeStateAccessor) (Object) state).dmz$textureState();
        if (textureState == null) return null;
        return ((TextureStateShardInvoker) (Object) textureState).dmz$cutoutTexture().orElse(null);
    }

    private static RenderType buildTexturedMask(ResourceLocation texture, boolean viewOffset) {
        CompositeState.CompositeStateBuilder builder = CompositeState.builder()
                .setShaderState(TRANSFORMATION_MASK_TEX_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .setOutputState(TRANSFORMATION_MASK_TARGET);
        if (viewOffset) {
            builder.setLayeringState(VIEW_OFFSET_Z_LAYERING);
        }
        return create(
                "transformation_mask_tex",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                builder.createCompositeState(false)
        );
    }

    public static RenderType transformationMask() {
        return TRANSFORMATION_MASK;
    }

    public static RenderType transformationMaskViewOffset() {
        return TRANSFORMATION_MASK_VIEW_OFFSET;
    }

}
