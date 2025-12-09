package com.dragonminez.common.init.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * @author Original author: Choonster <a href="https://github.com/Choonster-Minecraft-Mods/TestMod3">...</a>
 * <p>
 * Edited by MineAcademy
 */
public class SimpleFluid extends FluidType {
	private final ResourceLocation stillTexture;
	private final ResourceLocation flowTexture;
	private final ResourceLocation overlayTexture;
	private final int tintColor;
	private final Vector3f fogColor;
	private final int fogStart;
	private final int fogEnd;

	public SimpleFluid(int color, FluidType.Properties properties) {
		super(properties);
		this.stillTexture = new ResourceLocation("block/water_still");
		this.flowTexture = new ResourceLocation("block/water_flow");
		this.overlayTexture = new ResourceLocation("block/water_overlay");
		this.tintColor = toAlpha(color);
		this.fogColor = new Vector3f((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F);
		this.fogStart = 24;
		this.fogEnd = 48;
	}

	private int toAlpha(int color) {
		int red = (color >> 16) & 0xFF;
		int green = (color >> 8) & 0xFF;
		int blue = color & 0xFF;
		return (0xA1 << 24) | (red << 16) | (green << 8) | blue;
	}

	@Override
	public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
		consumer.accept(new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return stillTexture;
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return flowTexture;
			}

			@Override
			public ResourceLocation getOverlayTexture() {
				return overlayTexture;
			}

			@Override
			public int getTintColor() {
				return tintColor;
			}

			@Override
			public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
				return fogColor;
			}

			@Override
			public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
				RenderSystem.setShaderFogStart(fogStart);
				RenderSystem.setShaderFogEnd(fogEnd);
			}
		});
	}
}
