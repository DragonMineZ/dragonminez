package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaGTEntity {

    private static final int BABY_AURA = 0xA64DFF;
    private static final int BABY_KI_MAIN = 0xE9D2FF;
    private static final int BABY_KI_BORDER = 0x9C27B0;

    private static final int SSJ4_AURA = 0xFF3A3A;

    /*
        UUB [ UUB | MAJUUB ]
     */

    public static class UubEntity extends DBSagasEntity {

        public UubEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 80);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.AIR, ComboType.RAPID_KICKS);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.2F, 0xFFF3C4, 0xFFC94A);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 1.4F, 0xFFF3C4, 0xFFC94A);

            this.setWildSense(true, 150);
        }
    }

    public static class MajuubEntity extends DBSagasEntity {

        public MajuubEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFB8E8);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(120, ComboType.BASIC, ComboType.AIR, ComboType.RAPID_KICKS, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.4F, 0xFFE3F6, 0xFF6FCF);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 280, 1.8F, 0xFFE3F6, 0xFF6FCF);
            this.addKiSkill(KiSkillType.MAJIN_CANDY, 400, 1.0F, 0xFF82F3, 0xFF1AEC);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
        }
    }

    /*
        GOKU GT [ BASE - SSJ - SSJ3 | SSJ4 ]
     */

    public static class GokuGTBaseEntity extends DBSagasEntity {

        public GokuGTBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(180, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 220, 1.2F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.2F, 0x75FFFF, 0x75FFFF);

            this.setWildSense(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_kid_goku";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_GT_SSJ.get();
        }
    }

    public static class GokuGTSSJEntity extends DBSagasEntity {

        public GokuGTSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(160, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 120);
            this.setZanzoken(true, 300);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goten_ssj";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_GT_SSJ3.get();
        }
    }

    public static class GokuGTSSJ3Entity extends DBSagasEntity {

        public GokuGTSSJ3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(0xFFE657);
            this.setLightning(true);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(140, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 2.5F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 200);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gokugt_ssj3";
        }
    }

    public static class GokuGTSSJ4Entity extends DBSagasEntity {

        public GokuGTSSJ4Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ4_AURA);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA_X10, 220, 3.0F);
            this.addKiSkill(KiSkillType.DRAGON_FIST, 600);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 60);
            this.setZanzoken(true, 150);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
            this.setDefaultMovementSpeed(0.32D);
        }
    }

    /*
        VEGETA GT [ BASE - SSJ - SSJ2 | SSJ4 ]
     */

    public static class VegetaGTBaseEntity extends DBSagasEntity {

        public VegetaGTBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(150, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 350, 1.4F);

            this.setWildSense(true, 150);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_GT_SSJ.get();
        }
    }

    public static class VegetaGTSSJEntity extends DBSagasEntity {

        public VegetaGTSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.BIG_BANG, 300, 1.7F);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 420, 2.0F);

            this.setWildSense(true, 120);
            this.setZanzoken(true, 250);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_gt";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_GT_SSJ2.get();
        }
    }

    public static class VegetaGTSSJ2Entity extends DBSagasEntity {

        public VegetaGTSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setLightning(true);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 45);
            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 190, 1.5F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.BIG_BANG, 290, 1.8F);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 400, 2.3F);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_gt";
        }
    }

    public static class VegetaGTSSJ4Entity extends DBSagasEntity {

        public VegetaGTSSJ4Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ4_AURA);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            // Final Shine Attack
            this.addKiSkill(KiSkillType.FINAL_FLASH, 380, 2.6F, 0xE9FFE3, 0x5CFF6A, 0x1B9E2B);
            this.addKiSkill(KiSkillType.BIG_BANG, 280, 1.9F);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xFFB3B3, 0xFF4040);

            this.setWildSense(true, 60);
            this.setZanzoken(true, 150);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
            this.setDefaultMovementSpeed(0.32D);
        }
    }

    /*
        GOHAN GT [ BASE - SSJ | BABY ]
     */

    public static class GohanGTBaseEntity extends DBSagasEntity {

        public GohanGTBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 70);
            this.setAllowedCombos(160, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.MASENKO, 250, 1.4F);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 320, 1.4F);

            this.setWildSense(true, 150);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOHAN_GT_SSJ.get();
        }
    }

    public static class GohanGTSSJEntity extends DBSagasEntity {

        public GohanGTSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(140, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.MASENKO, 220, 1.6F);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 280, 2.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.4F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 120);
            this.setZanzoken(true, 250);
        }
    }

    public static class GohanGTBabyEntity extends DBSagasEntity {

        public GohanGTBabyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(BABY_AURA);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.MASENKO, 240, 1.5F, BABY_KI_MAIN, BABY_KI_BORDER);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.3F, BABY_KI_MAIN, BABY_KI_BORDER);

            this.setWildSense(true, 130);
            this.setZanzoken(true, 300);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gohan_gt";
        }
    }

    /*
        GOTEN GT [ BASE - SSJ | BABY ]
     */

    public static class GotenGTBaseEntity extends DBSagasEntity {

        public GotenGTBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 70);
            this.setAllowedCombos(170, ComboType.AIR, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 300, 1.3F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.2F, 0x75FFFF, 0x75FFFF);

            this.setWildSense(true, 160);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOTEN_GT_SSJ.get();
        }
    }

    public static class GotenGTSSJEntity extends DBSagasEntity {

        public GotenGTSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 260, 1.7F);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 130);
            this.setZanzoken(true, 280);
        }
    }

    public static class GotenGTBabyEntity extends DBSagasEntity {

        public GotenGTBabyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(BABY_AURA);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(160, ComboType.AIR, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 280, 1.5F, BABY_KI_MAIN, BABY_KI_BORDER);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.3F, BABY_KI_MAIN, BABY_KI_BORDER);

            this.setWildSense(true, 140);
            this.setZanzoken(true, 300);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goten_gt";
        }
    }

    /*
        TRUNKS GT [ BASE - SSJ | BABY ]
     */

    public static class TrunksGTBaseEntity extends DBSagasEntity {

        public TrunksGTBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 70);
            this.setAllowedCombos(160, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            // Burning Attack
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 1.4F, 0xFFF3C4, 0xFFB300);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0xFFF3C4, 0xFFB300);

            this.setWildSense(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_trunks";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_TRUNKS_GT_SSJ.get();
        }
    }

    public static class TrunksGTSSJEntity extends DBSagasEntity {

        public TrunksGTSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(140, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 260, 1.8F, 0xFFF3C4, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.4F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 120);
            this.setZanzoken(true, 250);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_trunks_ssj";
        }
    }

    public static class TrunksGTBabyEntity extends DBSagasEntity {

        public TrunksGTBabyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(BABY_AURA);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 280, 1.5F, BABY_KI_MAIN, BABY_KI_BORDER);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.3F, BABY_KI_MAIN, BABY_KI_BORDER);

            this.setWildSense(true, 130);
            this.setZanzoken(true, 300);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_trunks";
        }
    }

    /*
        PAN
     */

    public static class PanEntity extends DBSagasEntity {

        public PanEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setScaleVal(0.9F);
            this.setAllowedCombos(130, ComboType.BASIC, ComboType.RAPID_KICKS);
            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.2F, 0xFFD1E8, 0xFF7AB8);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 320, 1.0F);

            this.setWildSense(true, 120);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_videl";
        }
    }

    /*
        GOGETA SSJ4
     */

    public static class GogetaSSJ4Entity extends DBSagasEntity {

        public GogetaSSJ4Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ4_AURA);
            this.setKiBlastSpeed(2.0F);
            this.setDBZStyle(0);
            this.setEvade(true, 30);
            this.setAllowedCombos(100, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            // 100x Big Bang Kamehameha
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 260, 3.5F, 0xFFFFFF, 0x40C4FF, 0x0D47A1);
            this.addKiSkill(KiSkillType.BIG_BANG, 240, 2.0F);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 160, 1.8F, 0xFFB3B3, 0xFF4040);

            this.setWildSense(true, 40);
            this.setZanzoken(true, 100);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.34D);
            this.setDefaultMovementSpeed(0.34D);
        }
    }

    /*
        ANDROID 18 (GT)
     */

    public static class A18GTEntity extends DBSagasEntity {

        public A18GTEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.BASIC, ComboType.RAPID_KICKS);
            this.addKiSkill(KiSkillType.KIENZAN, 400, 3.0F, 0xFF70F6, 0xFF70F6);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 2.3F, 0xFF70F6, 0xFF70F6);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_a18";
        }
    }
}
