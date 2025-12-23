package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class DMZAdvancementsProvider extends AdvancementProvider {
	private static ServerLevel serverLevel;

	public DMZAdvancementsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, List.of(new DMZAdvancements()));
	}

	private static class DMZAdvancements implements AdvancementSubProvider {
		@Override
		public void generate(HolderLookup.Provider provider, Consumer<Advancement> consumer) {
			Advancement root = Advancement.Builder.advancement()
					.display(
							MainItems.DBALL4_BLOCK_ITEM.get(), // Ítem de muestra
							Component.translatable("advancements.dragonminez.root.title"), // Título
							Component.translatable("advancements.dragonminez.root.description"), // Descripción
							ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/block/rocky_stone.png"), // Textura de fondo
							FrameType.TASK, true, true, false
					) // Tipo de marco, si se muestra en la esquina superior derecha, si se muestra en el chat y si se oculta en los Logros ("Logro Oculto/Secreto")
					.addCriterion("first_spawn_in_world", // Nombre del criterio
							PlayerTrigger.TriggerInstance.located(EntityPredicate.Builder.entity().of(EntityType.PLAYER).build())) // Criterio
					.rewards(AdvancementRewards.Builder.experience(0)) // Recompensa de experiencia (Se pueden poner más tipos xd)
					.save(consumer, "dragonminez:root"); // Logro "raíz" o "inicial"; el primero de todos.

//			Advancement rockybiome = Advancement.Builder.advancement()
//					.parent(root)
//					.display(
//							MainBlocks.ROCKY_STONE.get(),
//							Component.translatable("advancements.dragonminez.rockybiome.title"),
//							Component.translatable("advancements.dragonminez.rockybiome.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("found_rockybiome",
//							PlayerTrigger.TriggerInstance.located(
//									LocationPredicate.Builder.location()
//											.setBiome(ModBiomes.ROCKY).build()
//							)
//					).save(consumer, "dragonminez:rockybiome");

//			Advancement otherworld = Advancement.Builder.advancement()
//					.parent(root)
//					.display(
//							MainBlocks.OTHERWORLD_CLOUD.get(),
//							Component.translatable("advancements.dragonminez.otherworld.title"),
//							Component.translatable("advancements.dragonminez.otherworld.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("found_otherworld",
//							ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.OTHERWORLD_DIM_LEVEL_KEY)
//					).save(consumer, "dragonminez:otherworld");

//			Advancement kaiosama = Advancement.Builder.advancement()
//					.parent(otherworld)
//					.display(
//							MainItems.RED_CAPSULE.get(),
//							Component.translatable("advancements.dragonminez.kaiosama.title"),
//							Component.translatable("advancements.dragonminez.kaiosama.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("kaiosama",
//							PlayerTrigger.TriggerInstance.located(
//									EntityPredicate.Builder.entity()
//											.of(EntityType.PLAYER)
//											.located(LocationPredicate.Builder.location()
//													.setX(MinMaxBounds.Doubles.between(2147482, 2147483))
//													.setY(MinMaxBounds.Doubles.between(2147482, 2147483))
//													.setZ(MinMaxBounds.Doubles.between(2147482, 2147483))
//													.build()
//											).build())
//					).save(consumer, "dragonminez:kaiosama");

//			Advancement invincible = Advancement.Builder.advancement()
//					.parent(root)
//					.display(
//							MainItems.INVENCIBLE_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.invincible.title"),
//							Component.translatable("advancements.dragonminez.invincible.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("invincible",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.INVENCIBLE_ARMOR_HELMET.get(),
//									MainItems.INVENCIBLE_ARMOR_CHESTPLATE.get(),
//									MainItems.INVENCIBLE_ARMOR_LEGGINGS.get(),
//									MainItems.INVENCIBLE_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:invincible");

			Advancement kamilookout = Advancement.Builder.advancement()
					.parent(root) // Este depende de X logro (Solo es orden, no requisito)
					.display(
							Items.CLOCK,
							Component.translatable("advancements.dragonminez.kamilookout.title"),
							Component.translatable("advancements.dragonminez.kamilookout.description"),
							null, FrameType.GOAL, true, true, false
					).addCriterion("kamilookout",
							// Criterio para que se active el logro al llegar a cualquier coordenada entre 2147482 y 2147483 (Muy lejos xd)
							// Esto es para que el logro sea imposible de obtener de manera normal, y lo damos mediante un evento al encontrar la Estructura.
							PlayerTrigger.TriggerInstance.located(
									EntityPredicate.Builder.entity()
											.of(EntityType.PLAYER)
											.located(LocationPredicate.Builder.location()
													.setX(MinMaxBounds.Doubles.between(2147482, 2147483))
													.setY(MinMaxBounds.Doubles.between(2147482, 2147483))
													.setZ(MinMaxBounds.Doubles.between(2147482, 2147483))
													.build()
											).build())
					).save(consumer, "dragonminez:kamilookout");

			Advancement gokuhouse = Advancement.Builder.advancement()
					.parent(kamilookout)
					.display(
							MainBlocks.DBALL4_BLOCK.get(),
							Component.translatable("advancements.dragonminez.gokuhouse.title"),
							Component.translatable("advancements.dragonminez.gokuhouse.description"),
							null, FrameType.GOAL, true, true, false
					).addCriterion("gokuhouse",
							PlayerTrigger.TriggerInstance.located(
									EntityPredicate.Builder.entity()
											.of(EntityType.PLAYER)
											.located(LocationPredicate.Builder.location()
													.setX(MinMaxBounds.Doubles.between(2147482, 2147483))
													.setY(MinMaxBounds.Doubles.between(2147482, 2147483))
													.setZ(MinMaxBounds.Doubles.between(2147482, 2147483))
													.build()
											).build())
					).save(consumer, "dragonminez:gokuhouse");

			Advancement roshihouse = Advancement.Builder.advancement()
					.parent(kamilookout)
					.display(
							Blocks.SAND,
							Component.translatable("advancements.dragonminez.roshihouse.title"),
							Component.translatable("advancements.dragonminez.roshihouse.description"),
							null, FrameType.GOAL, true, true, false
					).addCriterion("roshihouse",
							PlayerTrigger.TriggerInstance.located(
									EntityPredicate.Builder.entity()
											.of(EntityType.PLAYER)
											.located(LocationPredicate.Builder.location()
													.setX(MinMaxBounds.Doubles.between(2147482, 2147483))
													.setY(MinMaxBounds.Doubles.between(2147482, 2147483))
													.setZ(MinMaxBounds.Doubles.between(2147482, 2147483))
													.build()
											).build())
					).save(consumer, "dragonminez:roshihouse");

//			Advancement timechamber = Advancement.Builder.advancement()
//					.parent(kamilookout)
//					.display(
//							MainItems.VEGETA_Z_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.timechamber.title"),
//							Component.translatable("advancements.dragonminez.timechamber.description"),
//							null, FrameType.CHALLENGE, true, true, false
//					).addCriterion("timechamber",
//							ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.TIME_CHAMBER_DIM_LEVEL_KEY)
//					).save(consumer, "dragonminez:timechamber");

			Advancement nimbus = Advancement.Builder.advancement()
					.parent(kamilookout)
					.display(
							MainItems.NUBE_ITEM.get(),
							Component.translatable("advancements.dragonminez.nimbus.title"),
							Component.translatable("advancements.dragonminez.nimbus.description"),
							null, FrameType.GOAL, true, true, false
					).addCriterion("nimbus",
							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.NUBE_ITEM.get())
					).save(consumer, "dragonminez:nimbus");

			Advancement radar = Advancement.Builder.advancement()
					.parent(root)
					.display(
							MainItems.DBALL_RADAR_ITEM.get(),
							Component.translatable("advancements.dragonminez.radar.title"),
							Component.translatable("advancements.dragonminez.radar.description"),
							null, FrameType.GOAL, true, true, false
					).addCriterion("radar",
							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.DBALL_RADAR_ITEM.get())
					).save(consumer, "dragonminez:radar");

			Advancement dball1 = Advancement.Builder.advancement()
					.parent(radar)
					.display(
							MainItems.DBALL1_BLOCK_ITEM.get(),
							Component.translatable("advancements.dragonminez.dball1.title"),
							Component.translatable("advancements.dragonminez.dball1.description"),
							null, FrameType.TASK, true, true, false
					).addCriterion("dball1",
							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.DBALL1_BLOCK_ITEM.get())
					).save(consumer, "dragonminez:dball1");

			Advancement dball7 = Advancement.Builder.advancement()
					.parent(dball1)
					.display(
							MainItems.DBALL7_BLOCK_ITEM.get(),
							Component.translatable("advancements.dragonminez.dball7.title"),
							Component.translatable("advancements.dragonminez.dball7.description"),
							null, FrameType.CHALLENGE, true, true, true
					).addCriterion("dball7",
							InventoryChangeTrigger.TriggerInstance.hasItems(
									MainItems.DBALL1_BLOCK_ITEM.get(),
									MainItems.DBALL2_BLOCK_ITEM.get(),
									MainItems.DBALL3_BLOCK_ITEM.get(),
									MainItems.DBALL4_BLOCK_ITEM.get(),
									MainItems.DBALL5_BLOCK_ITEM.get(),
									MainItems.DBALL6_BLOCK_ITEM.get(),
									MainItems.DBALL7_BLOCK_ITEM.get())
					).save(consumer, "dragonminez:dball7");

//			Advancement namekdim = Advancement.Builder.advancement()
//					.parent(root)
//					.display(
//							MainBlocks.NAMEK_GRASS_BLOCK.get(),
//							Component.translatable("advancements.dragonminez.namekdim.title"),
//							Component.translatable("advancements.dragonminez.namekdim.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("namekdim",
//							ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(ModDimensions.NAMEK_DIM_LEVEL_KEY)
//					).save(consumer, "dragonminez:namekdim");
//
//			Advancement patriarca = Advancement.Builder.advancement()
//					.parent(namekdim)
//					.display(
//							MainItems.PICCOLO_ARMOR_CHESTPLATE_CAPE.get(),
//							Component.translatable("advancements.dragonminez.patriarca.title"),
//							Component.translatable("advancements.dragonminez.patriarca.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("patriarca",
//							PlayerTrigger.TriggerInstance.located(
//									EntityPredicate.Builder.entity()
//											.of(EntityType.PLAYER)
//											.located(LocationPredicate.Builder.location()
//													.setX(MinMaxBounds.Doubles.between(2147482, 2147483))
//													.setY(MinMaxBounds.Doubles.between(2147482, 2147483))
//													.setZ(MinMaxBounds.Doubles.between(2147482, 2147483))
//													.build()
//											).build())
//					).save(consumer, "dragonminez:patriarca");
//
//			Advancement radarnamek = Advancement.Builder.advancement()
//					.parent(namekdim)
//					.display(
//							MainItems.NAMEKDBALL_RADAR_ITEM.get(),
//							Component.translatable("advancements.dragonminez.radarnamek.title"),
//							Component.translatable("advancements.dragonminez.radarnamek.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("radarnamek",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.NAMEKDBALL_RADAR_ITEM.get())
//					).save(consumer, "dragonminez:radarnamek");
//
//			Advancement namekballs = Advancement.Builder.advancement()
//					.parent(radarnamek)
//					.display(
//							MainItems.DBALL1_NAMEK_BLOCK_ITEM.get(),
//							Component.translatable("advancements.dragonminez.namekballs.title"),
//							Component.translatable("advancements.dragonminez.namekballs.description"),
//							null, FrameType.CHALLENGE, true, true, true
//					).addCriterion("namekballs",
//							InventoryChangeTrigger.TriggerInstance.hasItems(
//									MainItems.DBALL1_NAMEK_BLOCK_ITEM.get(),
//									MainItems.DBALL2_NAMEK_BLOCK_ITEM.get(),
//									MainItems.DBALL3_NAMEK_BLOCK_ITEM.get(),
//									MainItems.DBALL4_NAMEK_BLOCK_ITEM.get(),
//									MainItems.DBALL5_NAMEK_BLOCK_ITEM.get(),
//									MainItems.DBALL6_NAMEK_BLOCK_ITEM.get(),
//									MainItems.DBALL7_NAMEK_BLOCK_ITEM.get())
//					).save(consumer, "dragonminez:namekballs");
//
//			Advancement kikono = Advancement.Builder.advancement()
//					.parent(namekdim)
//					.display(
//							MainItems.KIKONO_SHARD.get(),
//							Component.translatable("advancements.dragonminez.kikono.title"),
//							Component.translatable("advancements.dragonminez.kikono.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("kikono",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.KIKONO_SHARD.get())
//					).save(consumer, "dragonminez:kikono");
//
//			Advancement armorStation = Advancement.Builder.advancement()
//					.parent(kikono)
//					.display(
//							MainBlocks.KIKONO_ARMOR_STATION.get(),
//							Component.translatable("advancements.dragonminez.armorstation.title"),
//							Component.translatable("advancements.dragonminez.armorstation.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("armorstation",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainBlocks.KIKONO_ARMOR_STATION.get())
//					).save(consumer, "dragonminez:armorstation");
//
//			Advancement patternz = Advancement.Builder.advancement()
//					.parent(armorStation)
//					.display(
//							MainItems.BLANK_PATTERN_Z.get(),
//							Component.translatable("advancements.dragonminez.patternz.title"),
//							Component.translatable("advancements.dragonminez.patternz.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("patternz",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.BLANK_PATTERN_Z.get())
//					).save(consumer, "dragonminez:patternz");
//
//			Advancement patternsuper = Advancement.Builder.advancement()
//					.parent(armorStation)
//					.display(
//							MainItems.BLANK_PATTERN_SUPER.get(),
//							Component.translatable("advancements.dragonminez.patternsuper.title"),
//							Component.translatable("advancements.dragonminez.patternsuper.description"),
//							null, FrameType.GOAL, true, true, false
//					).addCriterion("patternsuper",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.BLANK_PATTERN_SUPER.get())
//					).save(consumer, "dragonminez:patternsuper");
//
//			Advancement gokuarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.GOKU_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.gokuarmor.title"),
//							Component.translatable("advancements.dragonminez.gokuarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("gokuarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.GOKU_ARMOR_CHESTPLATE.get(),
//									MainItems.GOKU_ARMOR_LEGGINGS.get(),
//									MainItems.GOKU_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:gokuarmor");
//
//			Advancement gotenarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.GOTEN_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.gotenarmor.title"),
//							Component.translatable("advancements.dragonminez.gotenarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("gotenarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.GOTEN_ARMOR_CHESTPLATE.get(),
//									MainItems.GOTEN_ARMOR_LEGGINGS.get(),
//									MainItems.GOTEN_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:gotenarmor");
//
//			Advancement greatsaiyamanarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.GREAT_SAIYAMAN_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.greatsaiyamanarmor.title"),
//							Component.translatable("advancements.dragonminez.greatsaiyamanarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("greatsaiyamanarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.GREAT_SAIYAMAN_ARMOR_HELMET.get(),
//									MainItems.GREAT_SAIYAMAN_ARMOR_CHESTPLATE.get(),
//									MainItems.GREAT_SAIYAMAN_ARMOR_LEGGINGS.get(),
//									MainItems.GREAT_SAIYAMAN_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:greatsaiyamanarmor");
//
//			Advancement futuregohanarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.FUTURE_GOHAN_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.futuregohanarmor.title"),
//							Component.translatable("advancements.dragonminez.futuregohanarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("futuregohanarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.FUTURE_GOHAN_ARMOR_CHESTPLATE.get(),
//									MainItems.FUTURE_GOHAN_ARMOR_LEGGINGS.get(),
//									MainItems.FUTURE_GOHAN_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:futuregohanarmor");
//
//			Advancement vegetaarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.VEGETA_SAIYAN_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.vegetaarmor.title"),
//							Component.translatable("advancements.dragonminez.vegetaarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("vegetaarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.VEGETA_SAIYAN_ARMOR_CHESTPLATE.get(),
//									MainItems.VEGETA_SAIYAN_ARMOR_LEGGINGS.get(),
//									MainItems.VEGETA_SAIYAN_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:vegetaarmor");
//
//			Advancement vegettoarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.VEGETTO_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.vegettoarmor.title"),
//							Component.translatable("advancements.dragonminez.vegettoarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("vegettoarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.VEGETTO_ARMOR_CHESTPLATE.get(),
//									MainItems.VEGETTO_ARMOR_LEGGINGS.get(),
//									MainItems.VEGETTO_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:vegettoarmor");
//
//			Advancement gogetaarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.GOGETA_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.gogetaarmor.title"),
//							Component.translatable("advancements.dragonminez.gogetaarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("gogetaarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.GOGETA_ARMOR_CHESTPLATE.get(),
//									MainItems.GOGETA_ARMOR_LEGGINGS.get(),
//									MainItems.GOGETA_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:gogetaarmor");
//
//			Advancement piccoloarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.PICCOLO_ARMOR_CHESTPLATE_CAPE.get(),
//							Component.translatable("advancements.dragonminez.piccoloarmor.title"),
//							Component.translatable("advancements.dragonminez.piccoloarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("piccoloarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.PICCOLO_ARMOR_HELMET.get(),
//									MainItems.PICCOLO_ARMOR_CHESTPLATE_CAPE.get(),
//									MainItems.PICCOLO_ARMOR_LEGGINGS.get(),
//									MainItems.PICCOLO_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:piccoloarmor");
//
//			Advancement demonbluegiarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.DEMON_GI_BLUE_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.demonbluegiarmor.title"),
//							Component.translatable("advancements.dragonminez.demonbluegiarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("demonbluegiarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.DEMON_GI_BLUE_ARMOR_CHESTPLATE.get(),
//									MainItems.DEMON_GI_BLUE_ARMOR_LEGGINGS.get(),
//									MainItems.DEMON_GI_BLUE_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:demonbluegiarmor");
//
//			Advancement bardockarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.BARDOCK_DBZ_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.bardockarmor.title"),
//							Component.translatable("advancements.dragonminez.bardockarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("bardockarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.BARDOCK_DBZ_ARMOR_CHESTPLATE.get(),
//									MainItems.BARDOCK_DBZ_ARMOR_LEGGINGS.get(),
//									MainItems.BARDOCK_DBZ_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:bardockarmor");
//
//			Advancement turlesarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.TURLES_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.turlesarmor.title"),
//							Component.translatable("advancements.dragonminez.turlesarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("turlesarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.TURLES_ARMOR_CHESTPLATE.get(),
//									MainItems.TURLES_ARMOR_LEGGINGS.get(),
//									MainItems.TURLES_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:turlesarmor");
//
//			Advancement tienarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.TIEN_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.tienarmor.title"),
//							Component.translatable("advancements.dragonminez.tienarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("tienarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.TIEN_ARMOR_CHESTPLATE.get(),
//									MainItems.TIEN_ARMOR_LEGGINGS.get(),
//									MainItems.TIEN_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:tienarmor");
//
//			Advancement trunksarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.TRUNKS_Z_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.trunksarmor.title"),
//							Component.translatable("advancements.dragonminez.trunksarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("trunksarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.TRUNKS_Z_ARMOR_CHESTPLATE.get(),
//									MainItems.TRUNKS_Z_ARMOR_LEGGINGS.get(),
//									MainItems.TRUNKS_Z_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:trunksarmor");
//
//			Advancement brolyarmor = Advancement.Builder.advancement()
//					.parent(patternsuper)
//					.display(
//							MainItems.BROLY_SUPER_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.brolyarmor.title"),
//							Component.translatable("advancements.dragonminez.brolyarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("brolyarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.BROLY_SUPER_ARMOR_CHESTPLATE.get(),
//									MainItems.BROLY_SUPER_ARMOR_LEGGINGS.get(),
//									MainItems.BROLY_SUPER_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:brolyarmor");
//
//			Advancement shinarmor = Advancement.Builder.advancement()
//					.parent(patternz)
//					.display(
//							MainItems.SHIN_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.shinarmor.title"),
//							Component.translatable("advancements.dragonminez.shinarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("shinarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.SHIN_ARMOR_CHESTPLATE.get(),
//									MainItems.SHIN_ARMOR_LEGGINGS.get(),
//									MainItems.SHIN_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:shinarmor");
//
//			Advancement blackarmor = Advancement.Builder.advancement()
//					.parent(patternsuper)
//					.display(
//							MainItems.BLACK_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.blackarmor.title"),
//							Component.translatable("advancements.dragonminez.blackarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("blackarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.BLACK_ARMOR_CHESTPLATE.get(),
//									MainItems.BLACK_ARMOR_LEGGINGS.get(),
//									MainItems.BLACK_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:blackarmor");
//
//			Advancement zamasuarmor = Advancement.Builder.advancement()
//					.parent(patternsuper)
//					.display(
//							MainItems.ZAMASU_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.zamasuarmor.title"),
//							Component.translatable("advancements.dragonminez.zamasuarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("zamasuarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.ZAMASU_ARMOR_CHESTPLATE.get(),
//									MainItems.ZAMASU_ARMOR_LEGGINGS.get(),
//									MainItems.ZAMASU_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:zamasuarmor");
//
//			Advancement fusionzamasuarmor = Advancement.Builder.advancement()
//					.parent(patternsuper)
//					.display(
//							MainItems.FUSION_ZAMASU_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.fusionzamasuarmor.title"),
//							Component.translatable("advancements.dragonminez.fusionzamasuarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("fusionzamasuarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.FUSION_ZAMASU_ARMOR_CHESTPLATE.get(),
//									MainItems.FUSION_ZAMASU_ARMOR_LEGGINGS.get(),
//									MainItems.FUSION_ZAMASU_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:fusionzamasuarmor");
//
//			Advancement pridetrooparmor = Advancement.Builder.advancement()
//					.parent(patternsuper)
//					.display(
//							MainItems.PRIDE_TROOPS_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.pridetroopsarmor.title"),
//							Component.translatable("advancements.dragonminez.pridetroopsarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("pridetrooparmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.PRIDE_TROOPS_ARMOR_CHESTPLATE.get(),
//									MainItems.PRIDE_TROOPS_ARMOR_LEGGINGS.get(),
//									MainItems.PRIDE_TROOPS_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:pridetrooparmor");
//
//			Advancement hitarmor = Advancement.Builder.advancement()
//					.parent(patternsuper)
//					.display(
//							MainItems.HIT_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.hitarmor.title"),
//							Component.translatable("advancements.dragonminez.hitarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("hitarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.HIT_ARMOR_CHESTPLATE.get(),
//									MainItems.HIT_ARMOR_LEGGINGS.get(),
//									MainItems.HIT_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:hitarmor");
//
//			Advancement gasarmor = Advancement.Builder.advancement()
//					.parent(patternsuper)
//					.display(
//							MainItems.GAS_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.gasarmor.title"),
//							Component.translatable("advancements.dragonminez.gasarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("gasarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.GAS_ARMOR_CHESTPLATE.get(),
//									MainItems.GAS_ARMOR_LEGGINGS.get(),
//									MainItems.GAS_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:gasarmor");
//
//			Advancement gammasarmor = Advancement.Builder.advancement()
//					.parent(patternsuper)
//					.display(
//							MainItems.GAMMA1_ARMOR_CHESTPLATE.get(),
//							Component.translatable("advancements.dragonminez.gammasarmor.title"),
//							Component.translatable("advancements.dragonminez.gammasarmor.description"),
//							null, FrameType.TASK, true, true, false
//					).addCriterion("gammasarmor",
//							InventoryChangeTrigger.TriggerInstance.hasItems(MainItems.GAMMA1_ARMOR_CHESTPLATE.get(),
//									MainItems.GAMMA1_ARMOR_LEGGINGS.get(),
//									MainItems.GAMMA1_ARMOR_BOOTS.get(),
//									MainItems.GAMMA2_ARMOR_CHESTPLATE.get(),
//									MainItems.GAMMA2_ARMOR_LEGGINGS.get(),
//									MainItems.GAMMA2_ARMOR_BOOTS.get())
//					).save(consumer, "dragonminez:gammasarmor");
		}
	}
}