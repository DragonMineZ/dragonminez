package com.yuseix.dragonminez.client.config;

import com.yuseix.dragonminez.common.config.races.transformations.*;
import com.yuseix.dragonminez.common.stats.DMZStatsAttributes;

import java.util.Locale;

public class DMZClientConfig {

    private static int maxStats = 5000;
    private static double multiplierZPoints = 1.2;
    private static double majin_multi = 1.5;
    private static double tree_might_multi = 1.3;
    private static int buyableTP = 0;
    private static int transfTPCost = 50000;
    private static int babaCooldown = 15, babaDuration = 10;
    private static int jumpLevels = 250, flyLevels = 750, meditationLevels = 150, potUnlockLevels = 1300, kiManipLevels = 5000, kiControlLevels = 100;
    private static int jumpMaster = 1000, flyMaster = 3000, meditationMaster = 600, potUnlockMaster = 5200, kiManipMaster = 20000, kiControlMaster = 400;

    private static int humanPassive = 35;
    private static int zenkaiTimer = 50, zenkaiHeal = 30, zenkaiBoost = 15, zenkaiCant = 3;
    private static int namekPassive = 60;
    private static int bioPassiveHalf = 8, bioPassiveQuarter = 4;
    private static double coldPassive = 1.35;
    private static double majinPassive = 2.5;

    private static double baseMult0 = 1, baseMult1 = 1, baseMult2 = 1, baseMult3 = 1, baseMult4 = 1, baseMult5 = 1;
    private static double buffed_human_str = 1, buffed_human_def = 1, buffed_human_pwr = 1, buffed_human_cost = 1;
    private static double full_power_human_str = 1, full_power_human_def = 1, full_power_human_pwr = 1, full_power_human_cost = 1;
    private static double potential_unleashed_human_str = 1, potential_unleashed_human_def = 1, potential_unleashed_human_pwr = 1, potential_unleashed_human_cost = 1;
    private static double oozaru_saiyan_str = 1, oozaru_saiyan_def = 1, oozaru_saiyan_pwr = 1, oozaru_saiyan_cost = 1;
    private static double ssj_saiyan_str = 1, ssj_saiyan_def = 1, ssj_saiyan_pwr = 1, ssj_saiyan_cost = 1;
    private static double ssgrade2_saiyan_str = 1, ssgrade2_saiyan_def = 1, ssgrade2_saiyan_pwr = 1, ssgrade2_saiyan_cost = 1;
    private static double ssgrade3_saiyan_str = 1, ssgrade3_saiyan_def = 1, ssgrade3_saiyan_pwr = 1, ssgrade3_saiyan_cost = 1;
    private static double ssjfp_saiyan_str = 1, ssjfp_saiyan_def = 1, ssjfp_saiyan_pwr = 1, ssjfp_saiyan_cost = 1;
    private static double ssj2_saiyan_str = 1, ssj2_saiyan_def = 1, ssj2_saiyan_pwr = 1, ssj2_saiyan_cost = 1;
    private static double ssj3_saiyan_str = 1, ssj3_saiyan_def = 1, ssj3_saiyan_pwr = 1, ssj3_saiyan_cost = 1;
    private static double golden_oozaru_saiyan_str = 1, golden_oozaru_saiyan_def = 1, golden_oozaru_saiyan_pwr = 1, golden_oozaru_saiyan_cost = 1;
    private static double giant_namek_str = 1, giant_namek_def = 1, giant_namek_pwr = 1, giant_namek_cost = 1;
    private static double full_power_namek_str = 1, full_power_namek_def = 1, full_power_namek_pwr = 1, full_power_namek_cost = 1;
    private static double super_namek_namek_str = 1, super_namek_namek_def = 1, super_namek_namek_pwr = 1, super_namek_namek_cost = 1;
    private static double semi_perfect_bio_str = 1, semi_perfect_bio_def = 1, semi_perfect_bio_pwr = 1, semi_perfect_bio_cost = 1;
    private static double perfect_bio_str = 1, perfect_bio_def = 1, perfect_bio_pwr = 1, perfect_bio_cost = 1;
    private static double second_form_cold_str = 1, second_form_cold_def = 1, second_form_cold_pwr = 1, second_form_cold_cost = 1;
    private static double third_form_cold_str = 1, third_form_cold_def = 1, third_form_cold_pwr = 1, third_form_cold_cost = 1;
    private static double final_form_cold_str = 1, final_form_cold_def = 1, final_form_cold_pwr = 1, final_form_cold_cost = 1;
    private static double full_power_cold_str = 1, full_power_cold_def = 1, full_power_cold_pwr = 1, full_power_cold_cost = 1;
    private static double evil_majin_str = 1, evil_majin_def = 1, evil_majin_pwr = 1, evil_majin_cost = 1;
    private static double kid_majin_str = 5, kid_majin_def = 5, kid_majin_pwr = 5, kid_majin_cost = 5;
    private static double super_majin_str = 1, super_majin_def = 1, super_majin_pwr = 1, super_majin_cost = 1;
    private static double ultra_majin_str = 1, ultra_majin_def = 1, ultra_majin_pwr = 1, ultra_majin_cost = 1;

    private static int ini_str_human_warrior = 5, ini_str_human_spiritualist = 5;
    private static int ini_def_human_warrior = 5, ini_def_human_spiritualist = 5;
    private static int ini_con_human_warrior = 5, ini_con_human_spiritualist = 5;
    private static int ini_pwr_human_warrior = 5, ini_pwr_human_spiritualist = 5;
    private static int ini_ene_human_warrior = 5, ini_ene_human_spiritualist = 5;

    private static double mult_str_human_warrior = 1, mult_str_human_spiritualist = 1;
    private static double mult_def_human_warrior = 1, mult_def_human_spiritualist = 1;
    private static double mult_con_human_warrior = 1, mult_con_human_spiritualist = 1;
    private static double mult_pwr_human_warrior = 1, mult_pwr_human_spiritualist = 1;
    private static double mult_ene_human_warrior = 1, mult_ene_human_spiritualist = 1;
    private static double mult_regen_human_warrior = 1, mult_regen_human_spiritualist = 1;

    private static int ini_str_saiyan_warrior = 5, ini_str_saiyan_spiritualist = 5;
    private static int ini_def_saiyan_warrior = 5, ini_def_saiyan_spiritualist = 5;
    private static int ini_con_saiyan_warrior = 5, ini_con_saiyan_spiritualist = 5;
    private static int ini_pwr_saiyan_warrior = 5, ini_pwr_saiyan_spiritualist = 5;
    private static int ini_ene_saiyan_warrior = 5, ini_ene_saiyan_spiritualist = 5;

    private static double mult_str_saiyan_warrior = 1, mult_str_saiyan_spiritualist = 1;
    private static double mult_def_saiyan_warrior = 1, mult_def_saiyan_spiritualist = 1;
    private static double mult_con_saiyan_warrior = 1, mult_con_saiyan_spiritualist = 1;
    private static double mult_pwr_saiyan_warrior = 1, mult_pwr_saiyan_spiritualist = 1;
    private static double mult_ene_saiyan_warrior = 1, mult_ene_saiyan_spiritualist = 1;
    private static double mult_regen_saiyan_warrior = 1, mult_regen_saiyan_spiritualist = 1;

    private static int ini_str_namek_warrior = 5, ini_str_namek_spiritualist = 5;
    private static int ini_def_namek_warrior = 5, ini_def_namek_spiritualist = 5;
    private static int ini_con_namek_warrior = 5, ini_con_namek_spiritualist = 5;
    private static int ini_pwr_namek_warrior = 5, ini_pwr_namek_spiritualist = 5;
    private static int ini_ene_namek_warrior = 5, ini_ene_namek_spiritualist = 5;

    private static double mult_str_namek_warrior = 1, mult_str_namek_spiritualist = 1;
    private static double mult_def_namek_warrior = 1, mult_def_namek_spiritualist = 1;
    private static double mult_con_namek_warrior = 1, mult_con_namek_spiritualist = 1;
    private static double mult_pwr_namek_warrior = 1, mult_pwr_namek_spiritualist = 1;
    private static double mult_ene_namek_warrior = 1, mult_ene_namek_spiritualist = 1;
    private static double mult_regen_namek_warrior = 1, mult_regen_namek_spiritualist = 1;

    private static int ini_str_bio_warrior = 5, ini_str_bio_spiritualist = 5;
    private static int ini_def_bio_warrior = 5, ini_def_bio_spiritualist = 5;
    private static int ini_con_bio_warrior = 5, ini_con_bio_spiritualist = 5;
    private static int ini_pwr_bio_warrior = 5, ini_pwr_bio_spiritualist = 5;
    private static int ini_ene_bio_warrior = 5, ini_ene_bio_spiritualist = 5;

    private static double mult_str_bio_warrior = 1, mult_str_bio_spiritualist = 1;
    private static double mult_def_bio_warrior = 1, mult_def_bio_spiritualist = 1;
    private static double mult_con_bio_warrior = 1, mult_con_bio_spiritualist = 1;
    private static double mult_pwr_bio_warrior = 1, mult_pwr_bio_spiritualist = 1;
    private static double mult_ene_bio_warrior = 1, mult_ene_bio_spiritualist = 1;
    private static double mult_regen_bio_warrior = 1, mult_regen_bio_spiritualist = 1;

    private static int ini_str_cold_warrior = 5, ini_str_cold_spiritualist = 5;
    private static int ini_def_cold_warrior = 5, ini_def_cold_spiritualist = 5;
    private static int ini_con_cold_warrior = 5, ini_con_cold_spiritualist = 5;
    private static int ini_pwr_cold_warrior = 5, ini_pwr_cold_spiritualist = 5;
    private static int ini_ene_cold_warrior = 5, ini_ene_cold_spiritualist = 5;

    private static double mult_str_cold_warrior = 1, mult_str_cold_spiritualist = 1;
    private static double mult_def_cold_warrior = 1, mult_def_cold_spiritualist = 1;
    private static double mult_con_cold_warrior = 1, mult_con_cold_spiritualist = 1;
    private static double mult_pwr_cold_warrior = 1, mult_pwr_cold_spiritualist = 1;
    private static double mult_ene_cold_warrior = 1, mult_ene_cold_spiritualist = 1;
    private static double mult_regen_cold_warrior = 1, mult_regen_cold_spiritualist = 1;

    private static int ini_str_majin_warrior = 5, ini_str_majin_spiritualist = 5;
    private static int ini_def_majin_warrior = 5, ini_def_majin_spiritualist = 5;
    private static int ini_con_majin_warrior = 5, ini_con_majin_spiritualist = 5;
    private static int ini_pwr_majin_warrior = 5, ini_pwr_majin_spiritualist = 5;
    private static int ini_ene_majin_warrior = 5, ini_ene_majin_spiritualist = 5;

    private static double mult_str_majin_warrior = 1, mult_str_majin_spiritualist = 1;
    private static double mult_def_majin_warrior = 1, mult_def_majin_spiritualist = 1;
    private static double mult_con_majin_warrior = 1, mult_con_majin_spiritualist = 1;
    private static double mult_pwr_majin_warrior = 1, mult_pwr_majin_spiritualist = 1;
    private static double mult_ene_majin_warrior = 1, mult_ene_majin_spiritualist = 1;
    private static double mult_regen_majin_warrior = 1, mult_regen_majin_spiritualist = 1;

    //trhuman
    private static double mult_trhuman_base_str = 1, mult_trhuman_buffed_str = 1,
            mult_trhuman_full_power_str = 1, mult_trhuman_potential_unleashed_str = 1;
    private static double mult_trhuman_base_def = 1, mult_trhuman_buffed_def = 1,
            mult_trhuman_full_power_def = 1, mult_trhuman_potential_unleashed_def = 1;
    private static double mult_trhuman_base_pwr = 1, mult_trhuman_buffed_pwr = 1,
            mult_trhuman_full_power_pwr = 1, mult_trhuman_potential_unleashed_pwr = 1;
    //trsaiyan
    private static double mult_trsaiyan_base_str = 1, mult_trsaiyan_oozaru_str = 1,
            mult_trsaiyan_ssj1_str = 1, mult_trsaiyan_ssgrade2_str = 1, mult_trsaiyan_ssgrade3_str = 1,
            mult_trsaiyan_mssj_str = 1, mult_trsaiyan_ssj2_str = 1, mult_trsaiyan_ssj3_str = 1,
            mult_trsaiyan_golden_oozaru_str = 1;
    private static double mult_trsaiyan_base_def = 1, mult_trsaiyan_oozaru_def = 1,
            mult_trsaiyan_ssj1_def = 1, mult_trsaiyan_ssgrade2_def = 1, mult_trsaiyan_ssgrade3_def = 1,
            mult_trsaiyan_mssj_def = 1, mult_trsaiyan_ssj2_def = 1, mult_trsaiyan_ssj3_def = 1,
            mult_trsaiyan_golden_oozaru_def = 1;
    private static double mult_trsaiyan_base_pwr = 1, mult_trsaiyan_oozaru_pwr = 1,
            mult_trsaiyan_ssj1_pwr = 1, mult_trsaiyan_ssgrade2_pwr = 1, mult_trsaiyan_ssgrade3_pwr = 1,
            mult_trsaiyan_mssj_pwr = 1, mult_trsaiyan_ssj2_pwr = 1, mult_trsaiyan_ssj3_pwr = 1,
            mult_trsaiyan_golden_oozaru_pwr = 1;
    //trnamek
    private static double mult_trnamek_base_str = 1, mult_trnamek_giant_str = 1,
            mult_trnamek_full_power_str = 1, mult_trnamek_super_namek_str = 1;
    private static double mult_trnamek_base_def = 1, mult_trnamek_giant_def = 1,
            mult_trnamek_full_power_def = 1, mult_trnamek_super_namek_def = 1;
    private static double mult_trnamek_base_pwr = 1, mult_trnamek_giant_pwr = 1,
            mult_trnamek_full_power_pwr = 1, mult_trnamek_super_namek_pwr = 1;
    //trbio
    private static double mult_trbioandroid_base_str = 1, mult_trbioandroid_semi_perfect_str = 1,
            mult_trbioandroid_perfect_str = 1;
    private static double mult_trbioandroid_base_def = 1, mult_trbioandroid_semi_perfect_def = 1,
            mult_trbioandroid_perfect_def = 1;
    private static double mult_trbioandroid_base_pwr = 1, mult_trbioandroid_semi_perfect_pwr = 1,
            mult_trbioandroid_perfect_pwr = 1;
    //trcolddemon
    private static double mult_trcolddemon_base_str = 1, mult_trcolddemon_second_form_str = 1,
            mult_trcolddemon_third_form_str = 1, mult_trcolddemon_final_form_str = 1,
            mult_trcolddemon_full_power_str = 1;
    private static double mult_trcolddemon_base_def = 1, mult_trcolddemon_second_form_def = 1,
            mult_trcolddemon_third_form_def = 1, mult_trcolddemon_final_form_def = 1,
            mult_trcolddemon_full_power_def = 1;
    private static double mult_trcolddemon_base_pwr = 1, mult_trcolddemon_second_form_pwr = 1,
            mult_trcolddemon_third_form_pwr = 1, mult_trcolddemon_final_form_pwr = 1,
            mult_trcolddemon_full_power_pwr = 1;
    //trmajin
    private static double mult_trmajin_base_str = 1, mult_trmajin_evil_str = 1,
            mult_trmajin_kid_str = 1, mult_trmajin_super_str = 1, mult_trmajin_ultra_str = 1;
    private static double mult_trmajin_base_def = 1, mult_trmajin_evil_def = 1,
            mult_trmajin_kid_def = 1, mult_trmajin_super_def = 1, mult_trmajin_ultra_def = 1;
    private static double mult_trmajin_base_pwr = 1, mult_trmajin_evil_pwr = 1,
            mult_trmajin_kid_pwr = 1, mult_trmajin_super_pwr = 1, mult_trmajin_ultra_pwr = 1;

    public static int getInit_StrStat(String race, String clase) {
        int initStat = 0;
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan"-> initStat = DMZClientConfig.ini_str_saiyan_warrior;
                    case "namek"->initStat = DMZClientConfig.ini_str_namek_warrior;
                    case "bio" -> initStat = DMZClientConfig.ini_str_bio_warrior;
                    case "cold" -> initStat = DMZClientConfig.ini_str_cold_warrior;
                    case "majin" -> initStat = DMZClientConfig.ini_str_majin_warrior;
                    default ->initStat = DMZClientConfig.ini_str_human_warrior;
                }
                break;
            default:
                switch (race){
                    case "saiyan"-> initStat = DMZClientConfig.ini_str_saiyan_spiritualist;
                    case "namek"->initStat = DMZClientConfig.ini_str_namek_spiritualist;
                    case "bio" -> initStat = DMZClientConfig.ini_str_bio_spiritualist;
                    case "cold" -> initStat = DMZClientConfig.ini_str_cold_spiritualist;
                    case "majin" -> initStat = DMZClientConfig.ini_str_majin_spiritualist;
                    default ->initStat = DMZClientConfig.ini_str_human_spiritualist;
                }
                break;
        }

        return initStat;
    }
    public static int getInit_DefStat(String race, String clase) {
        int initStat = 0;
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan" -> initStat = DMZClientConfig.ini_def_saiyan_warrior;
                    case "namek" -> initStat = DMZClientConfig.ini_def_namek_warrior;
                    case "bio" -> initStat = DMZClientConfig.ini_def_bio_warrior;
                    case "cold" -> initStat = DMZClientConfig.ini_def_cold_warrior;
                    case "majin" -> initStat = DMZClientConfig.ini_def_majin_warrior;
                    default -> initStat = DMZClientConfig.ini_def_human_warrior;
                }
                break;
            default:
                switch (race){
                    case "saiyan"->initStat = DMZClientConfig.ini_def_saiyan_spiritualist;
                    case "namek"->initStat = DMZClientConfig.ini_def_namek_spiritualist;
                    case "bio"->initStat = DMZClientConfig.ini_def_bio_spiritualist;
                    case "cold"->initStat = DMZClientConfig.ini_def_cold_spiritualist;
                    case "majin"->initStat = DMZClientConfig.ini_def_majin_spiritualist;
                    default->initStat = DMZClientConfig.ini_def_human_spiritualist;
                }
                break;
        }


        return initStat;
    }
    public static int getInit_ConStat(String race, String clase) {
        int initStat = 0;
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan"->initStat = DMZClientConfig.ini_con_saiyan_warrior;
                    case "namek"->initStat = DMZClientConfig.ini_con_namek_warrior;
                    case "bio"->initStat = DMZClientConfig.ini_con_bio_warrior;
                    case "cold"->initStat = DMZClientConfig.ini_con_cold_warrior;
                    case "majin"->initStat = DMZClientConfig.ini_con_majin_warrior;
                    default -> initStat = DMZClientConfig.ini_con_human_warrior;
                }
                break;
            default:
                switch (race){
                    case "saiyan"->initStat = DMZClientConfig.ini_con_saiyan_spiritualist;
                    case "namek"->initStat = DMZClientConfig.ini_con_namek_spiritualist;
                    case "bio"->initStat = DMZClientConfig.ini_con_bio_spiritualist;
                    case "cold"->initStat = DMZClientConfig.ini_con_cold_spiritualist;
                    case "majin"->initStat = DMZClientConfig.ini_con_majin_spiritualist;
                    default -> initStat = DMZClientConfig.ini_con_human_spiritualist;
                }
                break;
        }

        return initStat;
    }
    public static int getInit_PWRStat(String race, String clase) {
        int initStat = 0;
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan"->initStat = DMZClientConfig.ini_pwr_saiyan_warrior;
                    case "namek"->initStat = DMZClientConfig.ini_pwr_namek_warrior;
                    case "bio"->initStat = DMZClientConfig.ini_pwr_bio_warrior;
                    case "cold"->initStat = DMZClientConfig.ini_pwr_cold_warrior;
                    case "majin"->initStat = DMZClientConfig.ini_pwr_majin_warrior;
                    default->initStat = DMZClientConfig.ini_pwr_human_warrior;
                }
                break;
            default:
                switch (race){
                    case "saiyan"->initStat = DMZClientConfig.ini_pwr_saiyan_spiritualist;
                    case "namek"->initStat = DMZClientConfig.ini_pwr_namek_spiritualist;
                    case "bio"->initStat = DMZClientConfig.ini_pwr_bio_spiritualist;
                    case "cold"->initStat = DMZClientConfig.ini_pwr_cold_spiritualist;
                    case "majin"->initStat = DMZClientConfig.ini_pwr_majin_spiritualist;
                    default->initStat = DMZClientConfig.ini_pwr_human_spiritualist;
                }
                break;
        }

        return initStat;
    }
    public static int getInit_ENEStat(String race, String clase) {
        int initStat = 0;
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan" -> initStat = DMZClientConfig.ini_ene_saiyan_warrior;
                    case "namek" -> initStat = DMZClientConfig.ini_ene_namek_warrior;
                    case "bio" -> initStat = DMZClientConfig.ini_ene_bio_warrior;
                    case "cold" -> initStat = DMZClientConfig.ini_ene_cold_warrior;
                    case "majin" -> initStat = DMZClientConfig.ini_ene_majin_warrior;
                    default -> initStat = DMZClientConfig.ini_ene_human_warrior;
                }
                break;
            default:
                switch (race){
                    case "saiyan" -> initStat = DMZClientConfig.ini_ene_saiyan_spiritualist;
                    case "namek" -> initStat = DMZClientConfig.ini_ene_namek_spiritualist;
                    case "bio" -> initStat = DMZClientConfig.ini_ene_bio_spiritualist;
                    case "cold" -> initStat = DMZClientConfig.ini_ene_cold_spiritualist;
                    case "majin" -> initStat = DMZClientConfig.ini_ene_majin_spiritualist;
                    default -> initStat = DMZClientConfig.ini_ene_human_spiritualist;
                }
                break;
        }

        return initStat;
    }
    public static void setInit_StrStat(String race, String clase, int value) {
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan" -> DMZClientConfig.ini_str_saiyan_warrior = value;
                    case "namek" -> DMZClientConfig.ini_str_namek_warrior = value;
                    case "bio" -> DMZClientConfig.ini_str_bio_warrior = value;
                    case "cold" -> DMZClientConfig.ini_str_cold_warrior = value;
                    case "majin" -> DMZClientConfig.ini_str_majin_warrior = value;
                    default ->DMZClientConfig.ini_str_human_warrior = value;
                }
                break;
            default:
                switch (race){
                    case "saiyan" -> DMZClientConfig.ini_str_saiyan_spiritualist = value;
                    case "namek" -> DMZClientConfig.ini_str_namek_spiritualist = value;
                    case "bio" -> DMZClientConfig.ini_str_bio_spiritualist = value;
                    case "cold" -> DMZClientConfig.ini_str_cold_spiritualist = value;
                    case "majin" -> DMZClientConfig.ini_str_majin_spiritualist = value;
                    default ->DMZClientConfig.ini_str_human_spiritualist = value;
                }
                break;
        }

    }
    public static void setInit_DefStat(String race, String clase, int value) {
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan" -> DMZClientConfig.ini_def_saiyan_warrior = value;
                    case "namek" -> DMZClientConfig.ini_def_namek_warrior = value;
                    case "bio" -> DMZClientConfig.ini_def_bio_warrior = value;
                    case "cold" -> DMZClientConfig.ini_def_cold_warrior = value;
                    case "majin" -> DMZClientConfig.ini_def_majin_warrior = value;
                    default -> DMZClientConfig.ini_def_human_warrior = value;
                }
                break;
            default:
                switch (race){
                    case "saiyan" -> DMZClientConfig.ini_def_saiyan_spiritualist = value;
                    case "namek" -> DMZClientConfig.ini_def_namek_spiritualist = value;
                    case "bio" -> DMZClientConfig.ini_def_bio_spiritualist = value;
                    case "cold" -> DMZClientConfig.ini_def_cold_spiritualist = value;
                    case "majin" -> DMZClientConfig.ini_def_majin_spiritualist = value;
                    default -> DMZClientConfig.ini_def_human_spiritualist = value;
                }
                break;
        }

    }
    public static void setInit_ConStat(String race, String clase, int value) {
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan" -> DMZClientConfig.ini_con_saiyan_warrior = value;
                    case "namek" -> DMZClientConfig.ini_con_namek_warrior = value;
                    case "bio" -> DMZClientConfig.ini_con_bio_warrior = value;
                    case "cold" -> DMZClientConfig.ini_con_cold_warrior = value;
                    case "majin" -> DMZClientConfig.ini_con_majin_warrior = value;
                    default -> DMZClientConfig.ini_con_human_warrior = value;
                }
                break;
            default:
                switch (race){
                    case "saiyan" -> DMZClientConfig.ini_con_saiyan_spiritualist = value;
                    case "namek" -> DMZClientConfig.ini_con_namek_spiritualist = value;
                    case "bio" -> DMZClientConfig.ini_con_bio_spiritualist = value;
                    case "cold" -> DMZClientConfig.ini_con_cold_spiritualist = value;
                    case "majin" -> DMZClientConfig.ini_con_majin_spiritualist = value;
                    default -> DMZClientConfig.ini_con_human_spiritualist = value;
                }
                break;
        }

    }
    public static void setInit_PWRStat(String race, String clase, int value) {
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan"->DMZClientConfig.ini_pwr_saiyan_warrior = value;
                    case "namek"->DMZClientConfig.ini_pwr_namek_warrior = value;
                    case "bio"->DMZClientConfig.ini_pwr_bio_warrior = value;
                    case "cold"->DMZClientConfig.ini_pwr_cold_warrior = value;
                    case "majin"->DMZClientConfig.ini_pwr_majin_warrior = value;
                    default->DMZClientConfig.ini_pwr_human_warrior = value;
                }
                break;
            default:
                switch (race){
                    case "saiyan"->DMZClientConfig.ini_pwr_saiyan_spiritualist = value;
                    case "namek"->DMZClientConfig.ini_pwr_namek_spiritualist = value;
                    case "bio"->DMZClientConfig.ini_pwr_bio_spiritualist = value;
                    case "cold"->DMZClientConfig.ini_pwr_cold_spiritualist = value;
                    case "majin"->DMZClientConfig.ini_pwr_majin_spiritualist = value;
                    default->DMZClientConfig.ini_pwr_human_spiritualist = value;
                }
                break;
        }

    }
    public static void setInit_ENEStat(String race, String clase, int value) {
        switch (clase){
            case "warrior":
                switch (race){
                    case "saiyan" -> DMZClientConfig.ini_ene_saiyan_warrior = value;
                    case "namek" -> DMZClientConfig.ini_ene_namek_warrior = value;
                    case "bio" -> DMZClientConfig.ini_ene_bio_warrior = value;
                    case "cold" -> DMZClientConfig.ini_ene_cold_warrior = value;
                    case "majin" -> DMZClientConfig.ini_ene_majin_warrior = value;
                    default -> DMZClientConfig.ini_ene_human_warrior = value;
                }
                break;
            default:
                switch (race){
                    case "saiyan" -> DMZClientConfig.ini_ene_saiyan_spiritualist= value;
                    case "namek" -> DMZClientConfig.ini_ene_namek_spiritualist = value;
                    case "bio" -> DMZClientConfig.ini_ene_bio_spiritualist = value;
                    case "cold" -> DMZClientConfig.ini_ene_cold_spiritualist = value;
                    case "majin" -> DMZClientConfig.ini_ene_majin_spiritualist = value;
                    default -> DMZClientConfig.ini_ene_human_spiritualist = value;
                }
                break;
        }

    }
    public static double getMajin_multi() {
        return majin_multi;
    }
    public static void setMajin_multi(double majin_multi) {
        DMZClientConfig.majin_multi = majin_multi;
    }
    public static double getTree_might_multi() {
        return tree_might_multi;
    }
    public static void setTree_might_multi(double tree_might_multi) {
        DMZClientConfig.tree_might_multi = tree_might_multi;
    }
    public static int getMaxStats() {
        return maxStats;
    }
    public static void setMaxStats(int value) {
        maxStats = value;
    }
    public static double getMultiplierZPoints() {
        return multiplierZPoints;
    }
    public static void setMultiplierZPoints(double value) {
        multiplierZPoints = value;
    }
    public static int getTransfTPCost() {
        return transfTPCost;
    }
    public static void setTransfTPCost(int value) {
        transfTPCost = value;
    }
    public static int getBuyableTP() {
        return buyableTP;
    }
    public static void setBuyableTP(int value) {
        buyableTP = value;
    }
    public static int getBabaCooldown() {
        return babaCooldown;
    }
    public static void setBabaCooldown(int babaCooldown) {
        DMZClientConfig.babaCooldown = babaCooldown;
    }
    public static int getBabaDuration() {
        return babaDuration;
    }
    public static void setBabaDuration(int babaDuration) {
        DMZClientConfig.babaDuration = babaDuration;
    }
    public static int getJumpLevels() {
        return jumpLevels;
    }
    public static void setJumpLevels(int jumpLevels) {
        DMZClientConfig.jumpLevels = jumpLevels;
    }
    public static int getFlyLevels() {
        return flyLevels;
    }
    public static void setFlyLevels(int flyLevels) {
        DMZClientConfig.flyLevels = flyLevels;
    }
    public static int getMeditationLevels() {
        return meditationLevels;
    }
    public static void setMeditationLevels(int meditationLevels) {
        DMZClientConfig.meditationLevels = meditationLevels;
    }
    public static int getPotUnlockLevels() {
        return potUnlockLevels;
    }
    public static void setPotUnlockLevels(int potUnlockLevels) {
        DMZClientConfig.potUnlockLevels = potUnlockLevels;
    }
    public static int getKiManipLevels() {
        return kiManipLevels;
    }
    public static void setKiManipLevels(int kiManipLevels) {
        DMZClientConfig.kiManipLevels = kiManipLevels;
    }
    public static int getKiControlLevels() {
        return kiControlLevels;
    }
    public static void setKiControlLevels(int kiControlLevels) {
        DMZClientConfig.kiControlLevels = kiControlLevels;
    }
    public static void setBaseStats(int race, double stat) {
        switch (race) {
            case 0 -> DMZClientConfig.baseMult0 = stat;
            case 1 -> DMZClientConfig.baseMult1 = stat;
            case 2 -> DMZClientConfig.baseMult2 = stat;
            case 3 -> DMZClientConfig.baseMult3 = stat;
            case 4 -> DMZClientConfig.baseMult4 = stat;
            case 5 -> DMZClientConfig.baseMult5 = stat;
        }
    }
    public static double getBaseStats(int race) {
        return switch (race) {
            case 0 -> DMZClientConfig.baseMult0;
            case 1 -> DMZClientConfig.baseMult1;
            case 2 -> DMZClientConfig.baseMult2;
            case 3 -> DMZClientConfig.baseMult3;
            case 4 -> DMZClientConfig.baseMult4;
            case 5 -> DMZClientConfig.baseMult5;
            default -> DMZClientConfig.baseMult0;
        };
    }
    public static void setSTRStat(int race, String form, double stat) {
        switch (race) {
            case 0 -> {
                switch (form) {
                    case "buffed" -> DMZClientConfig.buffed_human_str = stat;
                    case "full_power" -> DMZClientConfig.full_power_human_str = stat;
                    case "potential_unleashed" -> DMZClientConfig.potential_unleashed_human_str = stat;
                }
            }
            case 1 -> {
                switch (form) {
                    case "oozaru" -> DMZClientConfig.oozaru_saiyan_str = stat;
                    case "ssj1" -> DMZClientConfig.ssj_saiyan_str = stat;
                    case "ssgrade2" -> DMZClientConfig.ssgrade2_saiyan_str = stat;
                    case "ssgrade3" -> DMZClientConfig.ssgrade3_saiyan_str = stat;
                    case "ssjfp" -> DMZClientConfig.ssjfp_saiyan_str = stat;
                    case "ssj2" -> DMZClientConfig.ssj2_saiyan_str = stat;
                    case "ssj3" -> DMZClientConfig.ssj3_saiyan_str = stat;
                    case "golden_oozaru" -> DMZClientConfig.golden_oozaru_saiyan_str = stat;
                }
            }
            case 2 -> {
                switch (form) {
                    case "giant" -> DMZClientConfig.giant_namek_str = stat;
                    case "full_power" -> DMZClientConfig.full_power_namek_str = stat;
                    case "super_namek" -> DMZClientConfig.super_namek_namek_str = stat;
                }
            }
            case 3 -> {
                switch (form) {
                    case "semi_perfect" -> DMZClientConfig.semi_perfect_bio_str = stat;
                    case "perfect" -> DMZClientConfig.perfect_bio_str = stat;
                }
            }
            case 4 -> {
                switch (form) {
                    case "second_form" -> DMZClientConfig.second_form_cold_str = stat;
                    case "third_form" -> DMZClientConfig.third_form_cold_str = stat;
                    case "final_form" -> DMZClientConfig.final_form_cold_str = stat;
                    case "full_power" -> DMZClientConfig.full_power_cold_str = stat;
                }
            }
            case 5 -> {
                switch (form) {
                    case "evil" -> DMZClientConfig.evil_majin_str = stat;
                    case "kid" -> DMZClientConfig.kid_majin_str = stat;
                    case "super" -> DMZClientConfig.super_majin_str = stat;
                    case "ultra" -> DMZClientConfig.ultra_majin_str = stat;
                }
            }
        }
    }
    public static void setDEFStat(int race, String form, double stat) {
        switch (race) {
            case 0 -> {
                switch (form) {
                    case "buffed" -> DMZClientConfig.buffed_human_def = stat;
                    case "full_power" -> DMZClientConfig.full_power_human_def = stat;
                    case "potential_unleashed" -> DMZClientConfig.potential_unleashed_human_def = stat;
                }
            }
            case 1 -> {
                switch (form) {
                    case "oozaru" -> DMZClientConfig.oozaru_saiyan_def = stat;
                    case "ssj1" -> DMZClientConfig.ssj_saiyan_def = stat;
                    case "ssgrade2" -> DMZClientConfig.ssgrade2_saiyan_def = stat;
                    case "ssgrade3" -> DMZClientConfig.ssgrade3_saiyan_def = stat;
                    case "ssjfp" -> DMZClientConfig.ssjfp_saiyan_def = stat;
                    case "ssj2" -> DMZClientConfig.ssj2_saiyan_def = stat;
                    case "ssj3" -> DMZClientConfig.ssj3_saiyan_def = stat;
                    case "golden_oozaru" -> DMZClientConfig.golden_oozaru_saiyan_def = stat;
                }
            }
            case 2 -> {
                switch (form) {
                    case "giant" -> DMZClientConfig.giant_namek_def = stat;
                    case "full_power" -> DMZClientConfig.full_power_namek_def = stat;
                    case "super_namek" -> DMZClientConfig.super_namek_namek_def = stat;
                }
            }
            case 3 -> {
                switch (form) {
                    case "semi_perfect" -> DMZClientConfig.semi_perfect_bio_def = stat;
                    case "perfect" -> DMZClientConfig.perfect_bio_def = stat;
                }
            }
            case 4 -> {
                switch (form) {
                    case "second_form" -> DMZClientConfig.second_form_cold_def = stat;
                    case "third_form" -> DMZClientConfig.third_form_cold_def = stat;
                    case "final_form" -> DMZClientConfig.final_form_cold_def = stat;
                    case "full_power" -> DMZClientConfig.full_power_cold_def = stat;
                }
            }
            case 5 -> {
                switch (form) {
                    case "evil" -> DMZClientConfig.evil_majin_def = stat;
                    case "kid" -> DMZClientConfig.kid_majin_def = stat;
                    case "super" -> DMZClientConfig.super_majin_def = stat;
                    case "ultra" -> DMZClientConfig.ultra_majin_def = stat;
                }
            }
        }
    }
    public static void setPWRStat(int race, String form, double stat) {
        switch (race) {
            case 0 -> {
                switch (form) {
                    case "buffed" -> DMZClientConfig.buffed_human_pwr = stat;
                    case "full_power" -> DMZClientConfig.full_power_human_pwr = stat;
                    case "potential_unleashed" -> DMZClientConfig.potential_unleashed_human_pwr = stat;
                }
            }
            case 1 -> {
                switch (form) {
                    case "oozaru" -> DMZClientConfig.oozaru_saiyan_pwr = stat;
                    case "ssj1" -> DMZClientConfig.ssj_saiyan_pwr = stat;
                    case "ssgrade2" -> DMZClientConfig.ssgrade2_saiyan_pwr = stat;
                    case "ssgrade3" -> DMZClientConfig.ssgrade3_saiyan_pwr = stat;
                    case "ssjfp" -> DMZClientConfig.ssjfp_saiyan_pwr = stat;
                    case "ssj2" -> DMZClientConfig.ssj2_saiyan_pwr = stat;
                    case "ssj3" -> DMZClientConfig.ssj3_saiyan_pwr = stat;
                    case "golden_oozaru" -> DMZClientConfig.golden_oozaru_saiyan_pwr = stat;
                }
            }
            case 2 -> {
                switch (form) {
                    case "giant" -> DMZClientConfig.giant_namek_pwr = stat;
                    case "full_power" -> DMZClientConfig.full_power_namek_pwr = stat;
                    case "super_namek" -> DMZClientConfig.super_namek_namek_pwr = stat;
                }
            }
            case 3 -> {
                switch (form) {
                    case "semi_perfect" -> DMZClientConfig.semi_perfect_bio_pwr = stat;
                    case "perfect" -> DMZClientConfig.perfect_bio_pwr = stat;
                }
            }
            case 4 -> {
                switch (form) {
                    case "second_form" -> DMZClientConfig.second_form_cold_pwr = stat;
                    case "third_form" -> DMZClientConfig.third_form_cold_pwr = stat;
                    case "final_form" -> DMZClientConfig.final_form_cold_pwr = stat;
                    case "full_power" -> DMZClientConfig.full_power_cold_pwr = stat;
                }
            }
            case 5 -> {
                switch (form) {
                    case "evil" -> DMZClientConfig.evil_majin_pwr = stat;
                    case "kid" -> DMZClientConfig.kid_majin_pwr = stat;
                    case "super" -> DMZClientConfig.super_majin_pwr = stat;
                    case "ultra" -> DMZClientConfig.ultra_majin_pwr = stat;
                }
            }
        }
    }
    public static void setCostForm(int race, String form, double stat) {
        switch (race) {
            case 0 -> {
                switch (form) {
                    case "buffed" -> DMZClientConfig.buffed_human_cost = stat;
                    case "full_power" -> DMZClientConfig.full_power_human_cost = stat;
                    case "potential_unleashed" -> DMZClientConfig.potential_unleashed_human_cost = stat;
                }
            }
            case 1 -> {
                switch (form) {
                    case "oozaru" -> DMZClientConfig.oozaru_saiyan_cost = stat;
                    case "ssj1" -> DMZClientConfig.ssj_saiyan_cost = stat;
                    case "ssgrade2" -> DMZClientConfig.ssgrade2_saiyan_cost = stat;
                    case "ssgrade3" -> DMZClientConfig.ssgrade3_saiyan_cost = stat;
                    case "ssjfp" -> DMZClientConfig.ssjfp_saiyan_cost = stat;
                    case "ssj2" -> DMZClientConfig.ssj2_saiyan_cost = stat;
                    case "ssj3" -> DMZClientConfig.ssj3_saiyan_cost = stat;
                    case "golden_oozaru" -> DMZClientConfig.golden_oozaru_saiyan_cost = stat;
                }
            }
            case 2 -> {
                switch (form) {
                    case "giant" -> DMZClientConfig.giant_namek_cost = stat;
                    case "full_power" -> DMZClientConfig.full_power_namek_cost = stat;
                    case "super_namek" -> DMZClientConfig.super_namek_namek_cost = stat;
                }
            }
            case 3 -> {
                switch (form) {
                    case "semi_perfect" -> DMZClientConfig.semi_perfect_bio_cost = stat;
                    case "perfect" -> DMZClientConfig.perfect_bio_cost = stat;
                }
            }
            case 4 -> {
                switch (form) {
                    case "second_form" -> DMZClientConfig.second_form_cold_cost = stat;
                    case "third_form" -> DMZClientConfig.third_form_cold_cost = stat;
                    case "final_form" -> DMZClientConfig.final_form_cold_cost = stat;
                    case "full_power" -> DMZClientConfig.full_power_cold_cost = stat;
                }
            }
            case 5 -> {
                switch (form) {
                    case "evil" -> DMZClientConfig.evil_majin_cost = stat;
                    case "kid" -> DMZClientConfig.kid_majin_cost = stat;
                    case "super" -> DMZClientConfig.super_majin_cost = stat;
                    case "ultra" -> DMZClientConfig.ultra_majin_cost = stat;
                }
            }
        }
    }
    public static double getDMZStat(int race, String form, String stat) {
        return switch (race) {
            case 0 -> switch (stat.toUpperCase(Locale.ROOT)) {
                case "STR" -> switch (form) {
                    case "base" -> getBaseStats(0);
                    case "buffed" -> DMZClientConfig.buffed_human_str;
                    case "full_power" -> DMZClientConfig.full_power_human_str;
                    case "potential_unleashed" -> DMZClientConfig.potential_unleashed_human_str;
                    default -> 1;
                };
                case "DEF" -> switch (form) {
                    case "base" -> getBaseStats(0);
                    case "buffed" -> DMZClientConfig.buffed_human_def;
                    case "full_power" -> DMZClientConfig.full_power_human_def;
                    case "potential_unleashed" -> DMZClientConfig.potential_unleashed_human_def;
                    default -> 1;
                };
                case "PWR" -> switch (form) {
                    case "base" -> getBaseStats(0);
                    case "buffed" -> DMZClientConfig.buffed_human_pwr;
                    case "full_power" -> DMZClientConfig.full_power_human_pwr;
                    case "potential_unleashed" -> DMZClientConfig.potential_unleashed_human_pwr;
                    default -> 1;
                };
                case "COST" -> switch (form) {
                    case "base" -> getBaseStats(0);
                    case "buffed" -> DMZClientConfig.buffed_human_cost;
                    case "full_power" -> DMZClientConfig.full_power_human_cost;
                    case "potential_unleashed" -> DMZClientConfig.potential_unleashed_human_cost;
                    default -> 1;
                };
                default -> 1;
            };
            case 1 -> switch (stat.toUpperCase(Locale.ROOT)) {
                case "STR" -> switch (form) {
                    case "base" -> getBaseStats(1);
                    case "oozaru" -> DMZClientConfig.oozaru_saiyan_str;
                    case "ssj1" -> DMZClientConfig.ssj_saiyan_str;
                    case "ssgrade2" -> DMZClientConfig.ssgrade2_saiyan_str;
                    case "ssgrade3" -> DMZClientConfig.ssgrade3_saiyan_str;
                    case "ssjfp" -> DMZClientConfig.ssjfp_saiyan_str;
                    case "ssj2" -> DMZClientConfig.ssj2_saiyan_str;
                    case "ssj3" -> DMZClientConfig.ssj3_saiyan_str;
                    case "golden_oozaru" -> DMZClientConfig.golden_oozaru_saiyan_str;
                    default -> 1;
                };
                case "DEF" -> switch (form) {
                    case "base" -> getBaseStats(1);
                    case "oozaru" -> DMZClientConfig.oozaru_saiyan_def;
                    case "ssj1" -> DMZClientConfig.ssj_saiyan_def;
                    case "ssgrade2" -> DMZClientConfig.ssgrade2_saiyan_def;
                    case "ssgrade3" -> DMZClientConfig.ssgrade3_saiyan_def;
                    case "ssjfp" -> DMZClientConfig.ssjfp_saiyan_def;
                    case "ssj2" -> DMZClientConfig.ssj2_saiyan_def;
                    case "ssj3" -> DMZClientConfig.ssj3_saiyan_def;
                    case "golden_oozaru" -> DMZClientConfig.golden_oozaru_saiyan_def;
                    default -> 1;
                };
                case "PWR" -> switch (form) {
                    case "base" -> getBaseStats(1);
                    case "oozaru" -> DMZClientConfig.oozaru_saiyan_pwr;
                    case "ssj1" -> DMZClientConfig.ssj_saiyan_pwr;
                    case "ssgrade2" -> DMZClientConfig.ssgrade2_saiyan_pwr;
                    case "ssgrade3" -> DMZClientConfig.ssgrade3_saiyan_pwr;
                    case "ssjfp" -> DMZClientConfig.ssjfp_saiyan_pwr;
                    case "ssj2" -> DMZClientConfig.ssj2_saiyan_pwr;
                    case "ssj3" -> DMZClientConfig.ssj3_saiyan_pwr;
                    case "golden_oozaru" -> DMZClientConfig.golden_oozaru_saiyan_pwr;
                    default -> 1;
                };
                case "COST" -> switch (form) {
                    case "base" -> getBaseStats(1);
                    case "oozaru" -> DMZClientConfig.oozaru_saiyan_cost;
                    case "ssj1" -> DMZClientConfig.ssj_saiyan_cost;
                    case "ssgrade2" -> DMZClientConfig.ssgrade2_saiyan_cost;
                    case "ssgrade3" -> DMZClientConfig.ssgrade3_saiyan_cost;
                    case "ssjfp" -> DMZClientConfig.ssjfp_saiyan_cost;
                    case "ssj2" -> DMZClientConfig.ssj2_saiyan_cost;
                    case "ssj3" -> DMZClientConfig.ssj3_saiyan_cost;
                    case "golden_oozaru" -> DMZClientConfig.golden_oozaru_saiyan_cost;
                    default -> 1;
                };
                default -> 1;
            };
            case 2 -> switch (stat.toUpperCase(Locale.ROOT)) {
                case "STR" -> switch (form) {
                    case "base" -> getBaseStats(2);
                    case "giant" -> DMZClientConfig.giant_namek_str;
                    case "full_power" -> DMZClientConfig.full_power_namek_str;
                    case "super_namek" -> DMZClientConfig.super_namek_namek_str;
                    default -> 1;
                };
                case "DEF" -> switch (form) {
                    case "base" -> getBaseStats(2);
                    case "giant" -> DMZClientConfig.giant_namek_def;
                    case "full_power" -> DMZClientConfig.full_power_namek_def;
                    case "super_namek" -> DMZClientConfig.super_namek_namek_def;
                    default -> 1;
                };
                case "PWR" -> switch (form) {
                    case "base" -> getBaseStats(2);
                    case "giant" -> DMZClientConfig.giant_namek_pwr;
                    case "full_power" -> DMZClientConfig.full_power_namek_pwr;
                    case "super_namek" -> DMZClientConfig.super_namek_namek_pwr;
                    default -> 1;
                };
                case "COST" -> switch (form) {
                    case "base" -> getBaseStats(2);
                    case "giant" -> DMZClientConfig.giant_namek_cost;
                    case "full_power" -> DMZClientConfig.full_power_namek_cost;
                    case "super_namek" -> DMZClientConfig.super_namek_namek_cost;
                    default -> 1;
                };
                default -> 1;
            };
            case 3 -> switch (stat.toUpperCase(Locale.ROOT)) {
                case "STR" -> switch (form) {
                    case "base" -> getBaseStats(3);
                    case "semi_perfect" -> DMZClientConfig.semi_perfect_bio_str;
                    case "perfect" -> DMZClientConfig.perfect_bio_str;
                    default -> 1;
                };
                case "DEF" -> switch (form) {
                    case "base" -> getBaseStats(3);
                    case "semi_perfect" -> DMZClientConfig.semi_perfect_bio_def;
                    case "perfect" -> DMZClientConfig.perfect_bio_def;
                    default -> 1;
                };
                case "PWR" -> switch (form) {
                    case "base" -> getBaseStats(3);
                    case "semi_perfect" -> DMZClientConfig.semi_perfect_bio_pwr;
                    case "perfect" -> DMZClientConfig.perfect_bio_pwr;
                    default -> 1;
                };
                case "COST" -> switch (form) {
                    case "base" -> getBaseStats(3);
                    case "semi_perfect" -> DMZClientConfig.semi_perfect_bio_cost;
                    case "perfect" -> DMZClientConfig.perfect_bio_cost;
                    default -> 1;
                };
                default -> 1;
            };
            case 4 -> switch (stat.toUpperCase(Locale.ROOT)) {
                case "STR" -> switch (form) {
                    case "base" -> getBaseStats(4);
                    case "second_form" -> DMZClientConfig.second_form_cold_str;
                    case "third_form" -> DMZClientConfig.third_form_cold_str;
                    case "final_form" -> DMZClientConfig.final_form_cold_str;
                    case "full_power" -> DMZClientConfig.full_power_cold_str;
                    default -> 1;
                };
                case "DEF" -> switch (form) {
                    case "base" -> getBaseStats(4);
                    case "second_form" -> DMZClientConfig.second_form_cold_def;
                    case "third_form" -> DMZClientConfig.third_form_cold_def;
                    case "final_form" -> DMZClientConfig.final_form_cold_def;
                    case "full_power" -> DMZClientConfig.full_power_cold_def;
                    default -> 1;
                };
                case "PWR" -> switch (form) {
                    case "base" -> getBaseStats(4);
                    case "second_form" -> DMZClientConfig.second_form_cold_pwr;
                    case "third_form" -> DMZClientConfig.third_form_cold_pwr;
                    case "final_form" -> DMZClientConfig.final_form_cold_pwr;
                    case "full_power" -> DMZClientConfig.full_power_cold_pwr;
                    default -> 1;
                };
                case "COST" -> switch (form) {
                    case "base" -> getBaseStats(4);
                    case "second_form" -> DMZClientConfig.second_form_cold_cost;
                    case "third_form" -> DMZClientConfig.third_form_cold_cost;
                    case "final_form" -> DMZClientConfig.final_form_cold_cost;
                    case "full_power" -> DMZClientConfig.full_power_cold_cost;
                    default -> 1;
                };
                default -> 1;
            };
            case 5 -> switch (stat.toUpperCase(Locale.ROOT)) {
                case "STR" -> switch (form) {
                    case "base" -> getBaseStats(5);
                    case "evil" -> DMZClientConfig.evil_majin_str;
                    case "kid" -> DMZClientConfig.kid_majin_str;
                    case "super" -> DMZClientConfig.super_majin_str;
                    case "ultra" -> DMZClientConfig.ultra_majin_str;
                    default -> 1;
                };
                case "DEF" -> switch (form) {
                    case "base" -> getBaseStats(5);
                    case "evil" -> DMZClientConfig.evil_majin_def;
                    case "kid" -> DMZClientConfig.kid_majin_def;
                    case "super" -> DMZClientConfig.super_majin_def;
                    case "ultra" -> DMZClientConfig.ultra_majin_def;
                    default -> 1;
                };
                case "PWR" -> switch (form) {
                    case "base" -> getBaseStats(5);
                    case "evil" -> DMZClientConfig.evil_majin_pwr;
                    case "kid" -> DMZClientConfig.kid_majin_pwr;
                    case "super" -> DMZClientConfig.super_majin_pwr;
                    case "ultra" -> DMZClientConfig.ultra_majin_pwr;
                    default -> 1;
                };
                case "COST" -> switch (form) {
                    case "base" -> getBaseStats(5);
                    case "evil" -> DMZClientConfig.evil_majin_cost;
                    case "kid" -> DMZClientConfig.kid_majin_cost;
                    case "super" -> DMZClientConfig.super_majin_cost;
                    case "ultra" -> DMZClientConfig.ultra_majin_cost;
                    default -> 1;
                };
                default -> 1;
            };
            default -> 1;
        };
    }
    public static void setClassMult(int race, String clase, String stat, double value) {
        switch (clase) {
            case "warrior":
                switch (race) {
                    case 0 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_human_warrior = value;
                            case "DEF" -> DMZClientConfig.mult_def_human_warrior = value;
                            case "CON" -> DMZClientConfig.mult_con_human_warrior = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_human_warrior = value;
                            case "ENE" -> DMZClientConfig.mult_ene_human_warrior = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_human_warrior = value;
                        }
                    }
                    case 1 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_saiyan_warrior = value;
                            case "DEF" -> DMZClientConfig.mult_def_saiyan_warrior = value;
                            case "CON" -> DMZClientConfig.mult_con_saiyan_warrior = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_saiyan_warrior = value;
                            case "ENE" -> DMZClientConfig.mult_ene_saiyan_warrior = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_saiyan_warrior = value;
                        }
                    }
                    case 2 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_namek_warrior = value;
                            case "DEF" -> DMZClientConfig.mult_def_namek_warrior = value;
                            case "CON" -> DMZClientConfig.mult_con_namek_warrior = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_namek_warrior = value;
                            case "ENE" -> DMZClientConfig.mult_ene_namek_warrior = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_namek_warrior = value;
                        }
                    }
                    case 3 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_bio_warrior = value;
                            case "DEF" -> DMZClientConfig.mult_def_bio_warrior = value;
                            case "CON" -> DMZClientConfig.mult_con_bio_warrior = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_bio_warrior = value;
                            case "ENE" -> DMZClientConfig.mult_ene_bio_warrior = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_bio_warrior = value;
                        }
                    }
                    case 4 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_cold_warrior = value;
                            case "DEF" -> DMZClientConfig.mult_def_cold_warrior = value;
                            case "CON" -> DMZClientConfig.mult_con_cold_warrior = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_cold_warrior = value;
                            case "ENE" -> DMZClientConfig.mult_ene_cold_warrior = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_cold_warrior = value;
                        }
                    }
                    case 5 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_majin_warrior = value;
                            case "DEF" -> DMZClientConfig.mult_def_majin_warrior = value;
                            case "CON" -> DMZClientConfig.mult_con_majin_warrior = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_majin_warrior = value;
                            case "ENE" -> DMZClientConfig.mult_ene_majin_warrior = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_majin_warrior = value;
                        }
                    }
                }
            case "spiritualist":
                switch (race) {
                    case 0 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_human_spiritualist = value;
                            case "DEF" -> DMZClientConfig.mult_def_human_spiritualist = value;
                            case "CON" -> DMZClientConfig.mult_con_human_spiritualist = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_human_spiritualist = value;
                            case "ENE" -> DMZClientConfig.mult_ene_human_spiritualist = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_human_spiritualist = value;
                        }
                    }
                    case 1 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_saiyan_spiritualist = value;
                            case "DEF" -> DMZClientConfig.mult_def_saiyan_spiritualist = value;
                            case "CON" -> DMZClientConfig.mult_con_saiyan_spiritualist = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_saiyan_spiritualist = value;
                            case "ENE" -> DMZClientConfig.mult_ene_saiyan_spiritualist = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_saiyan_spiritualist = value;
                        }
                    }
                    case 2 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_namek_spiritualist = value;
                            case "DEF" -> DMZClientConfig.mult_def_namek_spiritualist = value;
                            case "CON" -> DMZClientConfig.mult_con_namek_spiritualist = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_namek_spiritualist = value;
                            case "ENE" -> DMZClientConfig.mult_ene_namek_spiritualist = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_namek_spiritualist = value;
                        }
                    }
                    case 3 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_bio_spiritualist = value;
                            case "DEF" -> DMZClientConfig.mult_def_bio_spiritualist = value;
                            case "CON" -> DMZClientConfig.mult_con_bio_spiritualist = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_bio_spiritualist = value;
                            case "ENE" -> DMZClientConfig.mult_ene_bio_spiritualist = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_bio_spiritualist = value;
                        }
                    }
                    case 4 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_cold_spiritualist = value;
                            case "DEF" -> DMZClientConfig.mult_def_cold_spiritualist = value;
                            case "CON" -> DMZClientConfig.mult_con_cold_spiritualist = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_cold_spiritualist = value;
                            case "ENE" -> DMZClientConfig.mult_ene_cold_spiritualist = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_cold_spiritualist = value;
                        }
                    }
                    case 5 -> {
                        switch (stat.toUpperCase(Locale.ROOT)) {
                            case "STR" -> DMZClientConfig.mult_str_majin_spiritualist = value;
                            case "DEF" -> DMZClientConfig.mult_def_majin_spiritualist = value;
                            case "CON" -> DMZClientConfig.mult_con_majin_spiritualist = value;
                            case "PWR" -> DMZClientConfig.mult_pwr_majin_spiritualist = value;
                            case "ENE" -> DMZClientConfig.mult_ene_majin_spiritualist = value;
                            case "REGEN" -> DMZClientConfig.mult_regen_majin_spiritualist = value;
                        }
                    }
                }
        }
    }
    public static double getClassMult(int race, String clase, String stat) {
        return switch (clase) {
            case "warrior" -> switch (race) {
                case 0 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_human_warrior;
                    case "DEF" -> DMZClientConfig.mult_def_human_warrior;
                    case "CON" -> DMZClientConfig.mult_con_human_warrior;
                    case "PWR" -> DMZClientConfig.mult_pwr_human_warrior;
                    case "ENE" -> DMZClientConfig.mult_ene_human_warrior;
                    case "REGEN" -> DMZClientConfig.mult_regen_human_warrior;
                    default -> 1;
                };
                case 1 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_saiyan_warrior;
                    case "DEF" -> DMZClientConfig.mult_def_saiyan_warrior;
                    case "CON" -> DMZClientConfig.mult_con_saiyan_warrior;
                    case "PWR" -> DMZClientConfig.mult_pwr_saiyan_warrior;
                    case "ENE" -> DMZClientConfig.mult_ene_saiyan_warrior;
                    case "REGEN" -> DMZClientConfig.mult_regen_saiyan_warrior;
                    default -> 1;
                };
                case 2 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_namek_warrior;
                    case "DEF" -> DMZClientConfig.mult_def_namek_warrior;
                    case "CON" -> DMZClientConfig.mult_con_namek_warrior;
                    case "PWR" -> DMZClientConfig.mult_pwr_namek_warrior;
                    case "ENE" -> DMZClientConfig.mult_ene_namek_warrior;
                    case "REGEN" -> DMZClientConfig.mult_regen_namek_warrior;
                    default -> 1;
                };
                case 3 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_bio_warrior;
                    case "DEF" -> DMZClientConfig.mult_def_bio_warrior;
                    case "CON" -> DMZClientConfig.mult_con_bio_warrior;
                    case "PWR" -> DMZClientConfig.mult_pwr_bio_warrior;
                    case "ENE" -> DMZClientConfig.mult_ene_bio_warrior;
                    case "REGEN" -> DMZClientConfig.mult_regen_bio_warrior;
                    default -> 1;
                };
                case 4 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_cold_warrior;
                    case "DEF" -> DMZClientConfig.mult_def_cold_warrior;
                    case "CON" -> DMZClientConfig.mult_con_cold_warrior;
                    case "PWR" -> DMZClientConfig.mult_pwr_cold_warrior;
                    case "ENE" -> DMZClientConfig.mult_ene_cold_warrior;
                    case "REGEN" -> DMZClientConfig.mult_regen_cold_warrior;
                    default -> 1;
                };
                case 5 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_majin_warrior;
                    case "DEF" -> DMZClientConfig.mult_def_majin_warrior;
                    case "CON" -> DMZClientConfig.mult_con_majin_warrior;
                    case "PWR" -> DMZClientConfig.mult_pwr_majin_warrior;
                    case "ENE" -> DMZClientConfig.mult_ene_majin_warrior;
                    case "REGEN" -> DMZClientConfig.mult_regen_majin_warrior;
                    default -> 1;
                };
                default -> 1;
            };
            case "spiritualist" -> switch (race) {
                case 0 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_human_spiritualist;
                    case "DEF" -> DMZClientConfig.mult_def_human_spiritualist;
                    case "CON" -> DMZClientConfig.mult_con_human_spiritualist;
                    case "PWR" -> DMZClientConfig.mult_pwr_human_spiritualist;
                    case "ENE" -> DMZClientConfig.mult_ene_human_spiritualist;
                    case "REGEN" -> DMZClientConfig.mult_regen_human_spiritualist;
                    default -> 1;
                };
                case 1 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_saiyan_spiritualist;
                    case "DEF" -> DMZClientConfig.mult_def_saiyan_spiritualist;
                    case "CON" -> DMZClientConfig.mult_con_saiyan_spiritualist;
                    case "PWR" -> DMZClientConfig.mult_pwr_saiyan_spiritualist;
                    case "ENE" -> DMZClientConfig.mult_ene_saiyan_spiritualist;
                    case "REGEN" -> DMZClientConfig.mult_regen_saiyan_spiritualist;
                    default -> 1;
                };
                case 2 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_namek_spiritualist;
                    case "DEF" -> DMZClientConfig.mult_def_namek_spiritualist;
                    case "CON" -> DMZClientConfig.mult_con_namek_spiritualist;
                    case "PWR" -> DMZClientConfig.mult_pwr_namek_spiritualist;
                    case "ENE" -> DMZClientConfig.mult_ene_namek_spiritualist;
                    case "REGEN" -> DMZClientConfig.mult_regen_namek_spiritualist;
                    default -> 1;
                };
                case 3 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_bio_spiritualist;
                    case "DEF" -> DMZClientConfig.mult_def_bio_spiritualist;
                    case "CON" -> DMZClientConfig.mult_con_bio_spiritualist;
                    case "PWR" -> DMZClientConfig.mult_pwr_bio_spiritualist;
                    case "ENE" -> DMZClientConfig.mult_ene_bio_spiritualist;
                    case "REGEN" -> DMZClientConfig.mult_regen_bio_spiritualist;
                    default -> 1;
                };
                case 4 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_cold_spiritualist;
                    case "DEF" -> DMZClientConfig.mult_def_cold_spiritualist;
                    case "CON" -> DMZClientConfig.mult_con_cold_spiritualist;
                    case "PWR" -> DMZClientConfig.mult_pwr_cold_spiritualist;
                    case "ENE" -> DMZClientConfig.mult_ene_cold_spiritualist;
                    case "REGEN" -> DMZClientConfig.mult_regen_cold_spiritualist;
                    default -> 1;
                };
                case 5 -> switch (stat.toUpperCase(Locale.ROOT)) {
                    case "STR" -> DMZClientConfig.mult_str_majin_spiritualist;
                    case "DEF" -> DMZClientConfig.mult_def_majin_spiritualist;
                    case "CON" -> DMZClientConfig.mult_con_majin_spiritualist;
                    case "PWR" -> DMZClientConfig.mult_pwr_majin_spiritualist;
                    case "ENE" -> DMZClientConfig.mult_ene_majin_spiritualist;
                    case "REGEN" -> DMZClientConfig.mult_regen_majin_spiritualist;
                    default -> 1;
                };
                default -> 1;
            };
            default -> 1;
        };
    }
    public static void setHumanPassive(int stat) {
        DMZClientConfig.humanPassive = stat;
    }
    public static int getHumanPassive() {
        return DMZClientConfig.humanPassive;
    }
    public static void setSaiyanPassive(String stat, int value) {
        switch (stat.toLowerCase(Locale.ROOT)) {
            case "timer" -> DMZClientConfig.zenkaiTimer = value;
            case "heal" -> DMZClientConfig.zenkaiHeal = value;
            case "boost" -> DMZClientConfig.zenkaiBoost = value;
            case "cant" -> DMZClientConfig.zenkaiCant = value;
        }
    }
    public static int getSaiyanPassive(String stat) {
        return switch (stat.toLowerCase(Locale.ROOT)) {
            case "timer" -> DMZClientConfig.zenkaiTimer;
            case "heal" -> DMZClientConfig.zenkaiHeal;
            case "boost" -> DMZClientConfig.zenkaiBoost;
            case "cant" -> DMZClientConfig.zenkaiCant;
            default -> 1;
        };
    }
    public static void setNamekPassive(int stat) {
        DMZClientConfig.namekPassive = stat;
    }
    public static int getNamekPassive() {
        return DMZClientConfig.namekPassive;
    }
    public static void setBioPassive(String stat, int value) {
        switch (stat.toLowerCase(Locale.ROOT)) {
            case "half" -> DMZClientConfig.bioPassiveHalf = value;
            case "quarter" -> DMZClientConfig.bioPassiveQuarter = value;
        }
    }
    public static int getBioPassive(String stat) {
        return switch (stat.toLowerCase(Locale.ROOT)) {
            case "half" -> DMZClientConfig.bioPassiveHalf;
            case "quarter" -> DMZClientConfig.bioPassiveQuarter;
            default -> 1;
        };
    }
    public static void setTrHumanMulti(String form, String stat, double value) {
        switch (form.toLowerCase(Locale.ROOT)){
            case "buffed":
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trhuman_buffed_str = value;
                    case "def" -> DMZClientConfig.mult_trhuman_buffed_def = value;
                    case "pwr" -> DMZClientConfig.mult_trhuman_buffed_pwr = value;
                }
                break;
            case "full_power":
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trhuman_full_power_str = value;
                    case "def" -> DMZClientConfig.mult_trhuman_full_power_def = value;
                    case "pwr" -> DMZClientConfig.mult_trhuman_full_power_pwr = value;
                }
                break;
            case "potential_unleashed":
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trhuman_potential_unleashed_str = value;
                    case "def" -> DMZClientConfig.mult_trhuman_potential_unleashed_def = value;
                    case "pwr" -> DMZClientConfig.mult_trhuman_potential_unleashed_pwr = value;
                }
                break;
            default:
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trhuman_base_str = value;
                    case "def" -> DMZClientConfig.mult_trhuman_base_def = value;
                    case "pwr" -> DMZClientConfig.mult_trhuman_base_pwr = value;
                }
                break;
        }
    }
    public static void setTrSaiyanMulti(String form, String stat, double value) {
        switch (form.toLowerCase(Locale.ROOT)) {
            case "oozaru" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_oozaru_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_oozaru_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_oozaru_pwr = value;
                }
            }
            case "ssj1" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_ssj1_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_ssj1_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_ssj1_pwr = value;
                }
            }
            case "ssgrade2" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_ssgrade2_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_ssgrade2_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_ssgrade2_pwr = value;
                }
            }
            case "ssgrade3" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_ssgrade3_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_ssgrade3_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_ssgrade3_pwr = value;
                }
            }
            case "mssj" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_mssj_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_mssj_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_mssj_pwr = value;
                }
            }
            case "ssj2" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_ssj2_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_ssj2_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_ssj2_pwr = value;
                }
            }
            case "ssj3" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_ssj3_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_ssj3_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_ssj3_pwr = value;
                }
            }
            case "golden_oozaru" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_golden_oozaru_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_golden_oozaru_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_golden_oozaru_pwr = value;
                }
            }
            default -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trsaiyan_base_str = value;
                    case "def" -> DMZClientConfig.mult_trsaiyan_base_def = value;
                    case "pwr" -> DMZClientConfig.mult_trsaiyan_base_pwr = value;
                }
            }
        }
    }
    public static void setTrNamekMulti(String form, String stat, double value) {
        switch (form.toLowerCase(Locale.ROOT)) {
            case "giant" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trnamek_giant_str = value;
                    case "def" -> DMZClientConfig.mult_trnamek_giant_def = value;
                    case "pwr" -> DMZClientConfig.mult_trnamek_giant_pwr = value;
                }
            }
            case "full_power" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trnamek_full_power_str = value;
                    case "def" -> DMZClientConfig.mult_trnamek_full_power_def = value;
                    case "pwr" -> DMZClientConfig.mult_trnamek_full_power_pwr = value;
                }
            }
            case "super_namek" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trnamek_super_namek_str = value;
                    case "def" -> DMZClientConfig.mult_trnamek_super_namek_def = value;
                    case "pwr" -> DMZClientConfig.mult_trnamek_super_namek_pwr = value;
                }
            }
            default -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trnamek_base_str = value;
                    case "def" -> DMZClientConfig.mult_trnamek_base_def = value;
                    case "pwr" -> DMZClientConfig.mult_trnamek_base_pwr = value;
                }
            }
        }
    }
    public static void setTrBioAndroidMulti(String form, String stat, double value) {
        switch (form.toLowerCase(Locale.ROOT)) {
            case "semi_perfect" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trbioandroid_semi_perfect_str = value;
                    case "def" -> DMZClientConfig.mult_trbioandroid_semi_perfect_def = value;
                    case "pwr" -> DMZClientConfig.mult_trbioandroid_semi_perfect_pwr = value;
                }
            }
            case "perfect" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trbioandroid_perfect_str = value;
                    case "def" -> DMZClientConfig.mult_trbioandroid_perfect_def = value;
                    case "pwr" -> DMZClientConfig.mult_trbioandroid_perfect_pwr = value;
                }
            }
            default -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trbioandroid_base_str = value;
                    case "def" -> DMZClientConfig.mult_trbioandroid_base_def = value;
                    case "pwr" -> DMZClientConfig.mult_trbioandroid_base_pwr = value;
                }
            }
        }
    }
    public static void setTrColdDemonMulti(String form, String stat, double value) {
        switch (form.toLowerCase(Locale.ROOT)) {
            case "second_form" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trcolddemon_second_form_str = value;
                    case "def" -> DMZClientConfig.mult_trcolddemon_second_form_def = value;
                    case "pwr" -> DMZClientConfig.mult_trcolddemon_second_form_pwr = value;
                }
            }
            case "third_form" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trcolddemon_third_form_str = value;
                    case "def" -> DMZClientConfig.mult_trcolddemon_third_form_def = value;
                    case "pwr" -> DMZClientConfig.mult_trcolddemon_third_form_pwr = value;
                }
            }
            case "final_form" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trcolddemon_final_form_str = value;
                    case "def" -> DMZClientConfig.mult_trcolddemon_final_form_def = value;
                    case "pwr" -> DMZClientConfig.mult_trcolddemon_final_form_pwr = value;
                }
            }
            case "full_power" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trcolddemon_full_power_str = value;
                    case "def" -> DMZClientConfig.mult_trcolddemon_full_power_def = value;
                    case "pwr" -> DMZClientConfig.mult_trcolddemon_full_power_pwr = value;
                }
            }
            default -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trcolddemon_base_str = value;
                    case "def" -> DMZClientConfig.mult_trcolddemon_base_def = value;
                    case "pwr" -> DMZClientConfig.mult_trcolddemon_base_pwr = value;
                }
            }
        }
    }
    public static void setTrMajinMulti(String form, String stat, double value) {
        switch (form.toLowerCase(Locale.ROOT)) {
            case "evil" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trmajin_evil_str = value;
                    case "def" -> DMZClientConfig.mult_trmajin_evil_def = value;
                    case "pwr" -> DMZClientConfig.mult_trmajin_evil_pwr = value;
                }
            }
            case "kid" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trmajin_kid_str = value;
                    case "def" -> DMZClientConfig.mult_trmajin_kid_def = value;
                    case "pwr" -> DMZClientConfig.mult_trmajin_kid_pwr = value;
                }
            }
            case "super" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trmajin_super_str = value;
                    case "def" -> DMZClientConfig.mult_trmajin_super_def = value;
                    case "pwr" -> DMZClientConfig.mult_trmajin_super_pwr = value;
                }
            }
            case "ultra" -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trmajin_ultra_str = value;
                    case "def" -> DMZClientConfig.mult_trmajin_ultra_def = value;
                    case "pwr" -> DMZClientConfig.mult_trmajin_ultra_pwr = value;
                }
            }
            default -> {
                switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trmajin_base_str = value;
                    case "def" -> DMZClientConfig.mult_trmajin_base_def = value;
                    case "pwr" -> DMZClientConfig.mult_trmajin_base_pwr = value;
                }
            }
        }
    }

    public static double getTrHumanMulti(String form, String stat) {
        switch (form.toLowerCase(Locale.ROOT)) {
            case "buffed" -> {
                return switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trhuman_buffed_str;
                    case "def" -> DMZClientConfig.mult_trhuman_buffed_def;
                    case "pwr" -> DMZClientConfig.mult_trhuman_buffed_pwr;
                    default -> 1.0;
                };
            }
            case "full_power" -> {
                return switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trhuman_full_power_str;
                    case "def" -> DMZClientConfig.mult_trhuman_full_power_def;
                    case "pwr" -> DMZClientConfig.mult_trhuman_full_power_pwr;
                    default -> 1.0;
                };
            }
            case "potential_unleashed" -> {
                return switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trhuman_potential_unleashed_str;
                    case "def" -> DMZClientConfig.mult_trhuman_potential_unleashed_def;
                    case "pwr" -> DMZClientConfig.mult_trhuman_potential_unleashed_pwr;
                    default -> 1.0;
                };
            }
            default -> {
                return switch (stat.toLowerCase(Locale.ROOT)) {
                    case "str" -> DMZClientConfig.mult_trhuman_base_str;
                    case "def" -> DMZClientConfig.mult_trhuman_base_def;
                    case "pwr" -> DMZClientConfig.mult_trhuman_base_pwr;
                    default -> 1.0;
                };
            }
        }
    }

    public static double transfMultMenu(DMZStatsAttributes stats, String transformation) {
        double str = getTransformationStats(stats.getIntValue("race"), transformation, "STR");
        double def = getTransformationStats(stats.getIntValue("race"), transformation, "DEF");
        double pwr = getTransformationStats(stats.getIntValue("race"), transformation, "PWR");

        return (str + def + pwr) / 3;
    }

    public static double getTransformationStats(int raza, String transformation, String stat) {
        return switch (stat) {
            case "STR" -> switch (raza) {
                case 0 -> switch (transformation) { // Humanos
                    case "buffed" -> mult_trhuman_buffed_str;
                    case "full_power" -> mult_trhuman_full_power_str;
                    case "potential_unleashed" -> mult_trhuman_potential_unleashed_str;
                    default -> mult_trhuman_base_str;
                };
                case 1 -> switch (transformation) { // Saiyans
                    case "oozaru" -> mult_trsaiyan_oozaru_str;
                    case "ssj1" -> mult_trsaiyan_ssj1_str;
                    case "ssgrade2" -> mult_trsaiyan_ssgrade2_str;
                    case "ssgrade3" -> mult_trsaiyan_ssgrade3_str;
                    case "mssj" -> mult_trsaiyan_mssj_str;
                    case "ssj2" -> mult_trsaiyan_ssj2_str;
                    case "ssj3" -> mult_trsaiyan_ssj3_str;
                    case "golden_oozaru" -> mult_trsaiyan_golden_oozaru_str;
                    default -> mult_trsaiyan_base_str;
                };
                case 2 -> switch (transformation) { // Namekianos
                    case "giant" -> mult_trnamek_giant_str;
                    case "full_power" -> mult_trnamek_full_power_str;
                    case "super_namek" -> mult_trnamek_super_namek_str;
                    default -> mult_trnamek_base_str;
                };
                case 3 -> switch (transformation) { // Bioandroides
                    case "semi_perfect" -> mult_trbioandroid_semi_perfect_str;
                    case "perfect" -> mult_trbioandroid_perfect_str;
                    default -> mult_trbioandroid_base_str;
                };
                case 4 -> switch (transformation) { // Cold Demons
                    case "second_form" -> mult_trcolddemon_second_form_str;
                    case "third_form" -> mult_trcolddemon_third_form_str;
                    case "final_form" -> mult_trcolddemon_final_form_str;
                    case "full_power" -> mult_trcolddemon_full_power_str;
                    default -> mult_trcolddemon_base_str;
                };
                case 5 -> switch (transformation) { // Majin
                    case "evil" -> mult_trmajin_evil_str;
                    case "kid" -> mult_trmajin_kid_str;
                    case "super" -> mult_trmajin_super_str;
                    case "ultra" -> mult_trmajin_ultra_str;
                    default -> mult_trmajin_base_str;
                };
                default -> 1.0;
            };
            case "DEF" -> switch (raza) {
                case 0 -> switch (transformation) { // Humanos
                    case "buffed" -> mult_trhuman_buffed_def;
                    case "full_power" -> mult_trhuman_full_power_def;
                    case "potential_unleashed" -> mult_trhuman_potential_unleashed_def;
                    default -> mult_trhuman_base_def;
                };
                case 1 -> switch (transformation) { // Saiyans
                    case "oozaru" -> mult_trsaiyan_oozaru_def;
                    case "ssj1" -> mult_trsaiyan_ssj1_def;
                    case "ssgrade2" -> mult_trsaiyan_ssgrade2_def;
                    case "ssgrade3" -> mult_trsaiyan_ssgrade3_def;
                    case "mssj" -> mult_trsaiyan_mssj_def;
                    case "ssj2" -> mult_trsaiyan_ssj2_def;
                    case "ssj3" -> mult_trsaiyan_ssj3_def;
                    case "golden_oozaru" -> mult_trsaiyan_golden_oozaru_def;
                    default -> mult_trsaiyan_base_def;
                };
                case 2 -> switch (transformation) { // Namekianos
                    case "giant" -> mult_trnamek_giant_def;
                    case "full_power" -> mult_trnamek_full_power_def;
                    case "super_namek" -> mult_trnamek_super_namek_def;
                    default -> mult_trnamek_base_def;
                };
                case 3 -> switch (transformation) { // Bioandroides
                    case "semi_perfect" -> mult_trbioandroid_semi_perfect_def;
                    case "perfect" -> mult_trbioandroid_perfect_def;
                    default -> mult_trbioandroid_base_def;
                };
                case 4 -> switch (transformation) { // Cold Demons
                    case "second_form" -> mult_trcolddemon_second_form_def;
                    case "third_form" -> mult_trcolddemon_third_form_def;
                    case "final_form" -> mult_trcolddemon_final_form_def;
                    case "full_power" -> mult_trcolddemon_full_power_def;
                    default -> mult_trcolddemon_base_def;
                };
                case 5 -> switch (transformation) { // Majin
                    case "evil" -> mult_trmajin_evil_def;
                    case "kid" -> mult_trmajin_kid_def;
                    case "super" -> mult_trmajin_super_def;
                    case "ultra" -> mult_trmajin_ultra_def;
                    default -> mult_trmajin_base_def;
                };
                default -> 1.0;
            };
            case "PWR" -> switch (raza) {
                case 0 -> switch (transformation) { // Humanos
                    case "buffed" -> mult_trhuman_buffed_pwr;
                    case "full_power" -> mult_trhuman_full_power_pwr;
                    case "potential_unleashed" -> mult_trhuman_potential_unleashed_pwr;
                    default -> mult_trhuman_base_pwr;
                };
                case 1 -> switch (transformation) { // Saiyans
                    case "oozaru" -> mult_trsaiyan_oozaru_pwr;
                    case "ssj1" -> mult_trsaiyan_ssj1_pwr;
                    case "ssgrade2" -> mult_trsaiyan_ssgrade2_pwr;
                    case "ssgrade3" -> mult_trsaiyan_ssgrade3_pwr;
                    case "mssj" -> mult_trsaiyan_mssj_pwr;
                    case "ssj2" -> mult_trsaiyan_ssj2_pwr;
                    case "ssj3" -> mult_trsaiyan_ssj3_pwr;
                    case "golden_oozaru" -> mult_trsaiyan_golden_oozaru_pwr;
                    default -> mult_trsaiyan_base_pwr;
                };
                case 2 -> switch (transformation) { // Namekianos
                    case "giant" -> mult_trnamek_giant_pwr;
                    case "full_power" -> mult_trnamek_full_power_pwr;
                    case "super_namek" -> mult_trnamek_super_namek_pwr;
                    default -> mult_trnamek_base_pwr;
                };
                case 3 -> switch (transformation) { // Bioandroides
                    case "semi_perfect" -> mult_trbioandroid_semi_perfect_pwr;
                    case "perfect" -> mult_trbioandroid_perfect_pwr;
                    default -> mult_trbioandroid_base_pwr;
                };
                case 4 -> switch (transformation) { // Cold Demons
                    case "second_form" -> mult_trcolddemon_second_form_pwr;
                    case "third_form" -> mult_trcolddemon_third_form_pwr;
                    case "final_form" -> mult_trcolddemon_final_form_pwr;
                    case "full_power" -> mult_trcolddemon_full_power_pwr;
                    default -> mult_trcolddemon_base_pwr;
                };
                case 5 -> switch (transformation) { // Majin
                    case "evil" -> mult_trmajin_evil_pwr;
                    case "kid" -> mult_trmajin_kid_pwr;
                    case "super" -> mult_trmajin_super_pwr;
                    case "ultra" -> mult_trmajin_ultra_pwr;
                    default -> mult_trmajin_base_pwr;
                };
                default -> 1.0;
            };
            default -> 1.0;
        };
    }

    public static void setColdPassive(double value) {
        DMZClientConfig.coldPassive = value;
    }

    public static double getColdPassive() {
        return DMZClientConfig.coldPassive;
    }

    public static void setMajinPassive(double value) {
        DMZClientConfig.majinPassive = value;
    }

    public static double getMajinPassive() {
        return DMZClientConfig.majinPassive;
    }

    public static int getJumpMaster() {
        return jumpMaster;
    }

    public static void setJumpMaster(int jumpMaster) {
        DMZClientConfig.jumpMaster = jumpMaster;
    }

    public static int getFlyMaster() {
        return flyMaster;
    }

    public static void setFlyMaster(int flyMaster) {
        DMZClientConfig.flyMaster = flyMaster;
    }

    public static int getMeditationMaster() {
        return meditationMaster;
    }

    public static void setMeditationMaster(int meditationMaster) {
        DMZClientConfig.meditationMaster = meditationMaster;
    }

    public static int getPotUnlockMaster() {
        return potUnlockMaster;
    }

    public static void setPotUnlockMaster(int potUnlockMaster) {
        DMZClientConfig.potUnlockMaster = potUnlockMaster;
    }

    public static int getKiManipMaster() {
        return kiManipMaster;
    }

    public static void setKiManipMaster(int kiManipMaster) {
        DMZClientConfig.kiManipMaster = kiManipMaster;
    }

    public static int getKiControlMaster() {
        return kiControlMaster;
    }

    public static void setKiControlMaster(int kiControlMaster) {
        DMZClientConfig.kiControlMaster = kiControlMaster;
    }
}