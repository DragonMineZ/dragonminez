package com.yuseix.dragonminez.utils;

import com.yuseix.dragonminez.config.DMZGeneralConfig;
import com.yuseix.dragonminez.config.races.*;
import com.yuseix.dragonminez.config.races.transformations.*;
import com.yuseix.dragonminez.stats.DMZStatsAttributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class DMZDatos implements IDMZDatos{

    @Override
    public int calcStrength(DMZStatsAttributes stats) {
        double multRaza = getRaceStats(stats.getIntValue("race"), stats.getStringValue("class"), "STR");
        double multTransf = getTransformationStats(stats.getIntValue("race"), stats.getStringValue("form"), "STR");

        // Fórmula = ((((1 + (StatSTR / 10)) * ConfigRaza) * (Transf * Efectos)) * (Porcentaje / 10))
        return (int) Math.ceil((((1 + ((double) stats.getStat("STR") / 10)) * multRaza) * (multTransf * getEffectsMult(stats))) * ((double)stats.getIntValue("release")/10));
    }

    @Override
    public int calcDefense(DMZStatsAttributes stats, Player player) {
        int DefensaArmor = player.getArmorValue(); int DurezaArmor = Mth.floor(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        int armorTotal = (DefensaArmor + DurezaArmor) * 3;

        double multRaza = getRaceStats(stats.getIntValue("race"), stats.getStringValue("class"), "DEF");
        double multTransf = getTransformationStats(stats.getIntValue("race"), stats.getStringValue("form"), "DEF");

        // Fórmula = (((((StatDEF * ConfigRaza) * (Transf * Efectos)) * Porcentaje)) / 5) + ((DefensaArmor + DurezaArmor) * 3)
        return (int) Math.ceil((((((double) stats.getStat("DEF") / 3) * multRaza) * (multTransf * getEffectsMult(stats))) * ((double)stats.getIntValue("release")/10)) / 5)  + armorTotal;
    }

    @Override
    public int calcConstitution(DMZStatsAttributes stats) {
        double multRaza = getRaceStats(stats.getIntValue("race"), stats.getStringValue("class"), "CON");

        // Fórmula = Math.round(20 + (1.2 * (StatCON * ConfigRaza)))
        return (int) Math.round(20 + (1.2 * (stats.getStat("CON") * multRaza) * 8.0));
    }

    @Override
    public int calcStamina(DMZStatsAttributes stats) {
        double multRaza = getRaceStats(stats.getIntValue("race"), stats.getStringValue("class"), "STM");

        // Fórmula = Math.round((MaxCON * 0.85) * multRaza)
        return (int) Math.round((stats.getIntValue("maxhealth") * 0.20) * multRaza);
    }

    @Override
    public int calcKiPower(DMZStatsAttributes stats) {
        double multRaza = getRaceStats(stats.getIntValue("race"), stats.getStringValue("class"), "PWR");
        double multTransf = getTransformationStats(stats.getIntValue("race"), stats.getStringValue("form"), "PWR");

        // Fórmula = Math.ceil((((StatPWR / 5) * ConfigRaza * (Transf * Efectos))/3) * (Porcentaje / 10))
        return (int) Math.ceil(((((double) stats.getStat("PWR") / 2.5) * multRaza * (multTransf * getEffectsMult(stats)))/3) * ((double)stats.getIntValue("release")/10));
    }

    @Override
    public int calcEnergy(DMZStatsAttributes stats) {
        double multRaza = getRaceStats(stats.getIntValue("race"), stats.getStringValue("class"), "ENE");

        // Fórmula = Math.round(3 * StatENE * ConfigRaza + 40 * 3)
        return (int) Math.round(4.5 * stats.getStat("ENE") * multRaza + 40 * 40);
    }

    @Override
    public int calcKiConsume(DMZStatsAttributes stats) {
		// Fórmula = No hay, es consumo directo. Si en la config SSJ_KI_COST.get() = 50, se consume 50 de ki por segundo.
        return (int) getTransformationStats(stats.getIntValue("race"), stats.getStringValue("form"), "COST");
    }

    @Override
    public int calcKiRegen(DMZStatsAttributes stats) {
        double multRaza = getRaceStats(stats.getIntValue("race"), stats.getStringValue("class"), "REGEN");

        // Fórmula = Math.ceil(EnergiaTotal * ConfigRaza)
        return (int) Math.ceil(stats.getIntValue("maxenergy") * multRaza);
    }

    @Override
    public double calcTotalMultiplier(DMZStatsAttributes stats) {
        double multiSTR = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "STR");
        double multiDEF = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "DEF");
        double multiPWR = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "PWR");

        // Promedio, pq si se tiene x1 STR, x1 DEF y x1 PWR, debería ser x1 en Total y no x3
        return ((multiSTR + multiDEF + multiPWR) / 3) * getEffectsMult(stats);
    }

    @Override
    public double calcStatMultiplier(DMZStatsAttributes stats, String stat) {
        double multiTransf = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), stat);

        return multiTransf * getEffectsMult(stats);
    }

    public double calcularMultiTransf(DMZStatsAttributes stats) {
        double multiStr = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "STR");
        double multiDef = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "DEF");
        double multiKiPower = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "PWR");

        return (multiStr + multiDef + multiKiPower) / 3;
    }

    @Override
    public int calcMultipliedStrength(DMZStatsAttributes stats) {
        double multForma = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "STR");

        // Fórmula = (statStr * (DMZTrHumanConfig.MULTIPLIER_BASE.get() * getEffectsMult(stats)))
        return (int) (stats.getStat("STR") * multForma * getEffectsMult(stats));
    }

    @Override
    public int calcMultipliedDefense(DMZStatsAttributes stats) {
        double multForma = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "DEF");

        // Fórmula = (statDef * (DMZTrHumanConfig.MULTIPLIER_BASE.get() * getEffectsMult(stats)))
        return (int) (stats.getStat("DEF") * multForma * getEffectsMult(stats));
    }

    @Override
    public int calcMultipliedKiPower(DMZStatsAttributes stats) {
        double multForma = DMZClientConfig.getDMZStat(stats.getIntValue("race"), stats.getStringValue("form"), "PWR");

        // Fórmula = (statPwr * (DMZTrHumanConfig.MULTIPLIER_BASE.get() * getEffectsMult(stats)))
        return (int) (stats.getStat("PWR") * multForma * getEffectsMult(stats));
    }

    @Override
    public int calcKiCharge(DMZStatsAttributes stats) {
		return switch (stats.getStringValue("class")) {
			case "warrior" -> (int) Math.ceil((stats.getIntValue("maxenergy") * 0.02));
			case "spiritualist" -> (int) Math.ceil((stats.getIntValue("maxenergy") * 0.03));
			default -> 0;
		};
    }
    
    public double getEffectsMult(DMZStatsAttributes stats) {
        boolean majinOn = stats.hasDMZPermaEffect("majin"); boolean mightfruit = stats.hasDMZTemporalEffect("mightfruit");
        double majinDato = majinOn ? DMZGeneralConfig.MULTIPLIER_MAJIN.get() : 1; // 1 si no está activo para que no afecte
        double frutaDato = mightfruit ? DMZGeneralConfig.MULTIPLIER_TREE_MIGHT.get() : 1;
        double totalEffects = majinDato * frutaDato; // Agregar más efectos acá luego
        
        return totalEffects;
    }

    public double getRaceStats(int raza, String clase, String stat) {
        return switch (stat) {
            case "STR" -> switch (raza) {
                case 0 -> clase.equals("warrior") ? DMZHumanConfig.MULTIPLIER_STR_WARRIOR.get()
                        : DMZHumanConfig.MULTIPLIER_STR_SPIRITUALIST.get();
                case 1 -> clase.equals("warrior") ? DMZSaiyanConfig.MULTIPLIER_STR_WARRIOR.get()
                        : DMZSaiyanConfig.MULTIPLIER_STR_SPIRITUALIST.get();
                case 2 -> clase.equals("warrior") ? DMZNamekConfig.MULTIPLIER_STR_WARRIOR.get()
                        : DMZNamekConfig.MULTIPLIER_STR_SPIRITUALIST.get();
                case 3 -> clase.equals("warrior") ? DMZBioAndroidConfig.MULTIPLIER_STR_WARRIOR.get()
                        : DMZBioAndroidConfig.MULTIPLIER_STR_SPIRITUALIST.get();
                case 4 -> clase.equals("warrior") ? DMZColdDemonConfig.MULTIPLIER_STR_WARRIOR.get()
                        : DMZColdDemonConfig.MULTIPLIER_STR_SPIRITUALIST.get();
                case 5 -> clase.equals("warrior") ? DMZMajinConfig.MULTIPLIER_STR_WARRIOR.get()
                        : DMZMajinConfig.MULTIPLIER_STR_SPIRITUALIST.get();
                default -> 1.0;
            };
            case "DEF" -> switch (raza) {
                case 0 -> clase.equals("warrior") ? DMZHumanConfig.MULTIPLIER_DEF_WARRIOR.get()
                        : DMZHumanConfig.MULTIPLIER_DEF_SPIRITUALIST.get();
                case 1 -> clase.equals("warrior") ? DMZSaiyanConfig.MULTIPLIER_DEF_WARRIOR.get()
                        : DMZSaiyanConfig.MULTIPLIER_DEF_SPIRITUALIST.get();
                case 2 -> clase.equals("warrior") ? DMZNamekConfig.MULTIPLIER_DEF_WARRIOR.get()
                        : DMZNamekConfig.MULTIPLIER_DEF_SPIRITUALIST.get();
                case 3 -> clase.equals("warrior") ? DMZBioAndroidConfig.MULTIPLIER_DEF_WARRIOR.get()
                        : DMZBioAndroidConfig.MULTIPLIER_DEF_SPIRITUALIST.get();
                case 4 -> clase.equals("warrior") ? DMZColdDemonConfig.MULTIPLIER_DEF_WARRIOR.get()
                        : DMZColdDemonConfig.MULTIPLIER_DEF_SPIRITUALIST.get();
                case 5 -> clase.equals("warrior") ? DMZMajinConfig.MULTIPLIER_DEF_WARRIOR.get()
                        : DMZMajinConfig.MULTIPLIER_DEF_SPIRITUALIST.get();
                default -> 1.0;
            };
            case "CON" -> switch (raza) {
                case 0 -> clase.equals("warrior") ? DMZHumanConfig.MULTIPLIER_CON_WARRIOR.get()
                        : DMZHumanConfig.MULTIPLIER_CON_SPIRITUALIST.get();
                case 1 -> clase.equals("warrior") ? DMZSaiyanConfig.MULTIPLIER_CON_WARRIOR.get()
                        : DMZSaiyanConfig.MULTIPLIER_CON_SPIRITUALIST.get();
                case 2 -> clase.equals("warrior") ? DMZNamekConfig.MULTIPLIER_CON_WARRIOR.get()
                        : DMZNamekConfig.MULTIPLIER_CON_SPIRITUALIST.get();
                case 3 -> clase.equals("warrior") ? DMZBioAndroidConfig.MULTIPLIER_CON_WARRIOR.get()
                        : DMZBioAndroidConfig.MULTIPLIER_CON_SPIRITUALIST.get();
                case 4 -> clase.equals("warrior") ? DMZColdDemonConfig.MULTIPLIER_CON_WARRIOR.get()
                        : DMZColdDemonConfig.MULTIPLIER_CON_SPIRITUALIST.get();
                case 5 -> clase.equals("warrior") ? DMZMajinConfig.MULTIPLIER_CON_WARRIOR.get()
                        : DMZMajinConfig.MULTIPLIER_CON_SPIRITUALIST.get();
                default -> 1.0;
            };
            case "PWR" -> switch (raza) {
                case 0 -> clase.equals("warrior") ? DMZHumanConfig.MULTIPLIER_KIPOWER_WARRIOR.get()
                        : DMZHumanConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get();
                case 1 -> clase.equals("warrior") ? DMZSaiyanConfig.MULTIPLIER_KIPOWER_WARRIOR.get()
                        : DMZSaiyanConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get();
                case 2 -> clase.equals("warrior") ? DMZNamekConfig.MULTIPLIER_KIPOWER_WARRIOR.get()
                        : DMZNamekConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get();
                case 3 -> clase.equals("warrior") ? DMZBioAndroidConfig.MULTIPLIER_KIPOWER_WARRIOR.get()
                        : DMZBioAndroidConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get();
                case 4 -> clase.equals("warrior") ? DMZColdDemonConfig.MULTIPLIER_KIPOWER_WARRIOR.get()
                        : DMZColdDemonConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get();
                case 5 -> clase.equals("warrior") ? DMZMajinConfig.MULTIPLIER_KIPOWER_WARRIOR.get()
                        : DMZMajinConfig.MULTIPLIER_KIPOWER_SPIRITUALIST.get();
                default -> 1.0;
            };
            case "ENE" -> switch (raza) {
              case 0 -> clase.equals("warrior") ? DMZHumanConfig.MULTIPLIER_ENERGY_WARRIOR.get()
                      : DMZHumanConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get();
              case 1 -> clase.equals("warrior") ? DMZSaiyanConfig.MULTIPLIER_ENERGY_WARRIOR.get()
                      : DMZSaiyanConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get();
              case 2 -> clase.equals("warrior") ? DMZNamekConfig.MULTIPLIER_ENERGY_WARRIOR.get()
                      : DMZNamekConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get();
              case 3 -> clase.equals("warrior") ? DMZBioAndroidConfig.MULTIPLIER_ENERGY_WARRIOR.get()
                      : DMZBioAndroidConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get();
              case 4 -> clase.equals("warrior") ? DMZColdDemonConfig.MULTIPLIER_ENERGY_WARRIOR.get()
                      : DMZColdDemonConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get();
              case 5 -> clase.equals("warrior") ? DMZMajinConfig.MULTIPLIER_ENERGY_WARRIOR.get()
                      : DMZMajinConfig.MULTIPLIER_ENERGY_SPIRITUALIST.get();
              default -> 1.0;
            };
            case "REGEN" -> switch (raza) {
                case 0 -> clase.equals("warrior") ? DMZHumanConfig.KI_REGEN_WARRIOR.get()
                        : DMZHumanConfig.KI_REGEN_SPIRITUALIST.get();
                case 1 -> clase.equals("warrior") ? DMZSaiyanConfig.KI_REGEN_WARRIOR.get()
                        : DMZSaiyanConfig.KI_REGEN_SPIRITUALIST.get();
                case 2 -> clase.equals("warrior") ? DMZNamekConfig.KI_REGEN_WARRIOR.get()
                        : DMZNamekConfig.KI_REGEN_SPIRITUALIST.get();
                case 3 -> clase.equals("warrior") ? DMZBioAndroidConfig.KI_REGEN_WARRIOR.get()
                        : DMZBioAndroidConfig.KI_REGEN_SPIRITUALIST.get();
                case 4 -> clase.equals("warrior") ? DMZColdDemonConfig.KI_REGEN_WARRIOR.get()
                        : DMZColdDemonConfig.KI_REGEN_SPIRITUALIST.get();
                case 5 -> clase.equals("warrior") ? DMZMajinConfig.KI_REGEN_WARRIOR.get()
                        : DMZMajinConfig.KI_REGEN_SPIRITUALIST.get();
                default -> 1.0;
            };
            default -> 1.0;
        };
    }

    public double getTransformationStats(int raza, String transformation, String stat) {
        return switch (stat) {
            case "STR" -> switch (raza) {
                case 0 -> switch (transformation) { // Humanos
                    case "buffed" -> DMZTrHumanConfig.MULTIPLIER_BUFFED_FORM_STR.get();
                    case "full_power" -> DMZTrHumanConfig.MULTIPLIER_FP_FORM_STR.get();
                    case "potential_unleashed" -> DMZTrHumanConfig.MULTIPLIER_PU_FORM_STR.get();
                    default -> DMZTrHumanConfig.MULTIPLIER_BASE.get();
                };
                case 1 -> switch (transformation) { // Saiyans
                    case "oozaru" -> DMZTrSaiyanConfig.MULTIPLIER_OOZARU_FORM_STR.get();
                    case "ssj1" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ_FORM_STR.get();
                    case "ssgrade2" -> DMZTrSaiyanConfig.MULTIPLIER_SSGRADE2_FORM_STR.get();
                    case "ssgrade3" -> DMZTrSaiyanConfig.MULTIPLIER_SSGRADE3_FORM_STR.get();
                    case "mssj" -> DMZTrSaiyanConfig.MULTIPLIER_MSSJ_FORM_STR.get();
                    case "ssj2" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ2_FORM_STR.get();
                    case "ssj3" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ3_FORM_STR.get();
                    case "golden_oozaru" -> DMZTrSaiyanConfig.MULTIPLIER_GOLDENOOZARU_FORM_STR.get();
                    default -> DMZTrSaiyanConfig.MULTIPLIER_BASE.get();
                };
                case 2 -> switch (transformation) { // Namekianos
                    case "giant" -> DMZTrNamekConfig.MULTIPLIER_GIANT_FORM_STR.get();
                    case "full_power" -> DMZTrNamekConfig.MULTIPLIER_FP_FORM_STR.get();
                    case "super_namek" -> DMZTrNamekConfig.MULTIPLIER_SUPER_NAMEK_FORM_STR.get();
                    default -> DMZTrNamekConfig.MULTIPLIER_BASE.get();
                };
                case 3 -> switch (transformation) { // BioAndroides
                    case "semi_perfect" -> DMZTrBioAndroidConfig.MULTIPLIER_SEMIPERFECT_FORM_STR.get();
                    case "perfect" -> DMZTrBioAndroidConfig.MULTIPLIER_PERFECT_FORM_STR.get();
                    default -> DMZTrBioAndroidConfig.MULTIPLIER_BASE.get();
                };
                case 4 -> switch (transformation) { // Cold Demons
                    case "second_form" -> DMZTrColdDemonConfig.MULTIPLIER_SECOND_FORM_STR.get();
                    case "third_form" -> DMZTrColdDemonConfig.MULTIPLIER_THIRD_FORM_STR.get();
                    case "final_form" -> DMZTrColdDemonConfig.MULTIPLIER_FOURTH_FORM_STR.get();
                    case "full_power" -> DMZTrColdDemonConfig.MULTIPLIER_FULL_POWER_FORM_STR.get();
                    default -> DMZTrColdDemonConfig.MULTIPLIER_BASE.get();
                };
                case 5 -> switch (transformation) { // Majin
                    case "evil" -> DMZTrMajinConfig.MULTIPLIER_EVIL_FORM_STR.get();
                    case "kid" -> DMZTrMajinConfig.MULTIPLIER_KID_FORM_STR.get();
                    case "super" -> DMZTrMajinConfig.MULTIPLIER_SUPER_FORM_STR.get();
                    case "ultra" -> DMZTrMajinConfig.MULTIPLIER_ULTRA_FORM_STR.get();
                    default -> DMZTrMajinConfig.MULTIPLIER_BASE.get();
                };
                default -> 1.0;
            };
            case "DEF" -> switch (raza) {
                case 0 -> switch (transformation) { // Humanos
                    case "buffed" -> DMZTrHumanConfig.MULTIPLIER_BUFFED_FORM_DEF.get();
                    case "full_power" -> DMZTrHumanConfig.MULTIPLIER_FP_FORM_DEF.get();
                    case "potential_unleashed" -> DMZTrHumanConfig.MULTIPLIER_PU_FORM_DEF.get();
                    default -> DMZTrHumanConfig.MULTIPLIER_BASE.get();
                };
                case 1 -> switch (transformation) { // Saiyans
                    case "oozaru" -> DMZTrSaiyanConfig.MULTIPLIER_OOZARU_FORM_DEF.get();
                    case "ssj1" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ_FORM_DEF.get();
                    case "ssgrade2" -> DMZTrSaiyanConfig.MULTIPLIER_SSGRADE2_FORM_DEF.get();
                    case "ssgrade3" -> DMZTrSaiyanConfig.MULTIPLIER_SSGRADE3_FORM_DEF.get();
                    case "mssj" -> DMZTrSaiyanConfig.MULTIPLIER_MSSJ_FORM_DEF.get();
                    case "ssj2" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ2_FORM_DEF.get();
                    case "ssj3" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ3_FORM_DEF.get();
                    case "golden_oozaru" -> DMZTrSaiyanConfig.MULTIPLIER_GOLDENOOZARU_FORM_DEF.get();
                    default -> DMZTrSaiyanConfig.MULTIPLIER_BASE.get();
                };
                case 2 -> switch (transformation) { // Namekianos
                    case "giant" -> DMZTrNamekConfig.MULTIPLIER_GIANT_FORM_DEF.get();
                    case "full_power" -> DMZTrNamekConfig.MULTIPLIER_FP_FORM_DEF.get();
                    case "super_namek" -> DMZTrNamekConfig.MULTIPLIER_SUPER_NAMEK_FORM_DEF.get();
                    default -> DMZTrNamekConfig.MULTIPLIER_BASE.get();
                };
                case 3 -> switch (transformation) { // BioAndroides
                    case "semi_perfect" -> DMZTrBioAndroidConfig.MULTIPLIER_SEMIPERFECT_FORM_DEF.get();
                    case "perfect" -> DMZTrBioAndroidConfig.MULTIPLIER_PERFECT_FORM_DEF.get();
                    default -> DMZTrBioAndroidConfig.MULTIPLIER_BASE.get();
                };
                case 4 -> switch (transformation) { // Cold Demons
                    case "second_form" -> DMZTrColdDemonConfig.MULTIPLIER_SECOND_FORM_DEF.get();
                    case "third_form" -> DMZTrColdDemonConfig.MULTIPLIER_THIRD_FORM_DEF.get();
                    case "final_form" -> DMZTrColdDemonConfig.MULTIPLIER_FOURTH_FORM_DEF.get();
                    case "full_power" -> DMZTrColdDemonConfig.MULTIPLIER_FULL_POWER_FORM_DEF.get();
                    default -> DMZTrColdDemonConfig.MULTIPLIER_BASE.get();
                };
                case 5 -> switch (transformation) { // Majin
                    case "evil" -> DMZTrMajinConfig.MULTIPLIER_EVIL_FORM_DEF.get();
                    case "kid" -> DMZTrMajinConfig.MULTIPLIER_KID_FORM_DEF.get();
                    case "super" -> DMZTrMajinConfig.MULTIPLIER_SUPER_FORM_DEF.get();
                    case "ultra" -> DMZTrMajinConfig.MULTIPLIER_ULTRA_FORM_DEF.get();
                    default -> DMZTrMajinConfig.MULTIPLIER_BASE.get();
                };
                default -> 1.0;
            };
            case "PWR" -> switch (raza) {
                case 0 -> switch (transformation) { // Humanos
                    case "buffed" -> DMZTrHumanConfig.MULTIPLIER_BUFFED_FORM_PWR.get();
                    case "full_power" -> DMZTrHumanConfig.MULTIPLIER_FP_FORM_PWR.get();
                    case "potential_unleashed" -> DMZTrHumanConfig.MULTIPLIER_PU_FORM_PWR.get();
                    default -> DMZTrHumanConfig.MULTIPLIER_BASE.get();
                };
                case 1 -> switch (transformation) { // Saiyans
                    case "oozaru" -> DMZTrSaiyanConfig.MULTIPLIER_OOZARU_FORM_PWR.get();
                    case "ssj1" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ_FORM_PWR.get();
                    case "ssgrade2" -> DMZTrSaiyanConfig.MULTIPLIER_SSGRADE2_FORM_PWR.get();
                    case "ssgrade3" -> DMZTrSaiyanConfig.MULTIPLIER_SSGRADE3_FORM_PWR.get();
                    case "mssj" -> DMZTrSaiyanConfig.MULTIPLIER_MSSJ_FORM_PWR.get();
                    case "ssj2" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ2_FORM_PWR.get();
                    case "ssj3" -> DMZTrSaiyanConfig.MULTIPLIER_SSJ3_FORM_PWR.get();
                    case "golden_oozaru" -> DMZTrSaiyanConfig.MULTIPLIER_GOLDENOOZARU_FORM_PWR.get();
                    default -> DMZTrSaiyanConfig.MULTIPLIER_BASE.get();
                };
                case 2 -> switch (transformation) { // Namekianos
                    case "giant" -> DMZTrNamekConfig.MULTIPLIER_GIANT_FORM_PWR.get();
                    case "full_power" -> DMZTrNamekConfig.MULTIPLIER_FP_FORM_PWR.get();
                    case "super_namek" -> DMZTrNamekConfig.MULTIPLIER_SUPER_NAMEK_FORM_PWR.get();
                    default -> DMZTrNamekConfig.MULTIPLIER_BASE.get();
                };
                case 3 -> switch (transformation) { // BioAndroides
                    case "semi_perfect" -> DMZTrBioAndroidConfig.MULTIPLIER_SEMIPERFECT_FORM_PWR.get();
                    case "perfect" -> DMZTrBioAndroidConfig.MULTIPLIER_PERFECT_FORM_PWR.get();
                    default -> DMZTrBioAndroidConfig.MULTIPLIER_BASE.get();
                };
                case 4 -> switch (transformation) { // Cold Demons
                    case "second_form" -> DMZTrColdDemonConfig.MULTIPLIER_SECOND_FORM_PWR.get();
                    case "third_form" -> DMZTrColdDemonConfig.MULTIPLIER_THIRD_FORM_PWR.get();
                    case "final_form" -> DMZTrColdDemonConfig.MULTIPLIER_FOURTH_FORM_PWR.get();
                    case "full_power" -> DMZTrColdDemonConfig.MULTIPLIER_FULL_POWER_FORM_PWR.get();
                    default -> DMZTrColdDemonConfig.MULTIPLIER_BASE.get();
                };
                case 5 -> switch (transformation) { // Majin
                    case "evil" -> DMZTrMajinConfig.MULTIPLIER_EVIL_FORM_PWR.get();
                    case "kid" -> DMZTrMajinConfig.MULTIPLIER_KID_FORM_PWR.get();
                    case "super" -> DMZTrMajinConfig.MULTIPLIER_SUPER_FORM_PWR.get();
                    case "ultra" -> DMZTrMajinConfig.MULTIPLIER_ULTRA_FORM_PWR.get();
                    default -> DMZTrMajinConfig.MULTIPLIER_BASE.get();
                };
                default -> 1.0;
            };
            case "COST" -> switch (raza) {
                case 0 -> switch (transformation) { // Humanos
                    case "buffed" -> (double) DMZTrHumanConfig.BUFFED_FORM_KI_COST.get();
                    case "full_power" -> (double) DMZTrHumanConfig.FP_FORM_KI_COST.get();
                    case "potential_unleashed" -> (double) DMZTrHumanConfig.PU_FORM_KI_COST.get();
                    default -> (double) DMZTrHumanConfig.BASE_FORM_KI_COST.get();
                };
                case 1 -> switch (transformation) {
                    case "oozaru" -> (double) DMZTrSaiyanConfig.OOZARU_FORM_KI_COST.get();
                    case "ssj1" -> (double) DMZTrSaiyanConfig.SSJ_FORM_KI_COST.get();
                    case "ssgrade2" -> (double) DMZTrSaiyanConfig.SSGRADE2_FORM_KI_COST.get();
                    case "ssgrade3" -> (double) DMZTrSaiyanConfig.SSGRADE3_FORM_KI_COST.get();
                    case "mssj" -> (double) DMZTrSaiyanConfig.MSSJ_FORM_KI_COST.get();
                    case "ssj2" -> (double) DMZTrSaiyanConfig.SSJ2_FORM_KI_COST.get();
                    case "ssj3" -> (double) DMZTrSaiyanConfig.SSJ3_FORM_KI_COST.get();
                    case "golden_oozaru" -> (double) DMZTrSaiyanConfig.GOLDENOOZARU_FORM_KI_COST.get();
                    default -> (double) DMZTrSaiyanConfig.BASE_FORM_KI_COST.get();
                };
                case 2 -> switch (transformation) {
                    case "giant" -> (double) DMZTrNamekConfig.GIANT_FORM_KI_COST.get();
                    case "full_power" -> (double) DMZTrNamekConfig.FP_FORM_KI_COST.get();
                    case "super_namek" -> (double) DMZTrNamekConfig.SUPER_NAMEK_FORM_KI_COST.get();
                    default -> (double) DMZTrNamekConfig.BASE_FORM_KI_COST.get();
                };
                case 3 -> switch (transformation) {
                    case "semi_perfect" -> (double) DMZTrBioAndroidConfig.SEMIPERFECT_FORM_KI_COST.get();
                    case "perfect" -> (double) DMZTrBioAndroidConfig.PERFECT_FORM_KI_COST.get();
                    default -> (double) DMZTrBioAndroidConfig.BASE_FORM_KI_COST.get();
                };
                case 4 -> switch (transformation) {
                    case "second_form" -> (double) DMZTrColdDemonConfig.SECOND_FORM_KI_COST.get();
                    case "third_form" -> (double) DMZTrColdDemonConfig.THIRD_FORM_KI_COST.get();
                    case "final_form" -> (double) DMZTrColdDemonConfig.FOURTH_FORM_KI_COST.get();
                    case "full_power" -> (double) DMZTrColdDemonConfig.FULL_POWER_FORM_KI_COST.get();
                    default -> (double) DMZTrColdDemonConfig.BASE_FORM_KI_COST.get();
                };
                case 5 -> switch (transformation) {
                    case "evil" -> (double) DMZTrMajinConfig.EVIL_FORM_KI_COST.get();
                    case "kid" -> (double) DMZTrMajinConfig.KID_FORM_KI_COST.get();
                    case "super" -> (double) DMZTrMajinConfig.SUPER_FORM_KI_COST.get();
                    case "ultra" -> (double) DMZTrMajinConfig.ULTRA_FORM_KI_COST.get();
                    default -> (double) DMZTrMajinConfig.BASE_FORM_KI_COST.get();
                };
                default -> 1.0;
            };
            default -> 1.0;
        };
    }

    public double transfMultMenu(DMZStatsAttributes stats, String transformation) {
        double str = getTransformationStats(stats.getIntValue("race"), transformation, "STR");
        double def = getTransformationStats(stats.getIntValue("race"), transformation, "DEF");
        double pwr = getTransformationStats(stats.getIntValue("race"), transformation, "PWR");

        return (str + def + pwr) / 3;
    }
}
