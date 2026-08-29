package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.loot.AddMusicDiscModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class MainLootModifiers {
	public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
			DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Reference.MOD_ID);

	public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddMusicDiscModifier>> ADD_MUSIC_DISC =
			LOOT_MODIFIERS.register("add_music_disc", () -> AddMusicDiscModifier.CODEC);

	public static void register(IEventBus eventBus) {
		LOOT_MODIFIERS.register(eventBus);
	}
}
