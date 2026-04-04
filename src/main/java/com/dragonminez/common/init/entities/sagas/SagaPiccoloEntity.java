package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaPiccoloEntity{

    public static class SagaPiccoloEarlyEntity extends DBSagasEntity {

        public SagaPiccoloEarlyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(230);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);
            this.setTextureVariant(0); //Ejemplo para cambiar de variante de textura

            this.addKiSkill(KiSkillType.MAKANKOSAPPO, 250, 1.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFF554, 0xFFF554);

            this.setEvade(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_piccolo";
        }
    }

    public static class SagaNailEntity extends DBSagasEntity {

        public SagaNailEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(42000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 250, 1.0F,0xFFF554, 0xFFF554 );
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFF554, 0xFFF554);


            this.setEvade(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_piccolo";
        }
    }

    public static class SagaPiccoloKamiEntity extends DBSagasEntity {

        public SagaPiccoloKamiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(320000000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);
            this.setLightning(true);
            this.setTextureVariant(0);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.KI_CHARGE_ATTACK, ComboType.AIR);

            this.addKiSkill(KiSkillType.MAKANKOSAPPO, 350, 1.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFF554, 0xFFF554);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xFFF554, 0xFFF554);

            this.setEvade(true, 150);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_piccolo";
        }
    }


}
