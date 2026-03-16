package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DMZShaders {
	public static ShaderInstance auraShader;
	public static ShaderInstance lightningShader;

	@SubscribeEvent
	public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
		// Registra el shader del Aura usando POSITION_COLOR_NORMAL tal como lo pide el aura.json
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
						ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "aura"),
						DefaultVertexFormat.POSITION_COLOR_NORMAL),
				shaderInstance -> auraShader = shaderInstance);

		// Registra el shader de los Relámpagos
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
						ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "lightning"),
						DefaultVertexFormat.POSITION_COLOR_NORMAL),
				shaderInstance -> lightningShader = shaderInstance);
	}
}