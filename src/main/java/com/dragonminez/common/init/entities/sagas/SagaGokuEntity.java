package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class SagaGokuEntity{

    // GOKU EARLY
    public static class SagaGokuEarlyEntity extends DBSagasEntity {

        public SagaGokuEarlyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            String entityName = ForgeRegistries.ENTITY_TYPES.getKey(pEntityType).getPath();

            if (entityName != null && entityName.contains("noweights")) {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
                this.setCanFly(false);
                this.setEvade(true, 100);
                this.setAuraColor(0xFFFFFF);
                if (this instanceof IBattlePower bp) {
                    bp.setBattlePower(416);
                }
            } else {
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
                this.setCanFly(true);
                this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK);
                this.setWildSense(true, 200);
                this.setAuraColor(0xF52727);
            }
            this.setKiBlastSpeed(1.0F);

            this.setDBZStyle(0);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku";
        }

    }

    /*
        GOKU MID [ BASE - SSJ ]
     */

    public static class SagaGokuMidBaseEntity extends DBSagasEntity {

        public SagaGokuMidBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(3000000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);

            this.setWildSense(true, 200);

        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_MID_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return false;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku";
        }

    }

    public static class SagaGokuMidSSJEntity extends DBSagasEntity {

        public SagaGokuMidSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(150000000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setAllowedCombos(150, ComboType.AIR, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);

            this.setWildSense(true, 100);
            this.setKiCharge(true);
        }


        @Override
        public String getGeckolibModelName() {
            return "saga_goku_ssj";
        }

    }

        /*
        GOKU FIN [ BASE - SSJ - SSJ2 - SSJ3]
     */

    public static class SagaGokuEndBaseEntity extends DBSagasEntity {

        public SagaGokuEndBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);
            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.5F, 0x75FFFF, 0x75FFFF);

            this.setWildSense(true, 150);

        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_END_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goku";
        }

    }

    public static class SagaGokuEndSSJEntity extends DBSagasEntity {

        public SagaGokuEndSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 150);
            this.setZanzoken(true, 300);

        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_END_SSJ2.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {return true;}

        @Override
        public String getGeckolibModelName() {
            return "saga_goku_ssj";
        }

    }

    public static class SagaGokuEndSSJ2Entity extends DBSagasEntity {

        public SagaGokuEndSSJ2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setLightning(true);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 1.5F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 150);
            this.setZanzoken(true, 300);

        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOKU_END_SSJ3.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {return true;}


        @Override
        public String getGeckolibModelName() {
            return "saga_goku_ssj2";
        }

    }

    public static class SagaGokuEndSSJ3Entity extends DBSagasEntity {

        public SagaGokuEndSSJ3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setLightning(true);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 40);
            this.setAllowedCombos(200, ComboType.KI_CHARGE_ATTACK, ComboType.AIR, ComboType.METEOR_COMBINATION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 3.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.5F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xFFE657, 0xFFE657);

            this.setWildSense(true, 70);
            this.setZanzoken(true, 200);

        }


        @Override
        public String getGeckolibModelName() {
            return "saga_goku_ssj3";
        }

    }


}
