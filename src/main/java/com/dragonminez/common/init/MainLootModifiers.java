package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.loot.AddMusicDiscModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainLootModifiers {
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
			DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Reference.MOD_ID);

	public static final RegistryObject<Codec<AddMusicDiscModifier>> ADD_MUSIC_DISC =
			LOOT_MODIFIERS.register("add_music_disc", AddMusicDiscModifier.CODEC);

	public static void register(IEventBus eventBus) {
		LOOT_MODIFIERS.register(eventBus);
	}
}
