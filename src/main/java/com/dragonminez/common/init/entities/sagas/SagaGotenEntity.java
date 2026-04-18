package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaGotenEntity {

    public static class SagaGotenKidEntity extends DBSagasEntity {

        public SagaGotenKidEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);


            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setAllowedCombos(200, ComboType.AIR);
            this.setisKid(true);

            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 0.8F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goten";
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_GOTEN_SSJ.get();
        }


        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }
    }

    public static class SagaGotenKidSSJEntity extends DBSagasEntity {

        public SagaGotenKidSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setisKid(true);

            this.setAllowedCombos(120, ComboType.BASIC);

            this.addKiSkill(KiSkillType.KAMEHAMEHA, 200, 0.8F);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 100, 0.8f, 0xFFE657, 0xFFE657);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_goten_ssj";
        }

    }

}
