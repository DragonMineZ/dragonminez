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
                this.setCombo(2, 200);
                this.setWildSense(true, 200);
                this.setAuraColor(0xF52727);
            }
            this.setKiBlastSpeed(1.0F);

            this.setDBZStyle(0);
            this.setMainSkill(1, 200, 1.5f);
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

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setCombo(2, 200);
            this.setMainSkill(1, 200, 1.5f);
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

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setAllowedCombos(150, 1, 3);
            this.setMainSkill(1, 200, 1.5f);
            this.setWildSense(true, 100);
            this.setKiCharge(true);
        }


        @Override
        public String getGeckolibModelName() {
            return "saga_goku_ssj";
        }

    }




}
