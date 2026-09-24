package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SagaDaimaEntity {

    private static final int SSJ_AURA = 0xFFE657;
    private static final int SSJ4_AURA = 0xFF3A3A;
    private static final float TAMAGAMI_SCALE = 1.5F;

    /*
        GOKU DAIMA [ BASE - SSJ - SSJ2 - SSJ3 | SSJ4 ]
     */

    public static class GokuDaimaBaseEntity extends DBSagasEntity {

        public GokuDaimaBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(180, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.SUPER_GOD_FIST);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);
            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.5F, 0x75FFFF, 0x75FFFF);

            this.setWildSense(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_DAIMA_SSJ.get();
        }
    }

    public static class GokuDaimaSSJEntity extends DBSagasEntity {

        public GokuDaimaSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ_AURA);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 45);
            this.setAllowedCombos(170, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.8F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, SSJ_AURA, SSJ_AURA);

            this.setWildSense(true, 130);
            this.setZanzoken(3, 300);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku_ssj";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_DAIMA_SSJ2.get();
        }
    }

    public static class GokuDaimaSSJ2Entity extends DBSagasEntity {

        public GokuDaimaSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ_AURA);
            this.setLightning(true);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(160, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 2.2F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.6F, SSJ_AURA, SSJ_AURA);

            this.setWildSense(true, 110);
            this.setZanzoken(3, 250);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku_ssj2";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_DAIMA_SSJ3.get();
        }
    }

    public static class GokuDaimaSSJ3Entity extends DBSagasEntity {

        public GokuDaimaSSJ3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ_AURA);
            this.setLightning(true);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(0);
            this.setEvade(true, 35);
            this.setAllowedCombos(140, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 3.0F);
            this.addKiSkill(KiSkillType.DRAGON_FIST, 600);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.6F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.6F, SSJ_AURA, SSJ_AURA);

            this.setWildSense(true, 70);
            this.setZanzoken(3, 200);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku_ssj3";
        }
    }

    public static class GokuDaimaSSJ4Entity extends DBSagasEntity {

        public GokuDaimaSSJ4Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ4_AURA);
            this.setScaleVal(1.2F);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(0);
            this.setEvade(true, 35);
            this.setAllowedCombos(120, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA_X10, 220, 3.2F);
            this.addKiSkill(KiSkillType.DRAGON_FIST, 550);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.7F, 0xFFB3B3, 0xFF4040);

            this.setWildSense(true, 60);
            this.setZanzoken(3, 150);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
            this.setDefaultMovementSpeed(0.32D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku_gt_ssj4";
        }
    }

    /*
        GOKU MINI [ BASE - SSJ - SSJ2 - SSJ3 | SSJ4 ]
     */

    public static class GokuMiniBaseEntity extends DBSagasEntity {

        public GokuMiniBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(7);
            this.setEvade(true, 60);
            this.setAllowedCombos(180, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 220, 1.2F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.2F, 0x75FFFF, 0x75FFFF);

            this.setWildSense(true, 150);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.POWER_POLE.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
            return MainEntities.SAGA_GOKU_MINI_SSJ.get();
        }
    }

    public static class GokuMiniSSJEntity extends DBSagasEntity {

        public GokuMiniSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(SSJ_AURA);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(7);
            this.setEvade(true, 50);
            this.setAllowedCombos(160, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, SSJ_AURA, SSJ_AURA);

            this.setWildSense(true, 120);
            this.setZanzoken(3, 300);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.POWER_POLE.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
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
            return MainEntities.SAGA_GOKU_MINI_SSJ2.get();
        }
    }

    public static class GokuMiniSSJ2Entity extends DBSagasEntity {

        public GokuMiniSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(SSJ_AURA);
            this.setLightning(true);
            this.setKiBlastSpeed(1.55F);
            this.setDBZStyle(0);
            this.setEvade(true, 45);
            this.setAllowedCombos(150, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 2.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.4F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, SSJ_AURA, SSJ_AURA);

            this.setWildSense(true, 100);
            this.setZanzoken(3, 250);
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
            return MainEntities.SAGA_GOKU_MINI_SSJ3.get();
        }
    }

    public static class GokuMiniSSJ3Entity extends DBSagasEntity {

        public GokuMiniSSJ3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(SSJ_AURA);
            this.setLightning(true);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(140, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 2.5F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, SSJ_AURA, SSJ_AURA);

            this.setWildSense(true, 80);
            this.setZanzoken(3, 200);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_gokugt_ssj3";
        }
    }

    public static class GokuMiniSSJ4Entity extends DBSagasEntity {

        public GokuMiniSSJ4Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(SSJ4_AURA);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA_X10, 220, 2.8F);
            this.addKiSkill(KiSkillType.DRAGON_FIST, 600);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xFFB3B3, 0xFF4040);

            this.setWildSense(true, 60);
            this.setZanzoken(3, 150);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
            this.setDefaultMovementSpeed(0.32D);
        }
    }

    /*
        VEGETA DAIMA [ BASE - SSJ - SSJ2 - SSJ3 ]
     */

    public static class VegetaDaimaBaseEntity extends DBSagasEntity {

        public VegetaDaimaBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 55);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.3F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 350, 1.4F);

            this.setWildSense(true, 140);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_DAIMA_SSJ.get();
        }
    }

    public static class VegetaDaimaSSJEntity extends DBSagasEntity {

        public VegetaDaimaSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ_AURA);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 45);
            this.setAllowedCombos(140, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.GALICK_GUN, 320, 1.6F);
            this.addKiSkill(KiSkillType.BIG_BANG, 280, 1.7F);

            this.setWildSense(true, 120);
            this.setZanzoken(3, 280);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_DAIMA_SSJ2.get();
        }
    }

    public static class VegetaDaimaSSJ2Entity extends DBSagasEntity {

        public VegetaDaimaSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ_AURA);
            this.setLightning(true);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(130, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 190, 1.5F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.GALICK_GUN, 300, 1.8F);
            this.addKiSkill(KiSkillType.BIG_BANG, 280, 1.9F);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 400, 2.3F);

            this.setWildSense(true, 100);
            this.setZanzoken(3, 230);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_ssj2";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_DAIMA_SSJ3.get();
        }
    }

    public static class VegetaDaimaSSJ3Entity extends DBSagasEntity {

        public VegetaDaimaSSJ3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(SSJ_AURA);
            this.setLightning(true);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(0);
            this.setEvade(true, 35);
            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION, ComboType.BASIC, ComboType.DEADLY_DANCE_VEGETTO);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.GALICK_GUN, 280, 2.2F);
            this.addKiSkill(KiSkillType.BIG_BANG, 260, 2.2F);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 380, 3.0F);

            this.setWildSense(true, 70);
            this.setZanzoken(3, 180);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_ssj3";
        }
    }

    /*
        VEGETA MINI [ BASE - SSJ - SSJ2 - SSJ3 ]
     */

    public static class VegetaMiniBaseEntity extends DBSagasEntity {

        public VegetaMiniBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(170, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 380, 1.2F);

            this.setWildSense(true, 150);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_MINI_SSJ.get();
        }
    }

    public static class VegetaMiniSSJEntity extends DBSagasEntity {

        public VegetaMiniSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(SSJ_AURA);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.3F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.GALICK_GUN, 340, 1.4F);
            this.addKiSkill(KiSkillType.BIG_BANG, 300, 1.5F);

            this.setWildSense(true, 130);
            this.setZanzoken(3, 300);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_mini";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_MINI_SSJ2.get();
        }
    }

    public static class VegetaMiniSSJ2Entity extends DBSagasEntity {

        public VegetaMiniSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(SSJ_AURA);
            this.setLightning(true);
            this.setKiBlastSpeed(1.55F);
            this.setDBZStyle(0);
            this.setEvade(true, 45);
            this.setAllowedCombos(140, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 190, 1.4F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.GALICK_GUN, 320, 1.6F);
            this.addKiSkill(KiSkillType.BIG_BANG, 290, 1.7F);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 420, 2.0F);

            this.setWildSense(true, 110);
            this.setZanzoken(3, 250);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_mini";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_MINI_SSJ3.get();
        }
    }

    public static class VegetaMiniSSJ3Entity extends DBSagasEntity {

        public VegetaMiniSSJ3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setisKid(true);
            this.setAuraColor(SSJ_AURA);
            this.setLightning(true);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(130, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION, ComboType.BASIC, ComboType.DEADLY_DANCE_VEGETTO);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.5F, SSJ_AURA, SSJ_AURA);
            this.addKiSkill(KiSkillType.GALICK_GUN, 300, 1.9F);
            this.addKiSkill(KiSkillType.BIG_BANG, 270, 1.9F);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 400, 2.6F);

            this.setWildSense(true, 80);
            this.setZanzoken(3, 200);
        }
    }

    /*
        GLORIO
     */

    public static class GlorioEntity extends DBSagasEntity {

        private static final int ELECTRIC_WHIP_COOLDOWN = 260;

        public GlorioEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(140, ComboType.BASIC, ComboType.AIR, ComboType.RAPID_KICKS);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.4F, 0xE6CCFF, 0x9B30FF);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xE6CCFF, 0x9B30FF);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 1.8F, 0xE6CCFF, 0x9B30FF);
            this.addKiSkill(KiSkillType.DODONPA, ELECTRIC_WHIP_COOLDOWN, 1.2F, 0xE6CCFF, 0x9B30FF);

            this.setWildSense(true, 120);
            this.setZanzoken(1, 250);
        }
    }

    /*
        GOMAH [ MINI | THIRD EYE ]
     */

    public static class GomahMiniEntity extends DBSagasEntity {

        public GomahMiniEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xE0245E);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(160, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.3F, 0xFFD6E0, 0xE0245E);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.3F, 0xFFD6E0, 0xE0245E);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 320, 1.5F, 0xFFD6E0, 0xE0245E);
            this.addKiSkill(KiSkillType.KI_BARRIER, 300, 1.8F, 0xFF4A4A, 0xD10000);

            this.setWildSense(true, 140);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOMAH_THIRD_EYE.get();
        }
    }

    public static class GomahThirdEyeEntity extends DBSagasEntity {

        public GomahThirdEyeEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(2);
            this.setAuraColor(0xE0245E);
            this.setKiBlastSpeed(2.0F);
            this.setScaleVal(5.5F);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300, 1.0F, 0xFFD6E0, 0xE0245E);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 420, 1.2F, 0xFFD6E0, 0xE0245E);
            this.addKiSkill(KiSkillType.KI_BARRIER, 300, 1.8F, 0xFF4A4A, 0xD10000);

            this.setWildSense(true, 120);
            this.setZanzoken(1, 150);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28D);
            this.setDefaultMovementSpeed(0.28D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5D);
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(2.3F, 4.6F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 3.7F, 4.6F, 0.0F, 0.0F, 2.3F),
                    new DBSagasPart(this, "torso", 4.1F, 4.6F, 0.0F, 0.0F, 6.4F),
                    new DBSagasPart(this, "head", 3.7F, 3.7F, 0.5F, 0.0F, 10.1F)
            };
        }
    }

    /*
        MAJIN DUU
     */

    public static class MajinDuuEntity extends DBSagasEntity {

        public MajinDuuEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xA6FF3A);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(150, ComboType.GUM_PUNCH, ComboType.GUM_EXPAND, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.4F, 0xF0FFD6, 0x7ED321);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 160, 1.2F, 0xF0FFD6, 0x7ED321);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 1.6F, 0xF0FFD6, 0x7ED321);

            this.setWildSense(true, 120);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.35D);
        }
    }

    /*
        MAJIN KUU
     */

    public static class MajinKuuEntity extends DBSagasEntity {

        public MajinKuuEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xA6FF3A);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 70);
            this.setAllowedCombos(160, ComboType.AIR, ComboType.SLEEP_RECOVERY);
            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.5F, 0xF0FFD6, 0x7ED321);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 160, 1.3F, 0xF0FFD6, 0x7ED321);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 320, 1.8F, 0xF0FFD6, 0x7ED321);
            this.addKiSkill(KiSkillType.ASSAULT_RAIN, 300, 1.5F, 0xF0FFD6, 0x7ED321, 0x4C9A12);

            this.setWildSense(true, 130);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.45D);
        }
    }
    /*
        TAMAGAMI [ 1 - 2 - 3 ]
     */

    public static class Tamagami1Entity extends DBSagasEntity {

        public Tamagami1Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setScaleVal(TAMAGAMI_SCALE);
            this.refreshDimensions();
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.3F, 0xE6F7FF, 0x4FC3F7);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.3F, 0xE6F7FF, 0x4FC3F7);

            this.setWildSense(true, 130);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.TAMAGAMI_SWORD.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_TAMAGAMI_1_POWERED.get();
        }

        @Override
        protected void finishTransformationSpawn(DBSagasEntity newEntity, boolean fullHealth) {
            float healthFraction = this.getHealth() / this.getMaxHealth();
            super.finishTransformationSpawn(newEntity, fullHealth);
            if (newEntity != null && !this.level().isClientSide) {
                newEntity.setHealth(Math.max(1.0F, newEntity.getMaxHealth() * healthFraction));
            }
        }
    }

    public static class Tamagami1PoweredEntity extends DBSagasEntity {

        public Tamagami1PoweredEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setScaleVal(TAMAGAMI_SCALE);
            this.refreshDimensions();
            this.setAuraColor(0xFF8C1A);
            this.setKiBlastSpeed(1.9F);
            this.setDBZStyle(0);
            this.setEvade(true, 45);
            this.setAllowedCombos(100, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.6F, 0xFFE0B3, 0xFF8C1A);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 130, 1.6F, 0xFFE0B3, 0xFF8C1A);

            this.setWildSense(true, 90);
            this.setZanzoken(2, 250);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.TAMAGAMI_SWORD.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.5D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_tamagami_1";
        }

        @Override
        public OutlineStyle getOutlineStyle() {
            return new OutlineStyle(0xFF3030, 0x8A0000, 3.0F);
        }
    }

    public static class Tamagami2Entity extends DBSagasEntity {

        public Tamagami2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setScaleVal(TAMAGAMI_SCALE);
            this.refreshDimensions();
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.4F, 0xE6F7FF, 0x4FC3F7);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 1.6F, 0xE6F7FF, 0x4FC3F7);

            this.setWildSense(true, 120);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.TAMAGAMI_TRIDENT.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_TAMAGAMI_2_POWERED.get();
        }

        @Override
        protected void finishTransformationSpawn(DBSagasEntity newEntity, boolean fullHealth) {
            float healthFraction = this.getHealth() / this.getMaxHealth();
            super.finishTransformationSpawn(newEntity, fullHealth);
            if (newEntity != null && !this.level().isClientSide) {
                newEntity.setHealth(Math.max(1.0F, newEntity.getMaxHealth() * healthFraction));
            }
        }
    }

    public static class Tamagami2PoweredEntity extends DBSagasEntity {

        public Tamagami2PoweredEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setScaleVal(TAMAGAMI_SCALE);
            this.refreshDimensions();
            this.setAuraColor(0xFF8C1A);
            this.setKiBlastSpeed(1.9F);
            this.setDBZStyle(0);
            this.setEvade(true, 45);
            this.setAllowedCombos(100, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.6F, 0xFFE0B3, 0xFF8C1A);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 200, 1.9F, 0xFFE0B3, 0xFF8C1A);

            this.setWildSense(true, 90);
            this.setZanzoken(2, 250);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.TAMAGAMI_TRIDENT.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.5D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_tamagami_2";
        }

        @Override
        public OutlineStyle getOutlineStyle() {
            return new OutlineStyle(0x7FD4FF, 0x2F6BFF, 3.0F);
        }
    }

    public static class Tamagami3Entity extends DBSagasEntity {

        public Tamagami3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setScaleVal(TAMAGAMI_SCALE);
            this.refreshDimensions();
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(140, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xE6F7FF, 0x4FC3F7);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 320, 1.8F, 0xE6F7FF, 0x4FC3F7);

            this.setWildSense(true, 110);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.TAMAGAMI_HAMMER.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_TAMAGAMI_3_POWERED.get();
        }

        @Override
        protected void finishTransformationSpawn(DBSagasEntity newEntity, boolean fullHealth) {
            float healthFraction = this.getHealth() / this.getMaxHealth();
            super.finishTransformationSpawn(newEntity, fullHealth);
            if (newEntity != null && !this.level().isClientSide) {
                newEntity.setHealth(Math.max(1.0F, newEntity.getMaxHealth() * healthFraction));
            }
        }
    }

    public static class Tamagami3PoweredEntity extends DBSagasEntity {

        public Tamagami3PoweredEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setScaleVal(TAMAGAMI_SCALE);
            this.refreshDimensions();
            this.setAuraColor(0xFF8C1A);
            this.setKiBlastSpeed(1.9F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(90, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 130, 1.7F, 0xFFE0B3, 0xFF8C1A);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 220, 2.0F, 0xFFE0B3, 0xFF8C1A);

            this.setWildSense(true, 80);
            this.setZanzoken(2, 250);
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.TAMAGAMI_HAMMER.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.5D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_tamagami_3";
        }

        @Override
        public OutlineStyle getOutlineStyle() {
            return new OutlineStyle(0xFF9A2E, 0xB84A00, 3.0F);
        }
    }
}
