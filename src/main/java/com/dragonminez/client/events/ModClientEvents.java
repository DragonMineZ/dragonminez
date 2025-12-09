package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.init.blocks.renderer.DragonBallBlockRenderer;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.MainBlockEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        KeyBinds.registerAll(event);
    }

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			//Bloques
			BlockEntityRenderers.register(MainBlockEntities.DRAGON_BALL_BLOCK_ENTITY.get(), DragonBallBlockRenderer::new);
		});
	}
}
