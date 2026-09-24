package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DMZEntityTypeTagGenerator extends EntityTypeTagsProvider {
	public DMZEntityTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
									 @Nullable ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, Reference.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.@NotNull Provider provider) {
		this.tag(MainTags.EntityTypes.FRIEZA_SOLDIERS)
				.add(MainEntities.SAGA_FRIEZA_SOLDIER.get())
				.add(MainEntities.SAGA_FRIEZA_SOLDIER2.get())
				.add(MainEntities.SAGA_FRIEZA_SOLDIER3.get());

		this.tag(MainTags.EntityTypes.SAIBAMEN)
				.add(MainEntities.SAGA_SAIBAMAN.get())
				.add(MainEntities.SAGA_SAIBAMAN2.get())
				.add(MainEntities.SAGA_SAIBAMAN3.get())
				.add(MainEntities.SAGA_SAIBAMAN4.get())
				.add(MainEntities.SAGA_SAIBAMAN5.get())
				.add(MainEntities.SAGA_SAIBAMAN6.get());

		this.tag(MainTags.EntityTypes.RED_RIBBON_ROBOTS)
				.add(MainEntities.RED_RIBBON_ROBOT1.get())
				.add(MainEntities.RED_RIBBON_ROBOT2.get())
				.add(MainEntities.RED_RIBBON_ROBOT3.get());

		npc(MainTags.EntityTypes.NPC_SUPERSAIYAN, "saga_goku_mid_ssj", "saga_vegeta_mid_ssj", "saga_ftrunks_ssj", "saga_ftrunks_kid_ssj", "saga_gohan_mid_ssj", "saga_fgohan_ssj", "saga_goku_end_ssj", "saga_vegeta_end_ssj", "saga_gohan_end_ssj", "saga_goten_ssj", "saga_kid_trunks_ssj", "saga_gotenks_ssj", "saga_vegetto_ssj", "saga_goku_gt_ssj", "saga_vegeta_gt_ssj", "saga_gohan_gt_ssj", "saga_goten_gt_ssj", "saga_trunks_gt_ssj", "saga_broly_ssj", "saga_broly_ssj_restricted");
		npc(MainTags.EntityTypes.NPC_SUPERSAIYAN2, "saga_goku_end_ssj2", "saga_vegeta_end_ssj2", "saga_gohan_mid_ssj2", "saga_gohan_end_ssj2", "saga_vegeta_gt_ssj2");
		npc(MainTags.EntityTypes.NPC_SUPERSAIYAN3, "saga_goku_end_ssj3", "saga_gotenks_ssj3", "saga_goku_gt_ssj3");
		npc(MainTags.EntityTypes.NPC_SUPERSAIYAN4, "saga_goku_gt_ssj4", "saga_vegeta_gt_ssj4", "saga_gogeta_ssj4");
		npc(MainTags.EntityTypes.NPC_GINYUFORCE, "saga_guldo", "saga_recoome", "saga_burter", "saga_jeice", "saga_ginyu");
		npc(MainTags.EntityTypes.NPC_CELLSAGA, "saga_a16", "saga_a17", "saga_a18", "saga_a19", "saga_drgero", "saga_cell_imperfect", "saga_cell_semiperfect", "saga_cell_perfect", "saga_cell_jr", "saga_piccolo_kami", "saga_ftrunks_base", "saga_ftrunks_ssj", "saga_vegeta_mid_base", "saga_vegeta_mid_ssj", "saga_goku_mid_base", "saga_goku_mid_ssj", "saga_gohan_mid_base", "saga_krillin", "saga_tien_early", "saga_yamcha");
		npc(MainTags.EntityTypes.NPC_SAIYANSAGA, "saga_raditz", "saga_nappa", "saga_vegeta", "saga_ozaruvegeta", "saga_saibaman1", "saga_saibaman2", "saga_saibaman3", "saga_saibaman4", "saga_saibaman5", "saga_saibaman6", "saga_piccolo", "saga_goku_early", "saga_kid_gohan", "saga_chaoz", "saga_tien_early", "saga_yamcha", "saga_krillin");
		npc(MainTags.EntityTypes.NPC_FRIEZASAGA, "saga_cui", "saga_dodoria", "saga_zarbon", "saga_zarbont1", "saga_friezasoldier01", "saga_friezasoldier02", "saga_friezasoldier03", "saga_guldo", "saga_recoome", "saga_burter", "saga_jeice", "saga_ginyu", "saga_ginyu_goku", "saga_nail", "saga_vegeta_namek", "saga_frieza_first", "saga_frieza_second", "saga_frieza_third", "saga_frieza_base", "saga_frieza_fp");
		npc(MainTags.EntityTypes.NPC_FRIEZAFORMS, "saga_frieza_first", "saga_frieza_second", "saga_frieza_third", "saga_frieza_base", "saga_frieza_fp", "saga_mecha_frieza");
		npc(MainTags.EntityTypes.NPC_ANDROIDS, "saga_a8", "saga_a13", "saga_super_a13", "saga_a14", "saga_a15", "saga_a16", "saga_a17", "saga_a18", "saga_a19", "saga_a18_gt", "saga_super_17");
		npc(MainTags.EntityTypes.NPC_BUUSAGA, "saga_spopovitch", "saga_puipui", "saga_yakon", "saga_dabura", "saga_babidi", "saga_vegeta_majin", "saga_buufat", "saga_evilbuu", "mini_buu", "saga_superbuu", "saga_superbuu_piccolo", "saga_superbuu_gotenks", "saga_superbuu_gohan", "saga_kidbuu", "saga_goten_ssj", "saga_kid_trunks_ssj", "saga_gotenks", "saga_gotenks_ssj", "saga_gotenks_ssj3", "saga_vegetto_base", "saga_vegetto_ssj", "saga_gohan_end_ultimate", "saga_shin", "saga_kibito");
		npc(MainTags.EntityTypes.NPC_MAJINBUU, "saga_buufat", "saga_evilbuu", "mini_buu", "saga_superbuu", "saga_superbuu_piccolo", "saga_superbuu_gotenks", "saga_superbuu_gohan", "saga_kidbuu");
		npc(MainTags.EntityTypes.NPC_REDRIBBON, "saga_colonel_silver", "saga_general_blue", "saga_sergeant_metallic", "saga_a8", "saga_general_black_robot", "saga_general_red", "saga_tao_pai_pai", "saga_tao_pai_pai_cyborg", "saga_ninja_murasaki");
		npc(MainTags.EntityTypes.NPC_PILAFGANG, "saga_pilaf_robot", "saga_shu_robot", "saga_mai_robot", "saga_pilaf_robot_fused");
		npc(MainTags.EntityTypes.NPC_KINGPICCOLO, "saga_piccolo_daimao_old", "saga_piccolo_daimao_young", "saga_drum", "saga_tambourine", "saga_majunia", "saga_majunia_giant");
		npc(MainTags.EntityTypes.NPC_BABAFIGHTERS, "saga_masked_warrior", "saga_dracula", "saga_akkuman", "saga_invisible_man", "saga_mummy");
		npc(MainTags.EntityTypes.NPC_WORLDTOURNAMENT, "saga_kid_goku", "saga_teen_yamcha", "saga_kid_krillin", "saga_giran", "saga_nam", "saga_jackie_chun", "saga_jackie_chun_fp", "saga_goku_t23", "saga_yamcha_t23", "saga_tien_t23", "saga_chichi", "saga_young_tien", "saga_young_yamcha");
		npc(MainTags.EntityTypes.NPC_ZFIGHTERS, "saga_goku_early", "saga_piccolo", "saga_krillin", "saga_tien_early", "saga_yamcha", "saga_chaoz", "saga_kid_gohan", "saga_yajirobe", "saga_gohan_mid_base", "saga_vegeta_mid_base", "saga_ftrunks_base", "saga_goku_mid_base", "saga_goku_end_base", "saga_vegeta_end_base", "saga_gohan_end_base", "saga_goten", "saga_kid_trunks", "saga_a18", "saga_videl", "saga_uub", "saga_majuub");
		npc(MainTags.EntityTypes.NPC_COOLERFORCE, "saga_salza", "saga_dore", "saga_neiz", "saga_cooler", "saga_cooler_5ta", "saga_metal_cooler", "saga_gete_robot");
		npc(MainTags.EntityTypes.NPC_BOJACKCREW, "saga_zangya", "saga_gokua", "saga_bido", "saga_bujin", "saga_bojack", "saga_bojack_fp");
		npc(MainTags.EntityTypes.NPC_BROLY, "saga_paragus", "saga_broly_base", "saga_broly_ssj_restricted", "saga_broly_ssj", "saga_broly_lssj", "saga_bio_broly", "saga_bio_broly_giant");
		npc(MainTags.EntityTypes.NPC_MOVIEVILLAINS, "saga_garlick_jr", "saga_garlick_jr_transformed", "saga_dr_wheelo", "saga_turles", "saga_slug_soldier", "saga_slug", "saga_slug_giant", "saga_salza", "saga_dore", "saga_neiz", "saga_cooler", "saga_cooler_5ta", "saga_metal_cooler", "saga_a13", "saga_a14", "saga_a15", "saga_super_a13", "saga_broly_base", "saga_broly_ssj", "saga_broly_lssj", "saga_zangya", "saga_gokua", "saga_bido", "saga_bujin", "saga_bojack", "saga_bojack_fp", "saga_bio_broly", "saga_janemba_fat", "saga_super_janemba", "saga_hirudegarn");
		npc(MainTags.EntityTypes.NPC_GTVILLAINS, "saga_ledgic", "saga_bon_para", "saga_don_para", "saga_son_para", "saga_luud", "saga_rilldo", "saga_metal_rilldo", "saga_hyper_rilldo", "saga_baby_vegeta", "saga_super_baby_vegeta", "saga_super_baby_vegeta2", "saga_baby_golden_ozaru", "saga_baby", "saga_super_17", "saga_gohan_gt_baby", "saga_goten_gt_baby", "saga_trunks_gt_baby", "saga_liang_xing_long", "saga_wu_xing_long", "saga_liu_xing_long", "saga_qi_xing_long", "saga_neo_shenron", "saga_eis_shenron", "saga_syn_shenron", "saga_omega_shenron");
		npc(MainTags.EntityTypes.NPC_SHADOWDRAGONS, "saga_liang_xing_long", "saga_wu_xing_long", "saga_wu_xing_long_transformed", "saga_liu_xing_long", "saga_liu_xing_long_transformed", "saga_qi_xing_long", "saga_qi_xing_long_transformed", "saga_neo_shenron", "saga_neo_shenron_transformed", "saga_eis_shenron", "saga_syn_shenron", "saga_omega_shenron");
		npc(MainTags.EntityTypes.NPC_FUSIONS, "saga_gotenks", "saga_gotenks_ssj", "saga_gotenks_ssj3", "saga_vegetto_base", "saga_vegetto_ssj", "saga_gogeta_ssj4");
		npc(MainTags.EntityTypes.NPC_GIANTS, "saga_ozaru", "saga_ozaruvegeta", "saga_majunia_giant", "saga_slug_giant", "saga_bio_broly_giant", "saga_baby_golden_ozaru", "saga_hirudegarn", "saga_hirudegarn_incomplete1", "saga_hirudegarn_incomplete2", "saga_super_hirudegarn");
		npc(MainTags.EntityTypes.NPC_FINALBOSSES, "saga_piccolo_daimao_young", "saga_frieza_fp", "saga_cell_superperfect", "saga_kidbuu", "saga_super_janemba", "saga_omega_shenron", "saga_broly_lssj", "saga_cooler_5ta", "saga_metal_cooler", "saga_bojack_fp", "saga_super_hirudegarn", "saga_super_17", "saga_super_baby_vegeta2", "saga_super_a13", "saga_slug_giant", "saga_dr_wheelo", "saga_turles", "saga_garlick_jr_transformed");
	}

	private void npc(TagKey<EntityType<?>> tag, String... names) {
		IntrinsicTagAppender<EntityType<?>> appender = this.tag(tag);
		for (String name : names) {
			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
			if (type == null) throw new IllegalStateException("Unknown entity for tag " + tag.location() + ": " + name);
			appender.add(type);
		}
	}
}
