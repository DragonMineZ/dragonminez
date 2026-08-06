package com.dragonminez.common.init.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.Consumer;

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
        this.stillTexture = ResourceLocation.withDefaultNamespace("block/water_still");
        this.flowTexture = ResourceLocation.withDefaultNamespace("block/water_flow");
        this.overlayTexture = ResourceLocation.withDefaultNamespace("block/water_overlay");

        this.tintColor = toAlpha(color);
        this.fogColor = new Vector3f((color >> 16 & 0xFF) / 255F, (color >> 8 & 0xFF) / 255F, (color & 0xFF) / 255F);
        this.fogStart = -8;
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
            public ResourceLocation getStillTexture() { return stillTexture; }

            @Override
            public ResourceLocation getFlowingTexture() { return flowTexture; }

            @Override
            public ResourceLocation getOverlayTexture() { return overlayTexture; }

            @Override
            public int getTintColor() { return tintColor; }

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

    @Override
    public boolean canSwim(Entity entity) {
        return true;
    }

    @Override
    public boolean canDrownIn(LivingEntity entity) {
        return !(entity instanceof WaterAnimal);
    }

    @Override
    public @Nullable PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
        return PathType.WATER;
    }

    @Override
    public @org.jetbrains.annotations.Nullable PathType getAdjacentBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @org.jetbrains.annotations.Nullable Mob mob, PathType originalType) {
        return null;
    }

    @Override
    public boolean supportsBoating(FluidState state, Boat boat) {
        return true;
    }

    @Override
    public boolean canExtinguish(FluidState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}