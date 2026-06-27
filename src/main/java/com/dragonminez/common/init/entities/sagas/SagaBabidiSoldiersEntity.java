package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SagaBabidiSoldiersEntity {

    public static class SagaSpopovitchEntity extends DBSagasEntity {

        public SagaSpopovitchEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(2);
            this.addKiSkill(KiSkillType.OOZARU_BEAM, 120, 1.0F, 0xBE27F5, 0xBE27F5);

        }

    }

    public static class SagaPuiPuiEntity extends DBSagasEntity {

        public SagaPuiPuiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 170, 1.0F, 0x9E61FF, 0x9E61FF);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0x9E61FF, 0x9E61FF);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4D);

        }

    }

    public static class SagaYakonEntity extends DBSagasEntity {

        public SagaYakonEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(2);
            this.setScaleVal(1.5f);

        }

        @Override
        public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
            boolean hurt = super.doHurtTarget(target);

            if (hurt && target instanceof ServerPlayer serverPlayer) {

                StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(data -> {
                    if (!data.getStatus().isHasCreatedCharacter()) return;

                    double currentKi = data.getResources().getCurrentEnergy();
                    double maxKi = data.getMaxEnergy();

                    double drainAmount = maxKi * 0.03;

                    double newKi = Math.max(0, currentKi - drainAmount);
                    data.getResources().setCurrentEnergy((float) newKi);

                    if (data.getCharacter().hasActiveForm() || data.getCharacter().hasActiveStackForm()) {
                        float dmg = (float) (ConfigManager.getCombatConfig().getBaselineFormDrain() * (data.getTotalMultiplier(
                                data.getMeleeDamage() > data.getKiDamage() ? "STR" : data.getKiDamage() > data.getStrikeDamage() ? "PWR" : "SKP")) / 4);

                        data.getCharacter().clearActiveForm(this);
                        data.getCharacter().clearActiveStackForm(this);

                        this.hurt(damageSources().magic(), dmg);
                    }

                });
            }

            return hurt;
        }

    }

    public static class DaburaEntity extends DBSagasEntity {

        public DaburaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xF52727);
            this.setTextureVariant(0);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 450, 1.0F, 0xF52727, 0xBA1414);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(6.0D);
            this.setDefaultAttackSpeed(6.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MainItems.BRAVE_SWORD.get()));

        }
    }

    public static class BabidiEntity extends DBSagasEntity {

        public BabidiEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(1);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 2.3F, 0x4AD464, 0x32B36E);
        }

    }
}
