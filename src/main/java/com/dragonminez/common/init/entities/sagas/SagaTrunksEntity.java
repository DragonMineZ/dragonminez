package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SagaTrunksEntity {

    public static class SagaFutureTrunksKidBaseEntity extends DBSagasEntity {

        public SagaFutureTrunksKidBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x5CFFFF, 0x5CFFFF);
            this.addKiSkill(KiSkillType.KI_SMALL, 400, 1.2F,0x5CFFFF, 0x5CFFFF );

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_trunks";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_FUTURE_TRUNKS_KID_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

    }

    public static class SagaFutureTrunksKidSSJEntity extends DBSagasEntity {

        public SagaFutureTrunksKidSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.KI_SMALL, 400, 1.2F,0xFFE657, 0xFFE657 );

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_trunks_ssj";
        }

    }

    public static class SagaFutureTrunksBaseEntity extends DBSagasEntity {

        public SagaFutureTrunksBaseEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.BRAVE_SWORD.get()));
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_trunks";
        }


        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_FUTURE_TRUNKS_SSJ.get();
        }

        @Override
        protected boolean spawnsNewFormFullHealth() {
            return true;
        }

    }

    public static class SagaFutureTrunksSSJEntity extends DBSagasEntity {

        public SagaFutureTrunksSSJEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.2F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.MASENKO, 100, 1.2F, 0xFFE657, 0xFFE657);
            this.addKiSkill(KiSkillType.GALICK_GUN, 400, 1.2F);

            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.BRAVE_SWORD.get()));
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_trunks_ssj";
        }

    }

    public static class SagaFutureTrunksSSG3Entity extends DBSagasEntity {

        public SagaFutureTrunksSSG3Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE657);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setKiCharge(true);

            this.addKiSkill(KiSkillType.BIG_BANG, 200, 1.5F, 0x00C0FF, 0x00C0FF);
            this.addKiSkill(KiSkillType.MASENKO, 100, 1.2F, 0xFFE657, 0xFFE657);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.14D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_trunks_ssg3";
        }

    }

}
