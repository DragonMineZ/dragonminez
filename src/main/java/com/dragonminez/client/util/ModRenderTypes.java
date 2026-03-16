package com.dragonminez.client.util;

import com.dragonminez.client.events.ModClientEvents;
import com.dragonminez.client.render.shader.DMZShaders;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class ModRenderTypes extends RenderType {
    public ModRenderTypes(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    public static RenderType getCustomAura(ResourceLocation texture) {
        return RenderType.create("dragonminez_custom_aura",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> DMZShaders.auraSmoothShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
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
                        .createCompositeState(false));
    }

    public static RenderType getEnergySphere(ResourceLocation texture) {
        return RenderType.create("energy_sphere",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false, // Al ser 'false', evitamos que las capas se oculten entre sí por profundidad
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(ModClientEvents::getEnergySphereShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.NO_LIGHTMAP) // Importante: Ignora la luz ambiental para brillar siempre
                        .setOverlayState(RenderStateShard.NO_OVERLAY)
                        .setCullState(RenderStateShard.NO_CULL) // Permite ver el plano por ambos lados
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .createCompositeState(false));
    }

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
                        .createCompositeState(false));
    }

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

    public static RenderType glow(ResourceLocation pLocation) {
        return GLOW.apply(pLocation);
    }
    public static RenderType glow_ki(ResourceLocation pLocation) {
        return GLOW_KI.apply(pLocation);
    }
    public static RenderType energy(ResourceLocation pLocation) {
        return ENERGY.apply(pLocation);
    }
    public static RenderType energy2(ResourceLocation pLocation) {
        return ENERGY2.apply(pLocation);
    }
    public static RenderType lightning(ResourceLocation pLocation) {
        return LIGHTNING.apply(pLocation);
    }
    public static RenderType kiblast(ResourceLocation pLocation) {
        return KI_BLAST.apply(pLocation);
    }
    public static RenderType ki_rendertype(ResourceLocation pLocation) {
        return KI_RENDERTYPE.apply(pLocation);
    }
}