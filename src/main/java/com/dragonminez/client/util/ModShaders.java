package com.dragonminez.client.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.io.IOException;

public class ModShaders {
    private static ShaderInstance energySphereShader;

    public static void init(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath("dragonminez", "energy_sphere"),
                DefaultVertexFormat.NEW_ENTITY), (shader) -> {
            energySphereShader = shader;
        });
    }

    public static ShaderInstance getEnergySphereShader() {
        return energySphereShader;
    }
}