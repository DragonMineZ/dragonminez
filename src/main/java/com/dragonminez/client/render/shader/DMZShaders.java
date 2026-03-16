package com.dragonminez.client.render.shader;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ModRenderTypes;
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
	public static ShaderInstance auraSmoothShader;
	public static ShaderInstance auraSharpShader;
	public static ShaderInstance auraSparkingShader;
	public static ShaderInstance lightningShader;
	public static ShaderInstance outlineShader;

	@SubscribeEvent
	public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
						ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "aura_smooth"),
						DefaultVertexFormat.POSITION_COLOR_NORMAL),
				shaderInstance -> auraSmoothShader = shaderInstance);

		event.registerShader(new ShaderInstance(event.getResourceProvider(),
						ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "aura_sharp"),
						DefaultVertexFormat.POSITION_COLOR_NORMAL),
				shaderInstance -> auraSharpShader = shaderInstance);

		event.registerShader(new ShaderInstance(event.getResourceProvider(),
						ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "aura_sparking"),
						DefaultVertexFormat.POSITION_COLOR_NORMAL),
				shaderInstance -> auraSparkingShader = shaderInstance);

		event.registerShader(new ShaderInstance(event.getResourceProvider(),
						ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "lightning"),
						DefaultVertexFormat.POSITION_COLOR_NORMAL),
				shaderInstance -> lightningShader = shaderInstance);

		event.registerShader(new ShaderInstance(event.getResourceProvider(),
						ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "transformation_mask"),
						DefaultVertexFormat.NEW_ENTITY),
				shaderInstance -> outlineShader = shaderInstance);
	}
}