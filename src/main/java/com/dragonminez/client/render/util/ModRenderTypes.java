package com.dragonminez.client.render.util;

import com.dragonminez.client.events.ModClientEvents;
import com.dragonminez.client.render.shader.DMZShaders;
import com.dragonminez.client.render.shader.TransformationMaskRenderState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

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

    /**
     * Shaderpack-compatible aura variant. Under Oculus the world is deferred and
     * the scene depth (incl. the player body) is written to the main target, so
     * a depth-tested aura billboard at the player's position is occluded and
     * vanishes; the mod's stencil that normally gated this is gone too. This
     * variant disables depth testing and draws the aura on top (like the GUI
     * aura) so it stays visible with shaderpacks active.
     */
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

    /** Shaderpack-compatible lightning variant (see {@link #getCustomAuraCompat}). */
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

    private static final RenderStateShard.OutputStateShard TRANSFORMATION_MASK_TARGET = new RenderStateShard.OutputStateShard(
            "transformation_mask_target",
            TransformationMaskRenderState::bindMaskTarget,
            TransformationMaskRenderState::bindMainTarget
    );
    private static final RenderStateShard.OutputStateShard TRANSFORMATION_PARAMS_TARGET = new RenderStateShard.OutputStateShard(
            "transformation_params_target",
            TransformationMaskRenderState::bindParamsTarget,
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

    private static final RenderType TRANSFORMATION_PARAMS = create(
            "transformation_params",
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
                    .setOutputState(TRANSFORMATION_PARAMS_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType TRANSFORMATION_PARAMS_VIEW_OFFSET = create(
            "transformation_params_view_offset",
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
                    .setOutputState(TRANSFORMATION_PARAMS_TARGET)
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

    // Shaderpack-compatible aura: vanilla emissive shader + additive blending so
    // the grayscale-on-black aura spritesheet glows (black adds nothing). Same
    // proven pattern as the ki render types, which already work under Oculus —
    // unlike the custom aura shader, whose texture sampling Oculus drops.
    private static final Function<ResourceLocation, RenderType> AURA_EMISSIVE = Util.memoize((pLocation) ->
            create("aura_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(pLocation, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    public static RenderType auraEmissive(ResourceLocation pLocation) { return AURA_EMISSIVE.apply(pLocation); }

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
        if (sourceRenderType != null && sourceRenderType.toString().contains(VIEW_OFFSET_LAYERING_TOKEN)) {
            return TRANSFORMATION_MASK_VIEW_OFFSET;
        }
        return TRANSFORMATION_MASK;
    }

    public static RenderType transformationParams(RenderType sourceRenderType) {
        if (sourceRenderType != null && sourceRenderType.toString().contains(VIEW_OFFSET_LAYERING_TOKEN)) {
            return TRANSFORMATION_PARAMS_VIEW_OFFSET;
        }
        return TRANSFORMATION_PARAMS;
    }

    public static RenderType transformationMask() {
        return TRANSFORMATION_MASK;
    }

    public static RenderType transformationMaskViewOffset() {
        return TRANSFORMATION_MASK_VIEW_OFFSET;
    }

    public static RenderType transformationParams() {
        return TRANSFORMATION_PARAMS;
    }

    public static RenderType transformationParamsViewOffset() {
        return TRANSFORMATION_PARAMS_VIEW_OFFSET;
    }

    // Inflated outline used ONLY when a shaderpack is active (the PostChain
    // outline above cannot composite under Oculus). It uses a VANILLA emissive
    // shader (which Oculus patches and renders natively, unlike a custom shader).
    // It is drawn BEFORE the body with no depth writes, so the normal-size body
    // is then painted over the centre and only the surrounding ring remains —
    // this avoids relying on face-cull winding (which Oculus can override).
    // Colour is applied through the ColorModulator at flush time.
    private static final Function<ResourceLocation, RenderType> TRANSFORMATION_HULL = Util.memoize((pLocation) ->
            create("transformation_hull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 2048, false, false,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                            .setTextureState(new TextureStateShard(pLocation, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    public static RenderType transformationHull(ResourceLocation pLocation) {
        return TRANSFORMATION_HULL.apply(pLocation);
    }
}