package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaOzaruEntity extends DBSagasEntity{

    public SagaOzaruEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setCanFly(false);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(180000);
		}
        this.setKiBlastSpeed(1.5f);
        this.setDBZStyle(2);
        this.setOozaruBeam(400, 0xD627F5, 0xAA06C7, 1.5f);
        this.setSecondarySkill(7, 200, 15.0f); //roar

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.ATTACK_SPEED, 4.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public String getGeckolibModelName() {
        return "saga_ozaru";
    }
}