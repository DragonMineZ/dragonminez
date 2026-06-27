package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public class SagaCuiEntity extends DBSagasEntity {

    public SagaCuiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(18000);
		}

        this.setCanFly(true);
        this.setKiBlastSpeed(1.2F);
        this.setDBZStyle(0);
        this.setEvade(true, 100);

        this.addKiSkill(KiSkillType.KI_VOLLEY, 150, 1.2F, 0xAD27F5, 0xAD27F5);

    }

}