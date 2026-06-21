package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SagaMoviesEntity {

    public static class GarlickJrEntity extends DBSagasEntity {

        public GarlickJrEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setZanzoken(true, 100);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 2.3F, 0xCC3ED6, 0x8B1094);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 170, 1.0F, 0xCC3ED6, 0x8B1094);
        }
    }

    public static class GarlickJrTransformedEntity extends DBSagasEntity {

        public GarlickJrTransformedEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setAuraColor(0x99112A);
            this.setKiBlastSpeed(1.4F);
            this.setZanzoken(true, 100);
            this.setScaleVal(1.2f);
            this.setAllowedCombos(150, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 2.3F, 0xCC3ED6, 0x8B1094);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 170, 1.0F, 0xCC3ED6, 0x8B1094);
        }
    }

    public static class DrWheeloEntity extends DBSagasEntity {

        public DrWheeloEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setLightning(true);
            this.setScaleVal(1.5f);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 170, 1.0F, 0xCC3ED6, 0x8B1094);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xCC3ED6, 0x8B1094);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4D);

        }

    }


    public static class TurlesEntity extends DBSagasEntity {

        public TurlesEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xF52727);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 450, 1.0F, 0xA946E3, 0x671199);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300 //uff
                    , 0.5F, 0xA946E3, 0x671199);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setDefaultAttackSpeed(6.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class SlugSoldierEntity extends DBSagasEntity {

        public SlugSoldierEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setKiBlastSpeed(1.4F);
            this.setCanFly(true);
            this.setZanzoken(true, 100);
            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xD58AFF, 0x9B4EC7);
        }
    }

    public static class SlugEntity extends DBSagasEntity {

        public SlugEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.0f);
            this.setScaleVal(1.2f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.GUM_PUNCH);

            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 450, 1.0F, 0xA946E3, 0x671199);
            this.addKiSkill(KiSkillType.MASENKO, 250, 2.0F);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }


        @Override
        public String getGeckolibModelName() {
            return "saga_slug";
        }
    }

    public static class SlugGiantEntity extends DBSagasEntity {

        public SlugGiantEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.0f);
            this.setScaleVal(4.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.GUM_PUNCH);

            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 450, 1.0F, 0xA946E3, 0x671199);
            this.addKiSkill(KiSkillType.MASENKO, 250, 2.0F);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }


        @Override
        public String getGeckolibModelName() {
            return "saga_slug";
        }
    }

    public static class CoolerSoldierEntity extends DBSagasEntity {

        public CoolerSoldierEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setKiBlastSpeed(1.4F);
            this.setCanFly(true);
            this.setZanzoken(true, 100);
            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xDD85FF, 0x9122BD);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 250, 1.0F, 0xDD85FF, 0x9122BD);

        }
    }

    public static class CoolerEntity extends DBSagasEntity {

        public CoolerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.KI_SMALL, 200, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.1F, 0xEB1E1E, 0x9E1818);


            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class Cooler5TAEntity extends DBSagasEntity {

        public Cooler5TAEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setLightning(true);
            this.setLightningColor(0x991219);
            this.setKiBlastSpeed(2.2f);
            this.setScaleVal(1.4f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xBF0F18, 0x991219);
            this.addKiSkill(KiSkillType.KI_SMALL, 200, 1.0F, 0xBF0F18, 0x991219);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.1F, 0xBF0F18, 0x991219);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class GeteRobotEntity extends DBSagasEntity {

        public GeteRobotEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setZanzoken(true, 100);
            this.setDBZStyle(2);
        }
    }

    public static class MechaCoolerEntity extends DBSagasEntity {

        public MechaCoolerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.KI_SMALL, 200, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.1F, 0xEB1E1E, 0x9E1818);


            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
            this.setEvade(true, 60);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }


        @Override
        public String getGeckolibModelName() {
            return "saga_cooler";
        }
    }

    public static class MechaCoolerCoreEntity extends DBSagasEntity {

        public MechaCoolerCoreEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2f);
            this.setScaleVal(4.0f);
            this.setLightning(true);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_LASER, 60, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.KI_SMALL, 200, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.1F, 0xEB1E1E, 0x9E1818);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300 //uff
                    , 0.5F, 0xEB1E1E, 0x9E1818);


            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
            this.setEvade(true, 60);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class A14Entity extends DBSagasEntity {

        public A14Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2f);
            this.setScaleVal(1.2f);
            this.setCanFly(true);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_SMALL, 200, 1.0F, 0xAA00FF, 0x6C00A3);

            this.setWildSense(true, 100);
            this.setEvade(true, 60);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class A15Entity extends DBSagasEntity {

        public A15Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2f);
            this.setScaleVal(0.8f);
            this.setCanFly(true);

            this.setAllowedCombos(120, ComboType.AIR);


            this.setWildSense(true, 100);
            this.setEvade(true, 60);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class A13Entity extends DBSagasEntity {

        public A13Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2f);
            this.setCanFly(true);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_SMALL, 200, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.1F, 0xEB1E1E, 0x9E1818);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
            this.setEvade(true, 60);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class SuperA13Entity extends DBSagasEntity {

        public SuperA13Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setDBZStyle(0);
            this.setAuraColor(0x22BD94);
            this.setKiBlastSpeed(2.2f);
            this.setScaleVal(1.4f);
            this.setCanFly(true);
            this.setLightning(true);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);


            this.addKiSkill(KiSkillType.KI_SMALL, 200, 1.0F, 0xAA00FF, 0x6C00A3);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.1F, 0xEB1E1E, 0x9E1818);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300 //uff
                    , 0.5F, 0xEB1E1E, 0x9E1818);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
            this.setEvade(true, 60);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class BrolyBaseEntity extends DBSagasEntity {

        public BrolyBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0x3DF54A, 0x0FBF1B);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300 //uff
                    , 0.5F, 0x3DF54A, 0x0FBF1B);

            this.setWildSense(true, 100);
//            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setDefaultAttackSpeed(6.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_BROLY_SSJ_RESTRICTED.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return false;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_broly_base";
        }
    }

    public static class BrolySSJRestringidoEntity extends DBSagasEntity {

        public BrolySSJRestringidoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFEA57);
            this.setKiCharge(true);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0x3DF54A, 0x0FBF1B);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300 //uff
                    , 0.5F, 0x3DF54A, 0x0FBF1B);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setDefaultAttackSpeed(6.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_broly_base";
        }

    }

    public static class ParagusEntity extends DBSagasEntity {

        public ParagusEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0x3DF54A, 0x0FBF1B);

//            this.setWildSense(true, 100);
//            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setDefaultAttackSpeed(6.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class BrolySSJEntity extends DBSagasEntity {

        public BrolySSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFEA57);
            this.setKiBlastSpeed(2.2f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0x3DF54A, 0x0FBF1B);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 400 //uff
                    , 0.5F, 0x3DF54A, 0x0FBF1B);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setDefaultAttackSpeed(6.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class BrolySSJLegendarioEntity extends DBSagasEntity {

        public BrolySSJLegendarioEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x3DF54A);
            this.setKiCharge(true);
            this.setKiBlastSpeed(2.5f);
            this.setScaleVal(1.4f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0x3DF54A, 0x0FBF1B);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 400 //uff
                    , 0.5F, 0x3DF54A, 0x0FBF1B);
            this.addKiSkill(KiSkillType.OOZARU_BEAM, 600, 1.5F, 0x3DF54A, 0x0FBF1B);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 200, 15.5F);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setDefaultAttackSpeed(6.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class ZangyaEntity extends DBSagasEntity {
        public ZangyaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x75B4FF);
            this.setKiBlastSpeed(1.8f);
            this.setAllowedCombos(120, ComboType.AIR, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.0F, 0x75B4FF, 0x3081E3);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public static class GokuaEntity extends DBSagasEntity {
        public GokuaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x75B4FF);
            this.setKiBlastSpeed(1.6f);
            this.setScaleVal(1.2f);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 200, 1.0F, 0x75B4FF, 0x3081E3);
            this.setWildSense(true, 100);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10.0D);
        }
    }

    public static class BidoEntity extends DBSagasEntity {
        public BidoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x75B4FF);
            this.setScaleVal(1.3f);
            this.setKiBlastSpeed(1.5f);
            this.setAllowedCombos(120, ComboType.AIR, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.2F, 0x75B4FF, 0x3081E3);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.50D);
        }
    }

    public static class BujinEntity extends DBSagasEntity {
        public BujinEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x75B4FF);
            this.setScaleVal(0.9f);
            this.setKiBlastSpeed(1.8f);
            this.setAllowedCombos(120, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 1.0F, 0x75B4FF, 0x3081E3);
            this.setWildSense(true, 80);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    public static class BojackEntity extends DBSagasEntity {
        public BojackEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x5EF24E);
            this.setKiBlastSpeed(2.0f);
            this.setScaleVal(1.2f);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 1.2F, 0x5EF24E, 0x2DDE18);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 250, 0.8F, 0x5EF24E, 0x2DDE18);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 2.3F, 0x5EF24E, 0x2DDE18);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.30D);
        }
    }

    public static class BojackFullPowerEntity extends DBSagasEntity {
        public BojackFullPowerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setLightning(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x2DDE18);
            this.setKiBlastSpeed(2.5f);
            this.setScaleVal(1.3f);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 1.5F, 0x5EF24E, 0x2DDE18);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.2F, 0x5EF24E, 0x2DDE18);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 2.3F, 0x5EF24E, 0x2DDE18);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.33D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.50D);
        }
    }


    public static class BioBrolyEntity extends DBSagasEntity {
        public BioBrolyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x7D8A2E);
            this.setKiBlastSpeed(1.5f);
            this.setScaleVal(1.4f);
            this.setAllowedCombos(120, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_SMALL, 100, 1.2F, 0x7D8A2E, 0x4F591A);

            this.setWildSense(false, 0);
            this.setZanzoken(false, 0);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.80D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_bio_broly";
        }

    }

    public static class BioBrolyGiganteEntity extends DBSagasEntity {
        public BioBrolyGiganteEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setAuraColor(0x3DF54A);
            this.setKiBlastSpeed(2.0f);
            this.setScaleVal(4.0f);

            this.setAllowedCombos(150, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300, 1.0F, 0x3DF54A, 0x0FBF1B);
            this.addKiSkill(KiSkillType.OOZARU_BEAM, 500, 1.5F, 0x3DF54A, 0x0FBF1B);

            this.setWildSense(true, 150);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_bio_broly";
        }
    }

    public static class PaikuhanEntity extends DBSagasEntity {
        public PaikuhanEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF5E00);
            this.setKiBlastSpeed(2.2f);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 200, 1.2F, 0xFF5E00, 0xCC4B00);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 2.3F, 0xFF5E00, 0xCC4B00);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 80);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.34D);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(7.0D);
        }
    }

    public static class JanembaGordoEntity extends DBSagasEntity {
        public JanembaGordoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFD700);
            this.setKiBlastSpeed(1.8f);
            this.setScaleVal(5.0f);
            this.setAllowedCombos(100, ComboType.BASIC, ComboType.GUM_PUNCH);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 150, 1.5F, 0xFFD700, 0xCCAC00);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 200, 15.5F);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.24D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.80D);
        }
    }

    public static class SuperJanembaEntity extends DBSagasEntity {
        public SuperJanembaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xC22948);
            this.setKiBlastSpeed(2.5f);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.GUM_PUNCH);
            this.addKiSkill(KiSkillType.KI_LASER, 150, 1.5F, 0xC22948, 0x8C1B32);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 250, 1.2F, 0xC22948, 0x8C1B32);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 200, 15.5F);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 2.3F, 0xC22948, 0x8C1B32);

            this.setWildSense(true, 50);
            this.setZanzoken(true, 40);
            this.setEvade(true, 50);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42D);
            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(8.0D);
        }
    }


    public static class HirudegarnEntity extends DBSagasEntity {
        public HirudegarnEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(false);
            this.setDBZStyle(0);
            this.setAuraColor(0x38104D);
            this.setKiBlastSpeed(1.5f);
            this.setScaleVal(4.0f); // Masivo
            this.setAllowedCombos(100, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_SMALL, 150, 2.0F, 0x38104D, 0x1B0726);
            this.addKiSkill(KiSkillType.OOZARU_BEAM, 300, 1.5F, 0x38104D, 0x1B0726);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 200, 15.5F);

            this.setWildSense(true, 120);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        }
    }

    public static class SuperHirudegarnEntity extends DBSagasEntity {
        public SuperHirudegarnEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0x38104D);
            this.setKiBlastSpeed(2.2f);
            this.setScaleVal(4.0f);
            this.setAllowedCombos(120, ComboType.AIR, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_SMALL, 150, 2.0F, 0x38104D, 0x1B0726);
            this.addKiSkill(KiSkillType.OOZARU_BEAM, 300, 1.5F, 0x38104D, 0x1B0726);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 200, 15.5F);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        }
    }

}
