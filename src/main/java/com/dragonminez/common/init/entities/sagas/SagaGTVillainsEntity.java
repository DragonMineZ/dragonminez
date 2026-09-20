package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class SagaGTVillainsEntity {

    /*
        BLACK STAR DRAGON BALLS [ LEDGIC - PARA PARA BROTHERS - LUUD - RILLDO ]
     */

    public static class LedgicEntity extends DBSagasEntity {

        public LedgicEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x8A5CFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(1);
            this.setScaleVal(1.1F);
            this.setEvade(true, 120);
            this.setAllowedCombos(160, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.3F, 0xE1BEE7, 0x8E24AA);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 220, 1.3F, 0xE1BEE7, 0x8E24AA);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.25D);

            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    /** Bon, Don and Son Para: three registry entries sharing one class, each with its own model and texture. */
    public static class ParaParaEntity extends DBSagasEntity {

        public ParaParaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFD166);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setAllowedCombos(160, ComboType.BASIC, ComboType.RAPID_KICKS);
            this.addKiSkill(KiSkillType.KI_SMALL, 70, 1.1F, 0xFFF3C4, 0xFFD166);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);
        }
    }

    public static class LuudEntity extends DBSagasEntity {

        public LuudEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setAuraColor(0x9BE7C4);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(1);
            this.setScaleVal(2.5F);
            this.addKiSkill(KiSkillType.KI_LASER, 160, 2.5F, 0xFFFFFF, 0xFF1744);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 2.0F, 0xE0FFF4, 0x4DD9A5);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 300, 15.5F);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2D);
            this.setDefaultMovementSpeed(0.2D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(2.5F, 5.0F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 4.5F, 3.75F, 0.0F, 0.0F, 1.9F),
                    new DBSagasPart(this, "torso", 5.0F, 3.75F, 0.0F, 0.0F, 5.6F),
                    new DBSagasPart(this, "head", 4.0F, 2.5F, 0.0F, 0.0F, 8.75F)
            };
        }
    }

    public static class RilldoEntity extends DBSagasEntity {

        public RilldoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xC0C0C0);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(2);
            this.setEvade(true, 80);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xF5F5F5, 0x9E9E9E);
            this.addKiSkill(KiSkillType.KI_LASER, 180, 1.4F, 0xF5F5F5, 0x9E9E9E);

            this.setWildSense(true, 150);
        }
    }

    public static class MetalRilldoEntity extends DBSagasEntity {

        public MetalRilldoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xB0C4DE);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(2);
            this.setScaleVal(1.1F);
            this.setEvade(true, 70);
            this.setAllowedCombos(140, ComboType.BASIC, ComboType.AIR, ComboType.ANDROID_ABSORPTION);
            this.addKiSkill(KiSkillType.KI_LASER, 160, 1.6F, 0xECEFF1, 0x607D8B);
            this.addKiSkill(KiSkillType.KI_BARRIER, 260, 2.3F, 0xECEFF1, 0x90A4AE);

            this.setWildSense(true, 120);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_rilldo";
        }
    }

    public static class HyperRilldoEntity extends DBSagasEntity {

        public HyperRilldoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x8FA3B8);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(1);
            this.setScaleVal(2.0F);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_LASER, 160, 2.0F, 0xECEFF1, 0x607D8B);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 360, 1.5F, 0xECEFF1, 0x607D8B);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 400, 10.0F);

            this.setWildSense(true, 150);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.24D);
            this.setDefaultMovementSpeed(0.24D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_rilldo";
        }
    }

    /*
        BABY [ BABY VEGETA - SUPER BABY VEGETA 1 - SUPER BABY VEGETA 2 - GOLDEN GREAT APE | TRUE FORM ]
     */

    public static class BabyVegetaEntity extends DBSagasEntity {

        public BabyVegetaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xA64DFF);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(2);
            this.setEvade(true, 60);
            this.setAllowedCombos(140, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xE9D2FF, 0x9C27B0);
            this.addKiSkill(KiSkillType.GALICK_GUN, 320, 1.5F);

            this.setWildSense(true, 140);
            this.setZanzoken(true, 300);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_SUPER_BABY_VEGETA.get();
        }
    }

    public static class SuperBabyVegetaEntity extends DBSagasEntity {

        public SuperBabyVegetaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xF2C12E);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(2);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xFFF3C4, 0xF2C12E);
            this.addKiSkill(KiSkillType.GALICK_GUN, 300, 1.6F);
            this.addKiSkill(KiSkillType.BIG_BANG, 380, 1.7F);

            this.setWildSense(true, 120);
            this.setZanzoken(true, 250);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_baby_vegeta";
        }
    }

    public static class SuperBabyVegeta2Entity extends DBSagasEntity {

        public SuperBabyVegeta2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xF2C12E);
            this.setLightning(true);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(2);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xFFF3C4, 0xF2C12E);
            // Revenge Final Flash / Revenge Death Ball
            this.addKiSkill(KiSkillType.FINAL_FLASH, 380, 2.2F);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 2.0F, 0xFFB3B3, 0xFF2E2E);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 180);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_BABY_GOLDEN_OZARU.get();
        }
    }

    public static class BabyGoldenOzaruEntity extends SagaOzaruEntity {

        public BabyGoldenOzaruEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setAuraColor(0xFFC94A);
            this.addKiSkill(KiSkillType.DEATH_BALL, 500, 3.0F, 0xFFB3B3, 0xFF2E2E);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_baby_golden_ozaru";
        }
    }

    public static class BabyEntity extends DBSagasEntity {

        public BabyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x9C27B0);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(2);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.ANDROID_ABSORPTION, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.3F, 0xE9D2FF, 0x9C27B0);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xE9D2FF, 0x9C27B0);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.6F, 0xE9D2FF, 0x6A1B9A);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
        }
    }

    /*
        SUPER 17
     */

    public static class Super17Entity extends DBSagasEntity {

        public Super17Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x2E8B57);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(0);
            this.setScaleVal(1.05F);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.ANDROID_ABSORPTION, ComboType.BASIC, ComboType.AIR);
            // Super Electric Strike / Hell's Storm barrier
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 2.2F, 0xE8F5E9, 0x00C853);
            this.addKiSkill(KiSkillType.KI_BARRIER, 240, 2.3F, 0xB9F6CA, 0x00E676);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xE8F5E9, 0x00C853);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 200);
        }
    }

    /*
        SHADOW DRAGONS [ LIANG - WU - LIU - QI - NEO (SI) - EIS (SAN) - SYN (YI) - OMEGA ]
     */

    /** Two-Star Dragon (Haze Shenron): pollution and poison smog. */
    public static class LiangXingLongEntity extends DBSagasEntity {

        public LiangXingLongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x7B8B3A);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(2);
            this.setScaleVal(2.0F);
            this.setEvade(true, 80);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 280, 1.8F, 0xDCE775, 0x827717);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 400, 1.3F, 0xC0CA33, 0x616A1E);

            this.setWildSense(true, 120);
        }
    }

    /** Five-Star Dragon (Rage Shenron): electricity. */
    public static class WuXingLongEntity extends DBSagasEntity {

        public WuXingLongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE45C);
            this.setLightning(true);
            this.setLightningColor(0xFFEB3B);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(1);
            this.setAllowedCombos(130, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_LASER, 150, 1.6F, 0xFFFDE7, 0xFFEB3B);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xFFFDE7, 0xFFEB3B);

            this.setWildSense(true, 120);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_WU_XING_LONG_TRANSFORMED.get();
        }
    }

    public static class WuXingLongTransformedEntity extends DBSagasEntity {

        public WuXingLongTransformedEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setAuraColor(0xFFE45C);
            this.setLightning(true);
            this.setLightningColor(0xFFEB3B);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(1);
            this.setScaleVal(2.4F);
            this.setAllowedCombos(130, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_LASER, 140, 2.0F, 0xFFFDE7, 0xFFEB3B);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 190, 1.8F, 0xFFFDE7, 0xFFEB3B);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 380, 1.6F, 0xFFFDE7, 0xFFEB3B);

            this.setWildSense(true, 100);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.24D);
            this.setDefaultMovementSpeed(0.24D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.9D);
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(2.0F, 4.0F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 3.6F, 2.4F, 0.0F, 0.0F, 1.2F),
                    new DBSagasPart(this, "torso", 4.4F, 2.4F, 0.0F, 0.0F, 3.5F),
                    new DBSagasPart(this, "head", 3.0F, 1.4F, 0.0F, 0.0F, 5.3F)
            };
        }
    }

    /** Six-Star Dragon (Oceanus Shenron): wind and typhoons. */
    public static class LiuXingLongEntity extends DBSagasEntity {

        public LiuXingLongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x4FC3F7);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(2);
            this.setEvade(true, 60);
            this.setAllowedCombos(140, ComboType.AIR, ComboType.RAPID_KICKS);
            this.addKiSkill(KiSkillType.BLUE_HURRICANE, 400, 1.5F);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 260, 0.8F, 0xE0F7FA, 0x26C6DA);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 250);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_LIU_XING_LONG_TRANSFORMED.get();
        }
    }

    public static class LiuXingLongTransformedEntity extends DBSagasEntity {

        public LiuXingLongTransformedEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x4FC3F7);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(2);
            this.setScaleVal(2.0F);
            this.setEvade(true, 60);
            this.setAllowedCombos(130, ComboType.AIR, ComboType.RAPID_KICKS, ComboType.BASIC);
            this.addKiSkill(KiSkillType.BLUE_HURRICANE, 360, 1.8F);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 240, 1.0F, 0xE0F7FA, 0x26C6DA);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 1.7F, 0xE0F7FA, 0x26C6DA);

            this.setWildSense(true, 90);
            this.setZanzoken(true, 220);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5D);
        }
    }

    /** Seven-Star Dragon (Naturon Shenron): absorbs whatever it touches. */
    public static class QiXingLongEntity extends DBSagasEntity {

        public QiXingLongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setAuraColor(0x6D8B3A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(1);
            this.setScaleVal(3.0F);
            this.setAllowedCombos(140, ComboType.ANDROID_ABSORPTION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 220, 1.6F, 0xD7CCC8, 0x6D4C41);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 420, 1.3F, 0xD7CCC8, 0x6D4C41);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22D);
            this.setDefaultMovementSpeed(0.22D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.9D);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_QI_XING_LONG_TRANSFORMED.get();
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(2.5F, 4.5F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 4.5F, 2.2F, 0.0F, 0.0F, 1.1F),
                    new DBSagasPart(this, "torso", 5.5F, 2.6F, 0.0F, 0.0F, 3.4F),
                    new DBSagasPart(this, "head", 3.5F, 1.4F, 0.0F, 0.0F, 5.3F)
            };
        }
    }

    public static class QiXingLongTransformedEntity extends DBSagasEntity {

        public QiXingLongTransformedEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setAuraColor(0x6D8B3A);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(1);
            this.setScaleVal(4.0F);
            this.setAllowedCombos(130, ComboType.ANDROID_ABSORPTION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.9F, 0xD7CCC8, 0x6D4C41);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 380, 1.7F, 0xD7CCC8, 0x6D4C41);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 320, 12.0F);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22D);
            this.setDefaultMovementSpeed(0.22D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(2.5F, 5.0F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 6.0F, 3.0F, 0.0F, 0.0F, 1.5F),
                    new DBSagasPart(this, "torso", 7.0F, 3.4F, 0.0F, 0.0F, 4.6F),
                    new DBSagasPart(this, "head", 4.5F, 1.8F, 0.0F, 0.0F, 7.1F)
            };
        }
    }

    /** Four-Star Dragon (Si Xing Long / Nuova Shenron): fire and the Nova Star. */
    public static class NeoShenronEntity extends DBSagasEntity {

        public NeoShenronEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFF7A1A);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(0);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 260, 1.8F, 0xFFE0B2, 0xFF6D00);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 380, 1.4F, 0xFFCC80, 0xFF3D00);
            this.addKiSkill(KiSkillType.DEATH_BALL, 480, 1.8F, 0xFFF3E0, 0xFF9100);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 220);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_NEO_SHENRON_TRANSFORMED.get();
        }
    }

    public static class NeoShenronTransformedEntity extends DBSagasEntity {

        public NeoShenronTransformedEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFD54F);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 240, 2.1F, 0xFFF8E1, 0xFFB300);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 360, 1.7F, 0xFFECB3, 0xFF6F00);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 2.2F, 0xFFF8E1, 0xFFB300);

            this.setWildSense(true, 60);
            this.setZanzoken(true, 180);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
            this.setDefaultMovementSpeed(0.32D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_neo_shenron";
        }
    }

    /** Three-Star Dragon (San Xing Long / Eis Shenron): ice. */
    public static class EisShenronEntity extends DBSagasEntity {

        public EisShenronEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xB3E5FC);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(2);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xE1F5FE, 0x81D4FA);
            this.addKiSkill(KiSkillType.KI_LASER, 170, 1.6F, 0xE1F5FE, 0x4FC3F7);
            this.addKiSkill(KiSkillType.KI_BARRIER, 260, 2.3F, 0xE1F5FE, 0x4FC3F7);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 220);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_neo_shenron";
        }
    }

    /** One-Star Dragon (Yi Xing Long / Syn Shenron) before he swallows the other Dragon Balls. */
    public static class SynShenronEntity extends DBSagasEntity {

        public SynShenronEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xB71C1C);
            this.setLightning(true);
            this.setLightningColor(0xFF5252);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(2);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            // Dragon Thunder / Negative Karma Ball
            this.addKiSkill(KiSkillType.KI_LASER, 150, 1.8F, 0xFFEBEE, 0xD50000);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.6F, 0xFFCDD2, 0xB71C1C);
            this.addKiSkill(KiSkillType.DEATH_BALL, 460, 2.0F, 0x311B92, 0xB71C1C);

            this.setWildSense(true, 60);
            this.setZanzoken(true, 180);
        }
    }

    /** Syn Shenron after absorbing the Dragon Balls: every Shadow Dragon's power at once. */
    public static class OmegaShenronEntity extends DBSagasEntity {

        public OmegaShenronEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xC62828);
            this.setLightning(true);
            this.setLightningColor(0xFF5252);
            this.setKiBlastSpeed(2.0F);
            this.setDBZStyle(2);
            this.setScaleVal(2.5F);
            this.setEvade(true, 30);
            this.setAllowedCombos(100, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            // Minus Energy Power Ball, Dragon Thunder, Ice Slash, Dragon Typhoon, Nova Star
            this.addKiSkill(KiSkillType.DEATH_BALL, 420, 3.0F, 0x1A0033, 0x7B1FA2);
            this.addKiSkill(KiSkillType.KI_LASER, 150, 2.0F, 0xFFEBEE, 0xD50000);
            this.addKiSkill(KiSkillType.KI_BARRIER, 280, 2.3F, 0xE1F5FE, 0x4FC3F7);
            this.addKiSkill(KiSkillType.BLUE_HURRICANE, 420, 1.5F);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 260, 2.0F, 0xFFE0B2, 0xFF6D00);

            this.setWildSense(true, 40);
            this.setZanzoken(true, 120);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8D);
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(2.0F, 4.0F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 2.4F, 2.6F, 0.0F, 0.0F, 1.3F),
                    new DBSagasPart(this, "torso", 3.0F, 2.4F, 0.0F, 0.0F, 3.7F),
                    new DBSagasPart(this, "head", 2.0F, 1.4F, 0.0F, 0.0F, 5.55F)
            };
        }
    }
}
