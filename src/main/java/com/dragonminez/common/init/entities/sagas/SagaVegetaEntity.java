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
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

public class SagaVegetaEntity extends DBSagasEntity{

    public SagaVegetaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(18000);
		}

        this.setCanFly(true);
        this.setWildSense(true, 200);
        this.setKiBlastDamage(12.0F);
        this.setKiBlastSpeed(1.3F);
        this.setAuraColor(0XF527AD);
        this.setDBZStyle(0);

        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(pEntityType).getPath();

        this.setCanFly(true);
        this.setWildSense(true, 200);
        this.setKiBlastSpeed(1.3F);
        this.setAuraColor(0XF527AD);
        this.setDBZStyle(0);

        if (entityName != null && entityName.contains("namek")) {

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(24000);
            }
            this.setCombo(0, 140);

            this.setKiVolley(200, 10.0f, 0X00C0FF);

            this.setSecondarySkill(2, 400, 25.0f, 1.8f);

        } else {

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(18000);
            }
            this.setCombo(1, 200);

            this.setMainSkill(2, 300, 15.0f, 1.5f);
        }

    }

    @Override
    public String getGeckolibModelName() {
        return "saga_vegeta";
    }

}