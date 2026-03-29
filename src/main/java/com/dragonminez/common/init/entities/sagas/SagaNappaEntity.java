package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public class SagaNappaEntity extends DBSagasEntity{

    public SagaNappaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(4000);
		}
        this.setCanFly(true);
        this.setWildSense(true, 200);
        this.setKiBlastSpeed(1.3F);
        this.setOozaruBeam(200, 0xF527C2, 0xF5277D, 1.5f);
        this.setAuraColor(0XF527AD);
        this.setDBZStyle(1);

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.20D);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

    }


}