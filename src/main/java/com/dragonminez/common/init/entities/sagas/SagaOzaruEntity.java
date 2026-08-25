package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaOzaruEntity extends DBSagasEntity{

    public SagaOzaruEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setCanFly(false);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(180000);
		}
        this.setScaleVal(1.8f);
        this.setKiBlastSpeed(1.5f);
        this.setDBZStyle(4);
        this.addKiSkill(KiSkillType.OOZARU_BEAM, 250, 1.5F, 0xC523DE, 0XAF23DE);
        this.addKiSkill(KiSkillType.OOZARU_ROAR, 500, 15.5F);
        this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(0.25D);

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.ATTACK_SPEED, 4.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.8D, false) {
            @Override
            protected double getAttackReachSqr(LivingEntity pAttackTarget) {
                return 5.0D;
            }
        });
    }

    @Override
    public String getGeckolibModelName() {
        return "saga_ozaru";
    }

    @Override
    public boolean hasHitboxParts() {
        return true;
    }

    @Override
    protected EntityDimensions getCoreDimensions() {
        return EntityDimensions.scalable(3.0F, 4.5F);
    }

    @Override
    protected DBSagasPart[] createHitboxParts() {
        // Bulky great ape (registered 6.5x10). Tune offsets/sizes in-game.
        return new DBSagasPart[] {
                new DBSagasPart(this, "legs", 5.0F, 4.0F, 0.0F, 0.0F, 2.0F),
                new DBSagasPart(this, "torso", 6.0F, 4.0F, 0.0F, 0.0F, 6.0F),
                new DBSagasPart(this, "head", 4.5F, 3.5F, 0.5F, 0.0F, 9.0F)
        };
    }
}