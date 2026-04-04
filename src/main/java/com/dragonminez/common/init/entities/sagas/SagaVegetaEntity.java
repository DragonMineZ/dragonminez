package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
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

public class SagaVegetaEntity{

    public static class SagaVegetaExplorerEntity extends DBSagasEntity {

        public SagaVegetaExplorerEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(18000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.2F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.2F, 0xFFF48A, 0xFFF48A);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }

    }

    public static class SagaVegetaNamekEntity extends DBSagasEntity {

        public SagaVegetaNamekEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(24000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }

    }

    public static class SagaVegetaMidBaseEntity extends DBSagasEntity {

        public SagaVegetaMidBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(13000000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_MID_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }


    }

    public static class SagaVegetaMidSSJEntity extends DBSagasEntity {

        public SagaVegetaMidSSJEntity   (EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(650000000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.GALICK_GUN, 200, 1.2F);
            this.addKiSkill(KiSkillType.BIG_BANG, 400, 1.5F, 0xE3FFFF, 0xE3FFFF);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_VEGETA_MID_SSG2.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return false;
        }
    }

    public static class SagaVegetaMidSSG2Entity extends DBSagasEntity {

        public SagaVegetaMidSSG2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(650000000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.BIG_BANG, 200, 1.5F, 0xE3FFFF, 0xE3FFFF);
            this.addKiSkill(KiSkillType.FINAL_FLASH, 400, 2.0F);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta_ssj2";
        }

    }

}