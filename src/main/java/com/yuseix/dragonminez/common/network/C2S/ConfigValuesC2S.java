package com.yuseix.dragonminez.common.network.C2S;

import com.yuseix.dragonminez.common.config.DMZGeneralConfig;
import com.yuseix.dragonminez.common.config.races.*;
import com.yuseix.dragonminez.common.config.races.transformations.*;
import com.yuseix.dragonminez.common.network.ModMessages;
import com.yuseix.dragonminez.common.network.S2C.PacketSyncConfig;
import com.yuseix.dragonminez.common.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.common.stats.DMZStatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ConfigValuesC2S {

    public ConfigValuesC2S() {
    }

    public ConfigValuesC2S(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(ConfigValuesC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();

            if (player != null) {
                DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
                    ModMessages.sendToPlayer(new PacketSyncConfig("maxattributes", DMZGeneralConfig.MAX_ATTRIBUTE_VALUE.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("zpoints_cost", DMZGeneralConfig.MULTIPLIER_ZPOINTS_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_multiplier", DMZGeneralConfig.MULTIPLIER_MAJIN.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("tree_might_multiplier", DMZGeneralConfig.MULTIPLIER_TREE_MIGHT.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("transfTPCost", DMZGeneralConfig.TPCOST_TRANSFORMATIONS.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("buyableTP", DMZGeneralConfig.TRANSFORMATIONS_WITH_TP.get() ? 1 : 0), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("babaCooldown", DMZGeneralConfig.BABA_COOLDOWN.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("babaDuration", DMZGeneralConfig.BABA_DURATION.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("jumpLevels", DMZGeneralConfig.JUMP_TP_COST_LEVELS.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("flyLevels", DMZGeneralConfig.FLY_TP_COST_LEVELS.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("meditationLevels", DMZGeneralConfig.MEDITATION_TP_COST_LEVELS.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("potUnlockLevels", DMZGeneralConfig.POTUNLOCK_TP_COST_LEVELS.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("kiManipLevels", DMZGeneralConfig.KI_MANIPULATION_TP_COST_LEVELS.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("kiControlLevels", DMZGeneralConfig.KI_CONTROL_TP_COST_LEVELS.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("jumpMaster", DMZGeneralConfig.JUMP_TP_COST_MASTER.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("flyMaster", DMZGeneralConfig.FLY_TP_COST_MASTER.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("meditationMaster", DMZGeneralConfig.MEDITATION_TP_COST_MASTER.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("potUnlockMaster", DMZGeneralConfig.POTUNLOCK_TP_COST_MASTER.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("kiManipMaster", DMZGeneralConfig.KI_MANIPULATION_TP_COST_MASTER.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("kiControlMaster", DMZGeneralConfig.KI_CONTROL_TP_COST_MASTER.get()), player);

                    //WARRIOR
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_human_warrior", DMZHumanConfig.INITIAL_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_saiyan_warrior", DMZSaiyanConfig.INITIAL_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_namek_warrior", DMZNamekConfig.INITIAL_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_bio_warrior", DMZBioAndroidConfig.INITIAL_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_cold_warrior", DMZColdDemonConfig.INITIAL_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_majin_warrior", DMZMajinConfig.INITIAL_STR_WARRIOR.get()), player);

                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_human_warrior", DMZHumanConfig.INITIAL_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_saiyan_warrior", DMZSaiyanConfig.INITIAL_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_namek_warrior", DMZNamekConfig.INITIAL_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_bio_warrior", DMZBioAndroidConfig.INITIAL_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_cold_warrior", DMZColdDemonConfig.INITIAL_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_majin_warrior", DMZMajinConfig.INITIAL_DEF_WARRIOR.get()), player);

                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_human_warrior", DMZHumanConfig.INITIAL_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_saiyan_warrior", DMZSaiyanConfig.INITIAL_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_namek_warrior", DMZNamekConfig.INITIAL_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_bio_warrior", DMZBioAndroidConfig.INITIAL_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_cold_warrior", DMZColdDemonConfig.INITIAL_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_majin_warrior", DMZMajinConfig.INITIAL_CON_WARRIOR.get()), player);

                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_human_warrior", DMZHumanConfig.INITIAL_KIPWR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_saiyan_warrior", DMZSaiyanConfig.INITIAL_KIPWR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_namek_warrior", DMZNamekConfig.INITIAL_KIPWR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_bio_warrior", DMZBioAndroidConfig.INITIAL_KIPWR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_cold_warrior", DMZColdDemonConfig.INITIAL_KIPWR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_majin_warrior", DMZMajinConfig.INITIAL_KIPWR_WARRIOR.get()), player);

                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_human_warrior", DMZHumanConfig.INITIAL_ENE_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_saiyan_warrior", DMZSaiyanConfig.INITIAL_ENE_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_namek_warrior", DMZNamekConfig.INITIAL_ENE_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_bio_warrior", DMZBioAndroidConfig.INITIAL_ENE_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_cold_warrior", DMZColdDemonConfig.INITIAL_ENE_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_majin_warrior", DMZMajinConfig.INITIAL_ENE_WARRIOR.get()), player);
                    //SPIRITUALIST
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_human_spiritualist", DMZHumanConfig.INITIAL_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_saiyan_spiritualist", DMZSaiyanConfig.INITIAL_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_namek_spiritualist", DMZNamekConfig.INITIAL_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_bio_spiritualist", DMZBioAndroidConfig.INITIAL_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_cold_spiritualist", DMZColdDemonConfig.INITIAL_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_str_majin_spiritualist", DMZMajinConfig.INITIAL_STR_SPIRITUALIST.get()), player);

                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_human_spiritualist", DMZHumanConfig.INITIAL_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_saiyan_spiritualist", DMZSaiyanConfig.INITIAL_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_namek_spiritualist", DMZNamekConfig.INITIAL_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_bio_spiritualist", DMZBioAndroidConfig.INITIAL_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_cold_spiritualist", DMZColdDemonConfig.INITIAL_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_def_majin_spiritualist", DMZMajinConfig.INITIAL_DEF_SPIRITUALIST.get()), player);

                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_human_spiritualist", DMZHumanConfig.INITIAL_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_saiyan_spiritualist", DMZSaiyanConfig.INITIAL_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_namek_spiritualist", DMZNamekConfig.INITIAL_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_bio_spiritualist", DMZBioAndroidConfig.INITIAL_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_cold_spiritualist", DMZColdDemonConfig.INITIAL_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_con_majin_spiritualist", DMZMajinConfig.INITIAL_CON_SPIRITUALIST.get()), player);

                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_human_spiritualist", DMZHumanConfig.INITIAL_KIPWR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_saiyan_spiritualist", DMZSaiyanConfig.INITIAL_KIPWR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_namek_spiritualist", DMZNamekConfig.INITIAL_KIPWR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_bio_spiritualist", DMZBioAndroidConfig.INITIAL_KIPWR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_cold_spiritualist", DMZColdDemonConfig.INITIAL_KIPWR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_pwr_majin_spiritualist", DMZMajinConfig.INITIAL_KIPWR_SPIRITUALIST.get()), player);

                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_human_spiritualist", DMZHumanConfig.INITIAL_ENE_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_saiyan_spiritualist", DMZSaiyanConfig.INITIAL_ENE_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_namek_spiritualist", DMZNamekConfig.INITIAL_ENE_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_bio_spiritualist", DMZBioAndroidConfig.INITIAL_ENE_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_cold_spiritualist", DMZColdDemonConfig.INITIAL_ENE_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ini_ene_majin_spiritualist", DMZMajinConfig.INITIAL_ENE_SPIRITUALIST.get()), player);

                    // Humano: Base, Buffed, FullPower, PotentialUnleashed
                    ModMessages.sendToPlayer(new PacketSyncConfig("base_human_stats", DMZTrHumanConfig.MULTIPLIER_BASE.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("buffed_human_str", DMZTrHumanConfig.MULTIPLIER_BUFFED_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("buffed_human_def", DMZTrHumanConfig.MULTIPLIER_BUFFED_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("buffed_human_pwr", DMZTrHumanConfig.MULTIPLIER_BUFFED_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("buffed_human_cost", DMZTrHumanConfig.BUFFED_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_human_str", DMZTrHumanConfig.MULTIPLIER_FP_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_human_def", DMZTrHumanConfig.MULTIPLIER_FP_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_human_pwr", DMZTrHumanConfig.MULTIPLIER_FP_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_human_cost", DMZTrHumanConfig.FP_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("potentialunleashed_human_str", DMZTrHumanConfig.MULTIPLIER_PU_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("potentialunleashed_human_def", DMZTrHumanConfig.MULTIPLIER_PU_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("potentialunleashed_human_pwr", DMZTrHumanConfig.MULTIPLIER_PU_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("potentialunleashed_human_cost", DMZTrHumanConfig.PU_FORM_KI_COST.get()), player);

                    // Saiyan: Base, Oozaru, SSJ, SSGrade 2, SSGrade 3, MSSJ, SSJ2, SSJ3, GoldenOozaru
                    ModMessages.sendToPlayer(new PacketSyncConfig("base_saiyan_stats", DMZTrSaiyanConfig.MULTIPLIER_BASE.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("oozaru_saiyan_str", DMZTrSaiyanConfig.MULTIPLIER_OOZARU_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("oozaru_saiyan_def", DMZTrSaiyanConfig.MULTIPLIER_OOZARU_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("oozaru_saiyan_pwr", DMZTrSaiyanConfig.MULTIPLIER_OOZARU_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("oozaru_saiyan_cost", DMZTrSaiyanConfig.OOZARU_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj_saiyan_str", DMZTrSaiyanConfig.MULTIPLIER_SSJ_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj_saiyan_def", DMZTrSaiyanConfig.MULTIPLIER_SSJ_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj_saiyan_pwr", DMZTrSaiyanConfig.MULTIPLIER_SSJ_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj_saiyan_cost", DMZTrSaiyanConfig.SSJ_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssgrade2_saiyan_str", DMZTrSaiyanConfig.MULTIPLIER_SSGRADE2_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssgrade2_saiyan_def", DMZTrSaiyanConfig.MULTIPLIER_SSGRADE2_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssgrade2_saiyan_pwr", DMZTrSaiyanConfig.MULTIPLIER_SSGRADE2_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssgrade2_saiyan_cost", DMZTrSaiyanConfig.SSGRADE2_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssgrade3_saiyan_str", DMZTrSaiyanConfig.MULTIPLIER_SSGRADE3_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssgrade3_saiyan_def", DMZTrSaiyanConfig.MULTIPLIER_SSGRADE3_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssgrade3_saiyan_pwr", DMZTrSaiyanConfig.MULTIPLIER_SSGRADE3_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssgrade3_saiyan_cost", DMZTrSaiyanConfig.SSGRADE3_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("mssj_saiyan_str", DMZTrSaiyanConfig.MULTIPLIER_MSSJ_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("mssj_saiyan_def", DMZTrSaiyanConfig.MULTIPLIER_MSSJ_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("mssj_saiyan_pwr", DMZTrSaiyanConfig.MULTIPLIER_MSSJ_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("mssj_saiyan_cost", DMZTrSaiyanConfig.MSSJ_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj2_saiyan_str", DMZTrSaiyanConfig.MULTIPLIER_SSJ2_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj2_saiyan_def", DMZTrSaiyanConfig.MULTIPLIER_SSJ2_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj2_saiyan_pwr", DMZTrSaiyanConfig.MULTIPLIER_SSJ2_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj2_saiyan_cost", DMZTrSaiyanConfig.SSJ2_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj3_saiyan_str", DMZTrSaiyanConfig.MULTIPLIER_SSJ3_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj3_saiyan_def", DMZTrSaiyanConfig.MULTIPLIER_SSJ3_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj3_saiyan_pwr", DMZTrSaiyanConfig.MULTIPLIER_SSJ3_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ssj3_saiyan_cost", DMZTrSaiyanConfig.SSJ3_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("goldenoozaru_saiyan_str", DMZTrSaiyanConfig.MULTIPLIER_GOLDENOOZARU_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("goldenoozaru_saiyan_def", DMZTrSaiyanConfig.MULTIPLIER_GOLDENOOZARU_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("goldenoozaru_saiyan_pwr", DMZTrSaiyanConfig.MULTIPLIER_GOLDENOOZARU_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("goldenoozaru_saiyan_cost", DMZTrSaiyanConfig.GOLDENOOZARU_FORM_KI_COST.get()), player);

                    // Namek: Base, Giant, FullPower, SuperNamek
                    ModMessages.sendToPlayer(new PacketSyncConfig("base_namek_stats", DMZTrNamekConfig.MULTIPLIER_BASE.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("giant_namek_str", DMZTrNamekConfig.MULTIPLIER_GIANT_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("giant_namek_def", DMZTrNamekConfig.MULTIPLIER_GIANT_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("giant_namek_pwr", DMZTrNamekConfig.MULTIPLIER_GIANT_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("giant_namek_cost", DMZTrNamekConfig.GIANT_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_namek_str", DMZTrNamekConfig.MULTIPLIER_FP_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_namek_def", DMZTrNamekConfig.MULTIPLIER_FP_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_namek_pwr", DMZTrNamekConfig.MULTIPLIER_FP_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_namek_cost", DMZTrNamekConfig.FP_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("supernamek_namek_str", DMZTrNamekConfig.MULTIPLIER_SUPER_NAMEK_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("supernamek_namek_def", DMZTrNamekConfig.MULTIPLIER_SUPER_NAMEK_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("supernamek_namek_pwr", DMZTrNamekConfig.MULTIPLIER_SUPER_NAMEK_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("supernamek_namek_cost", DMZTrNamekConfig.SUPER_NAMEK_FORM_KI_COST.get()), player);

                    // BioAndroid: Base, SemiPerfect, Perfect, Ultra
                    ModMessages.sendToPlayer(new PacketSyncConfig("base_bio_stats", DMZTrBioAndroidConfig.MULTIPLIER_BASE.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("semiperfect_bio_str", DMZTrBioAndroidConfig.MULTIPLIER_SEMIPERFECT_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("semiperfect_bio_def", DMZTrBioAndroidConfig.MULTIPLIER_SEMIPERFECT_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("semiperfect_bio_pwr", DMZTrBioAndroidConfig.MULTIPLIER_SEMIPERFECT_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("semiperfect_bio_cost", DMZTrBioAndroidConfig.SEMIPERFECT_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("perfect_bio_str", DMZTrBioAndroidConfig.MULTIPLIER_PERFECT_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("perfect_bio_def", DMZTrBioAndroidConfig.MULTIPLIER_PERFECT_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("perfect_bio_pwr", DMZTrBioAndroidConfig.MULTIPLIER_PERFECT_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("perfect_bio_cost", DMZTrBioAndroidConfig.PERFECT_FORM_KI_COST.get()), player);

                    // ColdDemon: Base, Second, Third, Final, FullPower
                    ModMessages.sendToPlayer(new PacketSyncConfig("base_cold_stats", DMZTrColdDemonConfig.MULTIPLIER_BASE.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("second_cold_str", DMZTrColdDemonConfig.MULTIPLIER_SECOND_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("second_cold_def", DMZTrColdDemonConfig.MULTIPLIER_SECOND_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("second_cold_pwr", DMZTrColdDemonConfig.MULTIPLIER_SECOND_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("second_cold_cost", DMZTrColdDemonConfig.SECOND_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("third_cold_str", DMZTrColdDemonConfig.MULTIPLIER_THIRD_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("third_cold_def", DMZTrColdDemonConfig.MULTIPLIER_THIRD_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("third_cold_pwr", DMZTrColdDemonConfig.MULTIPLIER_THIRD_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("third_cold_cost", DMZTrColdDemonConfig.THIRD_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("final_cold_str", DMZTrColdDemonConfig.MULTIPLIER_FOURTH_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("final_cold_def", DMZTrColdDemonConfig.MULTIPLIER_FOURTH_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("final_cold_pwr", DMZTrColdDemonConfig.MULTIPLIER_FOURTH_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("final_cold_cost", DMZTrColdDemonConfig.FOURTH_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_cold_str", DMZTrColdDemonConfig.MULTIPLIER_FULL_POWER_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_cold_def", DMZTrColdDemonConfig.MULTIPLIER_FULL_POWER_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_cold_pwr", DMZTrColdDemonConfig.MULTIPLIER_FULL_POWER_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("fullpower_cold_cost", DMZTrColdDemonConfig.FULL_POWER_FORM_KI_COST.get()), player);

                    // Majin: Base, Evil, Kid, Super, Ultra
                    ModMessages.sendToPlayer(new PacketSyncConfig("base_majin_stats", DMZTrMajinConfig.MULTIPLIER_BASE.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("evil_majin_str", DMZTrMajinConfig.MULTIPLIER_EVIL_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("evil_majin_def", DMZTrMajinConfig.MULTIPLIER_EVIL_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("evil_majin_pwr", DMZTrMajinConfig.MULTIPLIER_EVIL_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("evil_majin_cost", DMZTrMajinConfig.EVIL_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("kid_majin_str", DMZTrMajinConfig.MULTIPLIER_KID_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("kid_majin_def", DMZTrMajinConfig.MULTIPLIER_KID_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("kid_majin_pwr", DMZTrMajinConfig.MULTIPLIER_KID_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("kid_majin_cost", DMZTrMajinConfig.KID_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("super_majin_str", DMZTrMajinConfig.MULTIPLIER_SUPER_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("super_majin_def", DMZTrMajinConfig.MULTIPLIER_SUPER_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("super_majin_pwr", DMZTrMajinConfig.MULTIPLIER_SUPER_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("super_majin_cost", DMZTrMajinConfig.SUPER_FORM_KI_COST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ultra_majin_str", DMZTrMajinConfig.MULTIPLIER_ULTRA_FORM_STR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ultra_majin_def", DMZTrMajinConfig.MULTIPLIER_ULTRA_FORM_DEF.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ultra_majin_pwr", DMZTrMajinConfig.MULTIPLIER_ULTRA_FORM_PWR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("ultra_majin_cost", DMZTrMajinConfig.ULTRA_FORM_KI_COST.get()), player);

                    // Multis: STR, DEF, CON, PWR, ENE, REGEN, Human, Saiyan, Namek, BioAndroid, ColdDemon, Majin
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_str_warrior", DMZHumanConfig.MULTIPLIER_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_def_warrior", DMZHumanConfig.MULTIPLIER_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_con_warrior", DMZHumanConfig.MULTIPLIER_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_pwr_warrior", DMZHumanConfig.MULTIPLIER_KIPOWER_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_ene_warrior", DMZHumanConfig.MULTIPLIER_ENERGY_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_regen_warrior", DMZHumanConfig.KI_REGEN_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_str_spiritualist", DMZHumanConfig.MULTIPLIER_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_def_spiritualist", DMZHumanConfig.MULTIPLIER_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_con_spiritualist", DMZHumanConfig.MULTIPLIER_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_pwr_spiritualist", DMZHumanConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_ene_spiritualist", DMZHumanConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_mult_regen_spiritualist", DMZHumanConfig.KI_REGEN_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_str_warrior", DMZSaiyanConfig.MULTIPLIER_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_def_warrior", DMZSaiyanConfig.MULTIPLIER_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_con_warrior", DMZSaiyanConfig.MULTIPLIER_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_pwr_warrior", DMZSaiyanConfig.MULTIPLIER_KIPOWER_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_ene_warrior", DMZSaiyanConfig.MULTIPLIER_ENERGY_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_regen_warrior", DMZSaiyanConfig.KI_REGEN_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_str_spiritualist", DMZSaiyanConfig.MULTIPLIER_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_def_spiritualist", DMZSaiyanConfig.MULTIPLIER_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_con_spiritualist", DMZSaiyanConfig.MULTIPLIER_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_pwr_spiritualist", DMZSaiyanConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_ene_spiritualist", DMZSaiyanConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("saiyan_mult_regen_spiritualist", DMZSaiyanConfig.KI_REGEN_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_str_warrior", DMZNamekConfig.MULTIPLIER_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_def_warrior", DMZNamekConfig.MULTIPLIER_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_con_warrior", DMZNamekConfig.MULTIPLIER_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_pwr_warrior", DMZNamekConfig.MULTIPLIER_KIPOWER_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_ene_warrior", DMZNamekConfig.MULTIPLIER_ENERGY_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_regen_warrior", DMZNamekConfig.KI_REGEN_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_str_spiritualist", DMZNamekConfig.MULTIPLIER_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_def_spiritualist", DMZNamekConfig.MULTIPLIER_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_con_spiritualist", DMZNamekConfig.MULTIPLIER_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_pwr_spiritualist", DMZNamekConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_ene_spiritualist", DMZNamekConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_mult_regen_spiritualist", DMZNamekConfig.KI_REGEN_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_str_warrior", DMZBioAndroidConfig.MULTIPLIER_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_def_warrior", DMZBioAndroidConfig.MULTIPLIER_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_con_warrior", DMZBioAndroidConfig.MULTIPLIER_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_pwr_warrior", DMZBioAndroidConfig.MULTIPLIER_KIPOWER_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_ene_warrior", DMZBioAndroidConfig.MULTIPLIER_ENERGY_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_regen_warrior", DMZBioAndroidConfig.KI_REGEN_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_str_spiritualist", DMZBioAndroidConfig.MULTIPLIER_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_def_spiritualist", DMZBioAndroidConfig.MULTIPLIER_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_con_spiritualist", DMZBioAndroidConfig.MULTIPLIER_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_pwr_spiritualist", DMZBioAndroidConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_ene_spiritualist", DMZBioAndroidConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_mult_regen_spiritualist", DMZBioAndroidConfig.KI_REGEN_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_str_warrior", DMZColdDemonConfig.MULTIPLIER_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_def_warrior", DMZColdDemonConfig.MULTIPLIER_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_con_warrior", DMZColdDemonConfig.MULTIPLIER_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_pwr_warrior", DMZColdDemonConfig.MULTIPLIER_KIPOWER_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_ene_warrior", DMZColdDemonConfig.MULTIPLIER_ENERGY_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_regen_warrior", DMZColdDemonConfig.KI_REGEN_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_str_spiritualist", DMZColdDemonConfig.MULTIPLIER_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_def_spiritualist", DMZColdDemonConfig.MULTIPLIER_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_con_spiritualist", DMZColdDemonConfig.MULTIPLIER_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_pwr_spiritualist", DMZColdDemonConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_ene_spiritualist", DMZColdDemonConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_mult_regen_spiritualist", DMZColdDemonConfig.KI_REGEN_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_str_warrior", DMZMajinConfig.MULTIPLIER_STR_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_def_warrior", DMZMajinConfig.MULTIPLIER_DEF_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_con_warrior", DMZMajinConfig.MULTIPLIER_CON_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_pwr_warrior", DMZMajinConfig.MULTIPLIER_KIPOWER_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_ene_warrior", DMZMajinConfig.MULTIPLIER_ENERGY_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_regen_warrior", DMZMajinConfig.KI_REGEN_WARRIOR.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_str_spiritualist", DMZMajinConfig.MULTIPLIER_STR_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_def_spiritualist", DMZMajinConfig.MULTIPLIER_DEF_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_con_spiritualist", DMZMajinConfig.MULTIPLIER_CON_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_pwr_spiritualist", DMZMajinConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_ene_spiritualist", DMZMajinConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_mult_regen_spiritualist", DMZMajinConfig.KI_REGEN_SPIRITUALIST.get()), player);

                    // Pasivas
                    ModMessages.sendToPlayer(new PacketSyncConfig("human_passive", DMZHumanConfig.KICHARGE_REGEN_BOOST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("zenkai_timer", DMZSaiyanConfig.ZENKAI_COOLDOWN.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("zenkai_heal", DMZSaiyanConfig.ZENKAI_HEALTH_REGEN.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("zenkai_boost", DMZSaiyanConfig.ZENKAI_STAT_BOOST.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("zenkai_cant", DMZSaiyanConfig.ZENKAI_CANT.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("namek_passive", DMZNamekConfig.PASSIVE_REGEN.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_passive_half", DMZBioAndroidConfig.HALF_HEALTH_LIFESTEAL.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("bio_passive_quarter", DMZBioAndroidConfig.QUARTER_HEALTH_LIFESTEAL.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("cold_passive", DMZColdDemonConfig.TP_MULTIPLER_PASSIVE.get()), player);
                    ModMessages.sendToPlayer(new PacketSyncConfig("majin_passive", DMZMajinConfig.PASSIVE_HEALTH_REGEN.get()), player);
                });
            }

        });
        context.setPacketHandled(true);
    }
}