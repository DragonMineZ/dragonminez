package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities; // Asegúrate de importar tus entidades
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaZarbonEntity extends DBSagasEntity {

    public SagaZarbonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setCanFly(true);
        this.setDBZStyle(0);
        this.setAuraColor(0xFFFFFF);
        this.setKiBlastSpeed(1.4F);

        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(23000);
        }

        this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.0F, 0xFFEB8A, 0xFFEB8A);

        this.setEvade(true, 150);
    }

    @Override
    protected boolean hasTransformation() {
        return true;
    }

    @Override
    public EntityType<? extends DBSagasEntity> getNextTransform() {
        return MainEntities.SAGA_ZARBON_TRANSF.get();
    }

    @Override
    protected boolean spawnsNewFormFullHealth() {
        return false;
    }

    @Override
    public String getGeckolibModelName() {
        return "saga_zarbon";
    }

    public static class SagaZarbonT1Entity extends DBSagasEntity {

        public SagaZarbonT1Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(2);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(30000);
            }
            this.setAllowedCombos(150, ComboType.AIR);
            this.addKiSkill(KiSkillType.OOZARU_BEAM, 200, 1.5F, 0xFFEB8A, 0xFFEB8A);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_zarbont1";
        }
    }
}