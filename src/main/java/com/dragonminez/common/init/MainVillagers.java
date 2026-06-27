package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

/**
 * Registro del oficio "Capsule Corp Assistant" y su punto de interés (POI).
 *
 * <p>El bloque de trabajo es el {@code fuel_generator} del mod: cualquier aldeano
 * desempleado que esté cerca de uno sin dueño adoptará este oficio. Los intercambios
 * (mapas de estructuras) se añaden en {@code ForgeCommonEvents#onVillagerTrades}.
 */
public final class MainVillagers {

	public static final DeferredRegister<PoiType> POI_TYPES =
			DeferredRegister.create(ForgeRegistries.POI_TYPES, Reference.MOD_ID);

	public static final DeferredRegister<VillagerProfession> PROFESSIONS =
			DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Reference.MOD_ID);

	/** POI asociado a todos los blockstates del fuel generator. */
	public static final RegistryObject<PoiType> CAPSULE_CORP_POI = POI_TYPES.register("capsule_corp",
			() -> new PoiType(fuelGeneratorStates(), 1, 1));

	/** El oficio en sí. Usa el POI de arriba como sitio de trabajo. */
	public static final RegistryObject<VillagerProfession> CAPSULE_CORP_ASSISTANT =
			PROFESSIONS.register("capsule_corp_assistant",
					() -> new VillagerProfession(
							"capsule_corp_assistant",
							holder -> holder.is(CAPSULE_CORP_POI.getKey()),
							holder -> holder.is(CAPSULE_CORP_POI.getKey()),
							ImmutableSet.of(),
							ImmutableSet.of(),
							SoundEvents.VILLAGER_WORK_CARTOGRAPHER));

	private static Set<BlockState> fuelGeneratorStates() {
		return ImmutableSet.copyOf(MainBlocks.FUEL_GENERATOR.get().getStateDefinition().getPossibleStates());
	}

	public static void register(IEventBus modEventBus) {
		POI_TYPES.register(modEventBus);
		PROFESSIONS.register(modEventBus);
	}

	private MainVillagers() {
	}
}
