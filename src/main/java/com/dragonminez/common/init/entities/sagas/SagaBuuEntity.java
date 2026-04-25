package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaBuuEntity {

    public static class BuuFatEntity extends DBSagasEntity {

        public BuuFatEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF82F3);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 120, 1.0F, 0xFF82F3, 0xFF1AEC);

            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.30D);

        }

    }

}
