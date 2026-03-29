package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities; // Asegúrate de importar tus entidades
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaZarbonEntity extends DBSagasEntity {

    private int transformTick = 0;

    public SagaZarbonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setCanFly(true);
        this.setDBZStyle(0);
        this.setAuraColor(0xFFFFFF);

        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(23000);
        }

        this.setKiSmall(60, 0xFFEB8A);
        this.setEvade(true, 150);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isTransforming()) {
            this.transformTick++;
            boolean finished = this.handleTransformationLogic(this.transformTick, 80);

            if (finished && !this.level().isClientSide) {
                DBSagasEntity monsterForm = (DBSagasEntity) MainEntities.SAGA_ZARBON_TRANSF.get().create(this.level());
                this.finishTransformationSpawn(monsterForm, true);
            }
        }
    }

    @Override
    protected boolean shouldTriggerTransformationOnDeath() {
        return true;
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

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(30000);
            }

            this.setCombo(1, 150);
            this.setOozaruBeam(200, 0xFFEB8A, 0xFFEB8A, 1.5f);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_zarbont1";
        }
    }
}