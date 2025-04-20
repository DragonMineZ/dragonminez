package com.yuseix.dragonminez.common.network.S2C;

import com.yuseix.dragonminez.client.config.DMZClientConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncConfig {
    private final String config;
    private final double stat;

    public PacketSyncConfig(String config, double stat) {
        this.config = config;
        this.stat = stat;
    }

    public PacketSyncConfig(FriendlyByteBuf buf) {
        this.config = buf.readUtf();
        this.stat = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(config);
        buf.writeDouble(stat);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Guardar los valores en el cliente
            switch (config){
                case "maxattributes":
                    DMZClientConfig.setMaxStats((int) stat);
                    break;
                case "zpoints_cost":
                    DMZClientConfig.setMultiplierZPoints(stat);
                    break;
                case "majin_multiplier":
                    DMZClientConfig.setMajin_multi(stat);
                    break;
                case "tree_might_multiplier":
                    DMZClientConfig.setTree_might_multi(stat);
                    break;
                case "transfTPCost":
                    DMZClientConfig.setTransfTPCost((int) stat);
                    break;
                case "buyableTP":
                    DMZClientConfig.setBuyableTP((int) stat);
                    break;
                case "babaCooldown":
                    DMZClientConfig.setBabaCooldown((int) stat);
                    break;
                case "babaDuration":
                    DMZClientConfig.setBabaDuration((int) stat);
                    break;
                case "jumpLevels":
                    DMZClientConfig.setJumpLevels((int) stat);
                    break;
                case "flyLevels":
                    DMZClientConfig.setFlyLevels((int) stat);
                    break;
                case "meditationLevels":
                    DMZClientConfig.setMeditationLevels((int) stat);
                    break;
                case "potUnlockLevels":
                    DMZClientConfig.setPotUnlockLevels((int) stat);
                    break;
                case "kiManipLevels":
                    DMZClientConfig.setKiManipLevels((int) stat);
                    break;
                case "kiControlLevels":
                    DMZClientConfig.setKiControlLevels((int) stat);
                    break;
                case "jumpMaster":
                    DMZClientConfig.setJumpMaster((int) stat);
                    break;
                case "flyMaster":
                    DMZClientConfig.setFlyMaster((int) stat);
                    break;
                case "meditationMaster":
                    DMZClientConfig.setMeditationMaster((int) stat);
                    break;
                case "potUnlockMaster":
                    DMZClientConfig.setPotUnlockMaster((int) stat);
                    break;
                case "kiManipMaster":
                    DMZClientConfig.setKiManipMaster((int) stat);
                    break;
                case "kiControlMaster":
                    DMZClientConfig.setKiControlMaster((int) stat);
                    break;

                case "ini_str_human_warrior":
                    DMZClientConfig.setInit_StrStat("human", "warrior",(int) stat);
                    break;
                case "ini_str_saiyan_warrior":
                    DMZClientConfig.setInit_StrStat("saiyan", "warrior",(int) stat);
                    break;
                case "ini_str_namek_warrior":
                    DMZClientConfig.setInit_StrStat("namek", "warrior",(int) stat);
                    break;
                case "ini_str_bio_warrior":
                    DMZClientConfig.setInit_StrStat("bio", "warrior",(int) stat);
                    break;
                case "ini_str_cold_warrior":
                    DMZClientConfig.setInit_StrStat("cold", "warrior",(int) stat);
                    break;
                case "ini_str_majin_warrior":
                    DMZClientConfig.setInit_StrStat("majin", "warrior",(int) stat);
                    break;

                case "ini_def_human_warrior":
                    DMZClientConfig.setInit_DefStat("human", "warrior",(int) stat);
                    break;
                case "ini_def_saiyan_warrior":
                    DMZClientConfig.setInit_DefStat("saiyan", "warrior",(int) stat);
                    break;
                case "ini_def_namek_warrior":
                    DMZClientConfig.setInit_DefStat("namek", "warrior",(int) stat);
                    break;
                case "ini_def_bio_warrior":
                    DMZClientConfig.setInit_DefStat("bio", "warrior",(int) stat);
                    break;
                case "ini_def_cold_warrior":
                    DMZClientConfig.setInit_DefStat("cold", "warrior",(int) stat);
                    break;
                case "ini_def_majin_warrior":
                    DMZClientConfig.setInit_DefStat("majin", "warrior",(int) stat);
                    break;

                case "ini_con_human_warrior":
                    DMZClientConfig.setInit_ConStat("human", "warrior",(int) stat);
                    break;
                case "ini_con_saiyan_warrior":
                    DMZClientConfig.setInit_ConStat("saiyan", "warrior",(int) stat);
                    break;
                case "ini_con_namek_warrior":
                    DMZClientConfig.setInit_ConStat("namek", "warrior",(int) stat);
                    break;
                case "ini_con_bio_warrior":
                    DMZClientConfig.setInit_ConStat("bio", "warrior",(int) stat);
                    break;
                case "ini_con_cold_warrior":
                    DMZClientConfig.setInit_ConStat("cold", "warrior",(int) stat);
                    break;
                case "ini_con_majin_warrior":
                    DMZClientConfig.setInit_ConStat("majin", "warrior",(int) stat);
                    break;

                case "ini_pwr_human_warrior":
                    DMZClientConfig.setInit_PWRStat("human","spiritualist",(int) stat);
                    break;
                case "ini_pwr_saiyan_warrior":
                    DMZClientConfig.setInit_PWRStat("saiyan","warrior",(int) stat);
                    break;
                case "ini_pwr_namek_warrior":
                    DMZClientConfig.setInit_PWRStat("namek","warrior",(int) stat);
                    break;
                case "ini_pwr_bio_warrior":
                    DMZClientConfig.setInit_PWRStat("bio","warrior",(int) stat);
                    break;
                case "ini_pwr_cold_warrior":
                    DMZClientConfig.setInit_PWRStat("cold","warrior",(int) stat);
                    break;
                case "ini_pwr_majin_warrior":
                    DMZClientConfig.setInit_PWRStat("majin","warrior",(int) stat);
                    break;

                case "ini_ene_human_warrior":
                    DMZClientConfig.setInit_ENEStat("human","warrior",(int) stat);
                    break;
                case "ini_ene_saiyan_warrior":
                    DMZClientConfig.setInit_ENEStat("saiyan","warrior",(int) stat);
                    break;
                case "ini_ene_namek_warrior":
                    DMZClientConfig.setInit_ENEStat("namek","warrior",(int) stat);
                    break;
                case "ini_ene_bio_warrior":
                    DMZClientConfig.setInit_ENEStat("bio","warrior",(int) stat);
                    break;
                case "ini_ene_cold_warrior":
                    DMZClientConfig.setInit_ENEStat("cold","warrior",(int) stat);
                    break;
                case "ini_ene_majin_warrior":
                    DMZClientConfig.setInit_ENEStat("majin","warrior",(int) stat);
                    break;
                //SPIRITUALIST
                case "ini_str_human_spiritualist":
                    DMZClientConfig.setInit_StrStat("human", "spiritualist",(int) stat);
                    break;
                case "ini_str_saiyan_spiritualist":
                    DMZClientConfig.setInit_StrStat("saiyan", "spiritualist",(int) stat);
                    break;
                case "ini_str_namek_spiritualist":
                    DMZClientConfig.setInit_StrStat("namek", "spiritualist",(int) stat);
                    break;
                case "ini_str_bio_spiritualist":
                    DMZClientConfig.setInit_StrStat("bio", "spiritualist",(int) stat);
                    break;
                case "ini_str_cold_spiritualist":
                    DMZClientConfig.setInit_StrStat("cold", "spiritualist",(int) stat);
                    break;
                case "ini_str_majin_spiritualist":
                    DMZClientConfig.setInit_StrStat("majin", "spiritualist",(int) stat);
                    break;

                case "ini_def_human_spiritualist":
                    DMZClientConfig.setInit_DefStat("human", "spiritualist",(int) stat);
                    break;
                case "ini_def_saiyan_spiritualist":
                    DMZClientConfig.setInit_DefStat("saiyan", "spiritualist",(int) stat);
                    break;
                case "ini_def_namek_spiritualist":
                    DMZClientConfig.setInit_DefStat("namek", "spiritualist",(int) stat);
                    break;
                case "ini_def_bio_spiritualist":
                    DMZClientConfig.setInit_DefStat("bio", "spiritualist",(int) stat);
                    break;
                case "ini_def_cold_spiritualist":
                    DMZClientConfig.setInit_DefStat("cold", "spiritualist",(int) stat);
                    break;
                case "ini_def_majin_spiritualist":
                    DMZClientConfig.setInit_DefStat("majin", "spiritualist",(int) stat);
                    break;

                case "ini_con_human_spiritualist":
                    DMZClientConfig.setInit_ConStat("human", "spiritualist",(int) stat);
                    break;
                case "ini_con_saiyan_spiritualist":
                    DMZClientConfig.setInit_ConStat("saiyan", "spiritualist",(int) stat);
                    break;
                case "ini_con_namek_spiritualist":
                    DMZClientConfig.setInit_ConStat("namek", "spiritualist",(int) stat);
                    break;
                case "ini_con_bio_spiritualist":
                    DMZClientConfig.setInit_ConStat("bio", "spiritualist",(int) stat);
                    break;
                case "ini_con_cold_spiritualist":
                    DMZClientConfig.setInit_ConStat("cold", "spiritualist",(int) stat);
                    break;
                case "ini_con_majin_spiritualist":
                    DMZClientConfig.setInit_ConStat("majin", "spiritualist",(int) stat);
                    break;

                case "ini_pwr_human_spiritualist":
                    DMZClientConfig.setInit_PWRStat("human","spiritualist",(int) stat);
                    break;
                case "ini_pwr_saiyan_spiritualist":
                    DMZClientConfig.setInit_PWRStat("saiyan","spiritualist",(int) stat);
                    break;
                case "ini_pwr_namek_spiritualist":
                    DMZClientConfig.setInit_PWRStat("namek","spiritualist",(int) stat);
                    break;
                case "ini_pwr_bio_spiritualist":
                    DMZClientConfig.setInit_PWRStat("bio","spiritualist",(int) stat);
                    break;
                case "ini_pwr_cold_spiritualist":
                    DMZClientConfig.setInit_PWRStat("cold","spiritualist",(int) stat);
                    break;
                case "ini_pwr_majin_spiritualist":
                    DMZClientConfig.setInit_PWRStat("majin","spiritualist",(int) stat);
                    break;

                case "ini_ene_human_spiritualist":
                    DMZClientConfig.setInit_ENEStat("human","spiritualist",(int) stat);
                    break;
                case "ini_ene_saiyan_spiritualist":
                    DMZClientConfig.setInit_ENEStat("saiyan","spiritualist",(int) stat);
                    break;
                case "ini_ene_namek_spiritualist":
                    DMZClientConfig.setInit_ENEStat("namek","spiritualist",(int) stat);
                    break;
                case "ini_ene_bio_spiritualist":
                    DMZClientConfig.setInit_ENEStat("bio","spiritualist",(int) stat);
                    break;
                case "ini_ene_cold_spiritualist":
                    DMZClientConfig.setInit_ENEStat("cold","spiritualist",(int) stat);
                    break;
                case "ini_ene_majin_spiritualist":
                    DMZClientConfig.setInit_ENEStat("majin","spiritualist",(int) stat);
                    break;
                case "base_human_stats":
                    DMZClientConfig.setBaseStats(0, stat);
                    break;
                case "buffed_human_str":
                    DMZClientConfig.setSTRStat(0, "buffed", stat);
                    break;
                case "buffed_human_def":
                    DMZClientConfig.setDEFStat(0, "buffed", stat);
                    break;
                case "buffed_human_pwr":
                    DMZClientConfig.setPWRStat(0, "buffed", stat);
                    break;
                case "buffed_human_cost":
                    DMZClientConfig.setCostForm(0, "buffed", stat);
                    break;
                case "fullpower_human_str":
                    DMZClientConfig.setSTRStat(0, "full_power", stat);
                    break;
                case "fullpower_human_def":
                    DMZClientConfig.setDEFStat(0, "full_power", stat);
                    break;
                case "fullpower_human_pwr":
                    DMZClientConfig.setPWRStat(0, "full_power", stat);
                    break;
                case "fullpower_human_cost":
                    DMZClientConfig.setCostForm(0, "full_power", stat);
                    break;
                case "potentialunleashed_human_str":
                    DMZClientConfig.setSTRStat(0, "potential_unleashed", stat);
                    break;
                case "potentialunleashed_human_def":
                    DMZClientConfig.setDEFStat(0, "potential_unleashed", stat);
                    break;
                case "potentialunleashed_human_pwr":
                    DMZClientConfig.setPWRStat(0, "potential_unleashed", stat);
                    break;
                case "potentialunleashed_human_cost":
                    DMZClientConfig.setCostForm(0, "potential_unleashed", stat);
                    break;
                case "base_saiyan_stats":
                    DMZClientConfig.setBaseStats(1, stat);
                    break;
                case "oozaru_saiyan_str":
                    DMZClientConfig.setSTRStat(1, "oozaru", stat);
                    break;
                case "oozaru_saiyan_def":
                    DMZClientConfig.setDEFStat(1, "oozaru", stat);
                    break;
                case "oozaru_saiyan_pwr":
                    DMZClientConfig.setPWRStat(1, "oozaru", stat);
                    break;
                case "oozaru_saiyan_cost":
                    DMZClientConfig.setCostForm(1, "oozaru", stat);
                    break;
                case "ssj_saiyan_str":
                    DMZClientConfig.setSTRStat(1, "ssj1", stat);
                    break;
                case "ssj_saiyan_def":
                    DMZClientConfig.setDEFStat(1, "ssj1", stat);
                    break;
                case "ssj_saiyan_pwr":
                    DMZClientConfig.setPWRStat(1, "ssj1", stat);
                    break;
                case "ssj_saiyan_cost":
                    DMZClientConfig.setCostForm(1, "ssj1", stat);
                    break;
                case "ssgrade2_saiyan_str":
                    DMZClientConfig.setSTRStat(1, "ssgrade2", stat);
                    break;
                case "ssgrade2_saiyan_def":
                    DMZClientConfig.setDEFStat(1, "ssgrade2", stat);
                    break;
                case "ssgrade2_saiyan_pwr":
                    DMZClientConfig.setPWRStat(1, "ssgrade2", stat);
                    break;
                case "ssgrade2_saiyan_cost":
                    DMZClientConfig.setCostForm(1, "ssgrade2", stat);
                    break;
                case "ssgrade3_saiyan_str":
                    DMZClientConfig.setSTRStat(1, "ssgrade3", stat);
                    break;
                case "ssgrade3_saiyan_def":
                    DMZClientConfig.setDEFStat(1, "ssgrade3", stat);
                    break;
                case "ssgrade3_saiyan_pwr":
                    DMZClientConfig.setPWRStat(1, "ssgrade3", stat);
                    break;
                case "ssgrade3_saiyan_cost":
                    DMZClientConfig.setCostForm(1, "ssgrade3", stat);
                    break;
                case "mssj_saiyan_str":
                    DMZClientConfig.setSTRStat(1, "ssjfp", stat);
                    break;
                case "mssj_saiyan_def":
                    DMZClientConfig.setDEFStat(1, "ssjfp", stat);
                    break;
                case "mssj_saiyan_pwr":
                    DMZClientConfig.setPWRStat(1, "ssjfp", stat);
                    break;
                case "mssj_saiyan_cost":
                    DMZClientConfig.setCostForm(1, "ssjfp", stat);
                    break;
                case "ssj2_saiyan_str":
                    DMZClientConfig.setSTRStat(1, "ssj2", stat);
                    break;
                case "ssj2_saiyan_def":
                    DMZClientConfig.setDEFStat(1, "ssj2", stat);
                    break;
                case "ssj2_saiyan_pwr":
                    DMZClientConfig.setPWRStat(1, "ssj2", stat);
                    break;
                case "ssj2_saiyan_cost":
                    DMZClientConfig.setCostForm(1, "ssj2", stat);
                    break;
                case "ssj3_saiyan_str":
                    DMZClientConfig.setSTRStat(1, "ssj3", stat);
                    break;
                case "ssj3_saiyan_def":
                    DMZClientConfig.setDEFStat(1, "ssj3", stat);
                    break;
                case "ssj3_saiyan_pwr":
                    DMZClientConfig.setPWRStat(1, "ssj3", stat);
                    break;
                case "ssj3_saiyan_cost":
                    DMZClientConfig.setCostForm(1, "ssj3", stat);
                    break;
                case "goldenoozaru_saiyan_str":
                    DMZClientConfig.setSTRStat(1, "golden_oozaru", stat);
                    break;
                case "goldenoozaru_saiyan_def":
                    DMZClientConfig.setDEFStat(1, "golden_oozaru", stat);
                    break;
                case "goldenoozaru_saiyan_pwr":
                    DMZClientConfig.setPWRStat(1, "golden_oozaru", stat);
                    break;
                case "goldenoozaru_saiyan_cost":
                    DMZClientConfig.setCostForm(1, "golden_oozaru", stat);
                    break;
                case "base_namek_stats":
                    DMZClientConfig.setBaseStats(2, stat);
                    break;
                case "giant_namek_str":
                    DMZClientConfig.setSTRStat(2, "giant", stat);
                    break;
                case "giant_namek_def":
                    DMZClientConfig.setDEFStat(2, "giant", stat);
                    break;
                case "giant_namek_pwr":
                    DMZClientConfig.setPWRStat(2, "giant", stat);
                    break;
                case "giant_namek_cost":
                    DMZClientConfig.setCostForm(2, "giant", stat);
                    break;
                case "fullpower_namek_str":
                    DMZClientConfig.setSTRStat(2, "full_power", stat);
                    break;
                case "fullpower_namek_def":
                    DMZClientConfig.setDEFStat(2, "full_power", stat);
                    break;
                case "fullpower_namek_pwr":
                    DMZClientConfig.setPWRStat(2, "full_power", stat);
                    break;
                case "fullpower_namek_cost":
                    DMZClientConfig.setCostForm(2, "full_power", stat);
                    break;
                case "supernamek_namek_str":
                    DMZClientConfig.setSTRStat(2, "super_namek", stat);
                    break;
                case "supernamek_namek_def":
                    DMZClientConfig.setDEFStat(2, "super_namek", stat);
                    break;
                case "supernamek_namek_pwr":
                    DMZClientConfig.setPWRStat(2, "super_namek", stat);
                    break;
                case "supernamek_namek_cost":
                    DMZClientConfig.setCostForm(2, "super_namek", stat);
                    break;
                case "base_bio_stats":
                    DMZClientConfig.setBaseStats(3, stat);
                    break;
                case "semiperfect_bio_str":
                    DMZClientConfig.setSTRStat(3, "semi_perfect", stat);
                    break;
                case "semiperfect_bio_def":
                    DMZClientConfig.setDEFStat(3, "semi_perfect", stat);
                    break;
                case "semiperfect_bio_pwr":
                    DMZClientConfig.setPWRStat(3, "semi_perfect", stat);
                    break;
                case "semiperfect_bio_cost":
                    DMZClientConfig.setCostForm(3, "semi_perfect", stat);
                    break;
                case "perfect_bio_str":
                    DMZClientConfig.setSTRStat(3, "perfect", stat);
                    break;
                case "perfect_bio_def":
                    DMZClientConfig.setDEFStat(3, "perfect", stat);
                    break;
                case "perfect_bio_pwr":
                    DMZClientConfig.setPWRStat(3, "perfect", stat);
                    break;
                case "perfect_bio_cost":
                    DMZClientConfig.setCostForm(3, "perfect", stat);
                    break;
                case "base_cold_stats":
                    DMZClientConfig.setBaseStats(4, stat);
                    break;
                case "second_cold_str":
                    DMZClientConfig.setSTRStat(4, "second_form", stat);
                    break;
                case "second_cold_def":
                    DMZClientConfig.setDEFStat(4, "second_form", stat);
                    break;
                case "second_cold_pwr":
                    DMZClientConfig.setPWRStat(4, "second_form", stat);
                    break;
                case "second_cold_cost":
                    DMZClientConfig.setCostForm(4, "second_form", stat);
                    break;
                case "third_cold_str":
                    DMZClientConfig.setSTRStat(4, "third_form", stat);
                    break;
                case "third_cold_def":
                    DMZClientConfig.setDEFStat(4, "third_form", stat);
                    break;
                case "third_cold_pwr":
                    DMZClientConfig.setPWRStat(4, "third_form", stat);
                    break;
                case "third_cold_cost":
                    DMZClientConfig.setCostForm(4, "third_form", stat);
                    break;
                case "final_cold_str":
                    DMZClientConfig.setSTRStat(4, "final_form", stat);
                    break;
                case "final_cold_def":
                    DMZClientConfig.setDEFStat(4, "final_form", stat);
                    break;
                case "final_cold_pwr":
                    DMZClientConfig.setPWRStat(4, "final_form", stat);
                    break;
                case "final_cold_cost":
                    DMZClientConfig.setCostForm(4, "final_form", stat);
                    break;
                case "fullpower_cold_str":
                    DMZClientConfig.setSTRStat(4, "full_power", stat);
                    break;
                case "fullpower_cold_def":
                    DMZClientConfig.setDEFStat(4, "full_power", stat);
                    break;
                case "fullpower_cold_pwr":
                    DMZClientConfig.setPWRStat(4, "full_power", stat);
                    break;
                case "fullpower_cold_cost":
                    DMZClientConfig.setCostForm(4, "full_power", stat);
                    break;
                case "base_majin_stats":
                    DMZClientConfig.setBaseStats(5, stat);
                    break;
                case "evil_majin_str":
                    DMZClientConfig.setSTRStat(5, "evil", stat);
                    break;
                case "evil_majin_def":
                    DMZClientConfig.setDEFStat(5, "evil", stat);
                    break;
                case "evil_majin_pwr":
                    DMZClientConfig.setPWRStat(5, "evil", stat);
                    break;
                case "evil_majin_cost":
                    DMZClientConfig.setCostForm(5, "evil", stat);
                    break;
                case "kid_majin_str":
                    DMZClientConfig.setSTRStat(5, "kid", stat);
                    break;
                case "kid_majin_def":
                    DMZClientConfig.setDEFStat(5, "kid", stat);
                    break;
                case "kid_majin_pwr":
                    DMZClientConfig.setPWRStat(5, "kid", stat);
                    break;
                case "kid_majin_cost":
                    DMZClientConfig.setCostForm(5, "kid", stat);
                    break;
                case "super_majin_str":
                    DMZClientConfig.setSTRStat(5, "super", stat);
                    break;
                case "super_majin_def":
                    DMZClientConfig.setDEFStat(5, "super", stat);
                    break;
                case "super_majin_pwr":
                    DMZClientConfig.setPWRStat(5, "super", stat);
                    break;
                case "super_majin_cost":
                    DMZClientConfig.setCostForm(5, "super", stat);
                    break;
                case "ultra_majin_str":
                    DMZClientConfig.setSTRStat(5, "ultra", stat);
                    break;
                case "ultra_majin_def":
                    DMZClientConfig.setDEFStat(5, "ultra", stat);
                    break;
                case "ultra_majin_pwr":
                    DMZClientConfig.setPWRStat(5, "ultra", stat);
                    break;
                case "ultra_majin_cost":
                    DMZClientConfig.setCostForm(5, "ultra", stat);
                    break;
                case "human_mult_str_warrior":
                    DMZClientConfig.setClassMult(0, "warrior", "str", stat);
                    break;
                case "human_mult_def_warrior":
                    DMZClientConfig.setClassMult(0, "warrior", "def", stat);
                    break;
                case "human_mult_pwr_warrior":
                    DMZClientConfig.setClassMult(0, "warrior", "pwr", stat);
                    break;
                case "human_mult_regen_warrior":
                    DMZClientConfig.setClassMult(0, "warrior", "regen", stat);
                    break;
                case "human_mult_str_spiritualist":
                    DMZClientConfig.setClassMult(0, "spiritualist", "str", stat);
                    break;
                case "human_mult_def_spiritualist":
                    DMZClientConfig.setClassMult(0, "spiritualist", "def", stat);
                    break;
                case "human_mult_pwr_spiritualist":
                    DMZClientConfig.setClassMult(0, "spiritualist", "pwr", stat);
                    break;
                case "human_mult_regen_spiritualist":
                    DMZClientConfig.setClassMult(0, "spiritualist", "regen", stat);
                    break;
                case "saiyan_mult_str_warrior":
                    DMZClientConfig.setClassMult(1, "warrior", "str", stat);
                    break;
                case "saiyan_mult_def_warrior":
                    DMZClientConfig.setClassMult(1, "warrior", "def", stat);
                    break;
                case "saiyan_mult_pwr_warrior":
                    DMZClientConfig.setClassMult(1, "warrior", "pwr", stat);
                    break;
                case "saiyan_mult_regen_warrior":
                    DMZClientConfig.setClassMult(1, "warrior", "regen", stat);
                    break;
                case "saiyan_mult_str_spiritualist":
                    DMZClientConfig.setClassMult(1, "spiritualist", "str", stat);
                    break;
                case "saiyan_mult_def_spiritualist":
                    DMZClientConfig.setClassMult(1, "spiritualist", "def", stat);
                    break;
                case "saiyan_mult_pwr_spiritualist":
                    DMZClientConfig.setClassMult(1, "spiritualist", "pwr", stat);
                    break;
                case "saiyan_mult_regen_spiritualist":
                    DMZClientConfig.setClassMult(1, "spiritualist", "regen", stat);
                    break;
                case "namek_mult_str_warrior":
                    DMZClientConfig.setClassMult(2, "warrior", "str", stat);
                    break;
                case "namek_mult_def_warrior":
                    DMZClientConfig.setClassMult(2, "warrior", "def", stat);
                    break;
                case "namek_mult_pwr_warrior":
                    DMZClientConfig.setClassMult(2, "warrior", "pwr", stat);
                    break;
                case "namek_mult_regen_warrior":
                    DMZClientConfig.setClassMult(2, "warrior", "regen", stat);
                    break;
                case "namek_mult_str_spiritualist":
                    DMZClientConfig.setClassMult(2, "spiritualist", "str", stat);
                    break;
                case "namek_mult_def_spiritualist":
                    DMZClientConfig.setClassMult(2, "spiritualist", "def", stat);
                    break;
                case "namek_mult_pwr_spiritualist":
                    DMZClientConfig.setClassMult(2, "spiritualist", "pwr", stat);
                    break;
                case "namek_mult_regen_spiritualist":
                    DMZClientConfig.setClassMult(2, "spiritualist", "regen", stat);
                    break;
                case "bio_mult_str_warrior":
                    DMZClientConfig.setClassMult(3, "warrior", "str", stat);
                    break;
                case "bio_mult_def_warrior":
                    DMZClientConfig.setClassMult(3, "warrior", "def", stat);
                    break;
                case "bio_mult_pwr_warrior":
                    DMZClientConfig.setClassMult(3, "warrior", "pwr", stat);
                    break;
                case "bio_mult_regen_warrior":
                    DMZClientConfig.setClassMult(3, "warrior", "regen", stat);
                    break;
                case "bio_mult_str_spiritualist":
                    DMZClientConfig.setClassMult(3, "spiritualist", "str", stat);
                    break;
                case "bio_mult_def_spiritualist":
                    DMZClientConfig.setClassMult(3, "spiritualist", "def", stat);
                    break;
                case "bio_mult_pwr_spiritualist":
                    DMZClientConfig.setClassMult(3, "spiritualist", "pwr", stat);
                    break;
                case "bio_mult_regen_spiritualist":
                    DMZClientConfig.setClassMult(3, "spiritualist", "regen", stat);
                    break;
                case "cold_mult_str_warrior":
                    DMZClientConfig.setClassMult(4, "warrior", "str", stat);
                    break;
                case "cold_mult_def_warrior":
                    DMZClientConfig.setClassMult(4, "warrior", "def", stat);
                    break;
                case "cold_mult_pwr_warrior":
                    DMZClientConfig.setClassMult(4, "warrior", "pwr", stat);
                    break;
                case "cold_mult_regen_warrior":
                    DMZClientConfig.setClassMult(4, "warrior", "regen", stat);
                    break;
                case "cold_mult_str_spiritualist":
                    DMZClientConfig.setClassMult(4, "spiritualist", "str", stat);
                    break;
                case "cold_mult_def_spiritualist":
                    DMZClientConfig.setClassMult(4, "spiritualist", "def", stat);
                    break;
                case "cold_mult_pwr_spiritualist":
                    DMZClientConfig.setClassMult(4, "spiritualist", "pwr", stat);
                    break;
                case "cold_mult_regen_spiritualist":
                    DMZClientConfig.setClassMult(4, "spiritualist", "regen", stat);
                    break;
                case "majin_mult_str_warrior":
                    DMZClientConfig.setClassMult(5, "warrior", "str", stat);
                    break;
                case "majin_mult_def_warrior":
                    DMZClientConfig.setClassMult(5, "warrior", "def", stat);
                    break;
                case "majin_mult_pwr_warrior":
                    DMZClientConfig.setClassMult(5, "warrior", "pwr", stat);
                    break;
                case "majin_mult_regen_warrior":
                    DMZClientConfig.setClassMult(5, "warrior", "regen", stat);
                    break;
                case "majin_mult_str_spiritualist":
                    DMZClientConfig.setClassMult(5, "spiritualist", "str", stat);
                    break;
                case "majin_mult_def_spiritualist":
                    DMZClientConfig.setClassMult(5, "spiritualist", "def", stat);
                    break;
                case "majin_mult_pwr_spiritualist":
                    DMZClientConfig.setClassMult(5, "spiritualist", "pwr", stat);
                    break;
                case "majin_mult_regen_spiritualist":
                    DMZClientConfig.setClassMult(5, "spiritualist", "regen", stat);
                    break;
                case "human_mult_con_warrior":
                    DMZClientConfig.setClassMult(0, "warrior", "con", stat);
                    break;
                case "human_mult_ene_warrior":
                    DMZClientConfig.setClassMult(0, "warrior", "ene", stat);
                    break;
                case "human_mult_con_spiritualist":
                    DMZClientConfig.setClassMult(0, "spiritualist", "con", stat);
                    break;
                case "human_mult_ene_spiritualist":
                    DMZClientConfig.setClassMult(0, "spiritualist", "ene", stat);
                    break;
                case "saiyan_mult_con_warrior":
                    DMZClientConfig.setClassMult(1, "warrior", "con", stat);
                    break;
                case "saiyan_mult_ene_warrior":
                    DMZClientConfig.setClassMult(1, "warrior", "ene", stat);
                    break;
                case "saiyan_mult_con_spiritualist":
                    DMZClientConfig.setClassMult(1, "spiritualist", "con", stat);
                    break;
                case "saiyan_mult_ene_spiritualist":
                    DMZClientConfig.setClassMult(1, "spiritualist", "ene", stat);
                    break;
                case "namek_mult_con_warrior":
                    DMZClientConfig.setClassMult(2, "warrior", "con", stat);
                    break;
                case "namek_mult_ene_warrior":
                    DMZClientConfig.setClassMult(2, "warrior", "ene", stat);
                    break;
                case "namek_mult_con_spiritualist":
                    DMZClientConfig.setClassMult(2, "spiritualist", "con", stat);
                    break;
                case "namek_mult_ene_spiritualist":
                    DMZClientConfig.setClassMult(2, "spiritualist", "ene", stat);
                    break;
                case "bio_mult_con_warrior":
                    DMZClientConfig.setClassMult(3, "warrior", "con", stat);
                    break;
                case "bio_mult_ene_warrior":
                    DMZClientConfig.setClassMult(3, "warrior", "ene", stat);
                    break;
                case "bio_mult_con_spiritualist":
                    DMZClientConfig.setClassMult(3, "spiritualist", "con", stat);
                    break;
                case "bio_mult_ene_spiritualist":
                    DMZClientConfig.setClassMult(3, "spiritualist", "ene", stat);
                    break;
                case "cold_mult_con_warrior":
                    DMZClientConfig.setClassMult(4, "warrior", "con", stat);
                    break;
                case "cold_mult_ene_warrior":
                    DMZClientConfig.setClassMult(4, "warrior", "ene", stat);
                    break;
                case "cold_mult_con_spiritualist":
                    DMZClientConfig.setClassMult(4, "spiritualist", "con", stat);
                    break;
                case "cold_mult_ene_spiritualist":
                    DMZClientConfig.setClassMult(4, "spiritualist", "ene", stat);
                    break;
                case "majin_mult_con_warrior":
                    DMZClientConfig.setClassMult(5, "warrior", "con", stat);
                    break;
                case "majin_mult_ene_warrior":
                    DMZClientConfig.setClassMult(5, "warrior", "ene", stat);
                    break;
                case "majin_mult_con_spiritualist":
                    DMZClientConfig.setClassMult(5, "spiritualist", "con", stat);
                    break;
                case "majin_mult_ene_spiritualist":
                    DMZClientConfig.setClassMult(5, "spiritualist", "ene", stat);
                    break;
                case "human_passive":
                    DMZClientConfig.setHumanPassive((int) stat);
                    break;
                case "zenkai_timer":
                    DMZClientConfig.setSaiyanPassive("timer", (int) stat);
                    break;
                case "zenkai_heal":
                    DMZClientConfig.setSaiyanPassive("heal", (int) stat);
                    break;
                case "zenkai_boost":
                    DMZClientConfig.setSaiyanPassive("boost", (int) stat);
                    break;
                case "zenkai_cant":
                    DMZClientConfig.setSaiyanPassive("cant", (int) stat);
                    break;
                case "namek_passive":
                    DMZClientConfig.setNamekPassive((int) stat);
                    break;
                case "bio_passive_half":
                    DMZClientConfig.setBioPassive("half", (int) stat);
                    break;
                case "bio_passive_quarter":
                    DMZClientConfig.setBioPassive("quarter", (int) stat);
                    break;
                case "cold_passive":
                    DMZClientConfig.setColdPassive(stat);
                    break;
                case "majin_passive":
                    DMZClientConfig.setMajinPassive(stat);
                    break;
// Humanos
                case "trhuman_buffed_str":
                    DMZClientConfig.setTrHumanMulti("buffed", "str", stat);
                    break;
                case "trhuman_buffed_def":
                    DMZClientConfig.setTrHumanMulti("buffed", "def", stat);
                    break;
                case "trhuman_buffed_pwr":
                    DMZClientConfig.setTrHumanMulti("buffed", "pwr", stat);
                    break;
                case "trhuman_full_power_str":
                    DMZClientConfig.setTrHumanMulti("full_power", "str", stat);
                    break;
                case "trhuman_full_power_def":
                    DMZClientConfig.setTrHumanMulti("full_power", "def", stat);
                    break;
                case "trhuman_full_power_pwr":
                    DMZClientConfig.setTrHumanMulti("full_power", "pwr", stat);
                    break;
                case "trhuman_potential_unleashed_str":
                    DMZClientConfig.setTrHumanMulti("potential_unleashed", "str", stat);
                    break;
                case "trhuman_potential_unleashed_def":
                    DMZClientConfig.setTrHumanMulti("potential_unleashed", "def", stat);
                    break;
                case "trhuman_potential_unleashed_pwr":
                    DMZClientConfig.setTrHumanMulti("potential_unleashed", "pwr", stat);
                    break;

// Saiyans
                case "trsaiyan_oozaru_str":
                    DMZClientConfig.setTrSaiyanMulti("oozaru", "str", stat);
                    break;
                case "trsaiyan_oozaru_def":
                    DMZClientConfig.setTrSaiyanMulti("oozaru", "def", stat);
                    break;
                case "trsaiyan_oozaru_pwr":
                    DMZClientConfig.setTrSaiyanMulti("oozaru", "pwr", stat);
                    break;
                case "trsaiyan_ssj1_str":
                    DMZClientConfig.setTrSaiyanMulti("ssj1", "str", stat);
                    break;
                case "trsaiyan_ssj1_def":
                    DMZClientConfig.setTrSaiyanMulti("ssj1", "def", stat);
                    break;
                case "trsaiyan_ssj1_pwr":
                    DMZClientConfig.setTrSaiyanMulti("ssj1", "pwr", stat);
                    break;
                case "trsaiyan_ssgrade2_str":
                    DMZClientConfig.setTrSaiyanMulti("ssgrade2", "str", stat);
                    break;
                case "trsaiyan_ssgrade2_def":
                    DMZClientConfig.setTrSaiyanMulti("ssgrade2", "def", stat);
                    break;
                case "trsaiyan_ssgrade2_pwr":
                    DMZClientConfig.setTrSaiyanMulti("ssgrade2", "pwr", stat);
                    break;
                case "trsaiyan_ssgrade3_str":
                    DMZClientConfig.setTrSaiyanMulti("ssgrade3", "str", stat);
                    break;
                case "trsaiyan_ssgrade3_def":
                    DMZClientConfig.setTrSaiyanMulti("ssgrade3", "def", stat);
                    break;
                case "trsaiyan_ssgrade3_pwr":
                    DMZClientConfig.setTrSaiyanMulti("ssgrade3", "pwr", stat);
                    break;
                case "trsaiyan_mssj_str":
                    DMZClientConfig.setTrSaiyanMulti("mssj", "str", stat);
                    break;
                case "trsaiyan_mssj_def":
                    DMZClientConfig.setTrSaiyanMulti("mssj", "def", stat);
                    break;
                case "trsaiyan_mssj_pwr":
                    DMZClientConfig.setTrSaiyanMulti("mssj", "pwr", stat);
                    break;
                case "trsaiyan_ssj2_str":
                    DMZClientConfig.setTrSaiyanMulti("ssj2", "str", stat);
                    break;
                case "trsaiyan_ssj2_def":
                    DMZClientConfig.setTrSaiyanMulti("ssj2", "def", stat);
                    break;
                case "trsaiyan_ssj2_pwr":
                    DMZClientConfig.setTrSaiyanMulti("ssj2", "pwr", stat);
                    break;
                case "trsaiyan_ssj3_str":
                    DMZClientConfig.setTrSaiyanMulti("ssj3", "str", stat);
                    break;
                case "trsaiyan_ssj3_def":
                    DMZClientConfig.setTrSaiyanMulti("ssj3", "def", stat);
                    break;
                case "trsaiyan_ssj3_pwr":
                    DMZClientConfig.setTrSaiyanMulti("ssj3", "pwr", stat);
                    break;
                case "trsaiyan_golden_oozaru_str":
                    DMZClientConfig.setTrSaiyanMulti("golden_oozaru", "str", stat);
                    break;
                case "trsaiyan_golden_oozaru_def":
                    DMZClientConfig.setTrSaiyanMulti("golden_oozaru", "def", stat);
                    break;
                case "trsaiyan_golden_oozaru_pwr":
                    DMZClientConfig.setTrSaiyanMulti("golden_oozaru", "pwr", stat);
                    break;

// Namekianos
                case "trnamek_giant_str":
                    DMZClientConfig.setTrNamekMulti("giant", "str", stat);
                    break;
                case "trnamek_giant_def":
                    DMZClientConfig.setTrNamekMulti("giant", "def", stat);
                    break;
                case "trnamek_giant_pwr":
                    DMZClientConfig.setTrNamekMulti("giant", "pwr", stat);
                    break;
                case "trnamek_full_power_str":
                    DMZClientConfig.setTrNamekMulti("full_power", "str", stat);
                    break;
                case "trnamek_full_power_def":
                    DMZClientConfig.setTrNamekMulti("full_power", "def", stat);
                    break;
                case "trnamek_full_power_pwr":
                    DMZClientConfig.setTrNamekMulti("full_power", "pwr", stat);
                    break;
                case "trnamek_super_namek_str":
                    DMZClientConfig.setTrNamekMulti("super_namek", "str", stat);
                    break;
                case "trnamek_super_namek_def":
                    DMZClientConfig.setTrNamekMulti("super_namek", "def", stat);
                    break;
                case "trnamek_super_namek_pwr":
                    DMZClientConfig.setTrNamekMulti("super_namek", "pwr", stat);
                    break;

// Bioandroides
                case "trbioandroid_semi_perfect_str":
                    DMZClientConfig.setTrBioAndroidMulti("semi_perfect", "str", stat);
                    break;
                case "trbioandroid_semi_perfect_def":
                    DMZClientConfig.setTrBioAndroidMulti("semi_perfect", "def", stat);
                    break;
                case "trbioandroid_semi_perfect_pwr":
                    DMZClientConfig.setTrBioAndroidMulti("semi_perfect", "pwr", stat);
                    break;
                case "trbioandroid_perfect_str":
                    DMZClientConfig.setTrBioAndroidMulti("perfect", "str", stat);
                    break;
                case "trbioandroid_perfect_def":
                    DMZClientConfig.setTrBioAndroidMulti("perfect", "def", stat);
                    break;
                case "trbioandroid_perfect_pwr":
                    DMZClientConfig.setTrBioAndroidMulti("perfect", "pwr", stat);
                    break;

// Cold Demons
                case "trcolddemon_second_form_str":
                    DMZClientConfig.setTrColdDemonMulti("second_form", "str", stat);
                    break;
                case "trcolddemon_second_form_def":
                    DMZClientConfig.setTrColdDemonMulti("second_form", "def", stat);
                    break;
                case "trcolddemon_second_form_pwr":
                    DMZClientConfig.setTrColdDemonMulti("second_form", "pwr", stat);
                    break;
                case "trcolddemon_third_form_str":
                    DMZClientConfig.setTrColdDemonMulti("third_form", "str", stat);
                    break;
                case "trcolddemon_third_form_def":
                    DMZClientConfig.setTrColdDemonMulti("third_form", "def", stat);
                    break;
                case "trcolddemon_third_form_pwr":
                    DMZClientConfig.setTrColdDemonMulti("third_form", "pwr", stat);
                    break;
                case "trcolddemon_final_form_str":
                    DMZClientConfig.setTrColdDemonMulti("final_form", "str", stat);
                    break;
                case "trcolddemon_final_form_def":
                    DMZClientConfig.setTrColdDemonMulti("final_form", "def", stat);
                    break;
                case "trcolddemon_final_form_pwr":
                    DMZClientConfig.setTrColdDemonMulti("final_form", "pwr", stat);
                    break;
                case "trcolddemon_full_power_str":
                    DMZClientConfig.setTrColdDemonMulti("full_power", "str", stat);
                    break;
                case "trcolddemon_full_power_def":
                    DMZClientConfig.setTrColdDemonMulti("full_power", "def", stat);
                    break;
                case "trcolddemon_full_power_pwr":
                    DMZClientConfig.setTrColdDemonMulti("full_power", "pwr", stat);
                    break;

// Majin
                case "trmajin_base_str":
                    DMZClientConfig.setTrMajinMulti("base", "str", stat);
                    break;
                case "trmajin_base_def":
                    DMZClientConfig.setTrMajinMulti("base", "def", stat);
                    break;
                case "trmajin_base_pwr":
                    DMZClientConfig.setTrMajinMulti("base", "pwr", stat);
                    break;
                case "trmajin_evil_str":
                    DMZClientConfig.setTrMajinMulti("evil", "str", stat);
                    break;
                case "trmajin_evil_def":
                    DMZClientConfig.setTrMajinMulti("evil", "def", stat);
                    break;
                case "trmajin_evil_pwr":
                    DMZClientConfig.setTrMajinMulti("evil", "pwr", stat);
                    break;
                case "trmajin_kid_str":
                    DMZClientConfig.setTrMajinMulti("kid", "str", stat);
                    break;
                case "trmajin_kid_def":
                    DMZClientConfig.setTrMajinMulti("kid", "def", stat);
                    break;
                case "trmajin_kid_pwr":
                    DMZClientConfig.setTrMajinMulti("kid", "pwr", stat);
                    break;
                case "trmajin_super_str":
                    DMZClientConfig.setTrMajinMulti("super", "str", stat);
                    break;
                case "trmajin_super_def":
                    DMZClientConfig.setTrMajinMulti("super", "def", stat);
                    break;
                case "trmajin_super_pwr":
                    DMZClientConfig.setTrMajinMulti("super", "pwr", stat);
                    break;
                case "trmajin_ultra_str":
                    DMZClientConfig.setTrMajinMulti("ultra", "str", stat);
                    break;
                case "trmajin_ultra_def":
                    DMZClientConfig.setTrMajinMulti("ultra", "def", stat);
                    break;
                case "trmajin_ultra_pwr":
                    DMZClientConfig.setTrMajinMulti("ultra", "pwr", stat);
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}