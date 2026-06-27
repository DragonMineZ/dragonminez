package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaAndroidsEntity {

    public static class SagaDrGeroEntity extends DBSagasEntity {

        public SagaDrGeroEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.ANDROID_ABSORPTION);

            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.2F, 0xFFF48A, 0xFFF48A);
            this.addKiSkill(KiSkillType.KI_LASER, 50, 1.2F, 0xF52727, 0xF52727);

        }

    }

    public static class SagaA19Entity extends DBSagasEntity {

        public SagaA19Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.ANDROID_ABSORPTION);

            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.2F, 0xFFF48A, 0xFFF48A);
            this.addKiSkill(KiSkillType.KI_LASER, 50, 1.2F, 0xF52727, 0xF52727);

        }

    }

    public static class SagaA16Entity extends DBSagasEntity {

        public SagaA16Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setScaleVal(1.2f);

            this.setAllowedCombos(120, ComboType.BASIC);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 400, 3.0F, 0xFFF870, 0xFFF870);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 120, 2.3F, 0xFFF870, 0xFFF870);

        }

    }

    public static class SagaA17Entity extends DBSagasEntity {

        public SagaA17Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.AIR);
            this.addKiSkill(KiSkillType.KIENZAN, 400, 5.0F, 0x4AD464, 0x4AD464);
            this.addKiSkill(KiSkillType.KI_BARRIER, 200, 2.3F, 0x4AD464, 0x32B36E);

        }

    }

    public static class SagaA18Entity extends DBSagasEntity {

        public SagaA18Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFF48A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.setAllowedCombos(120, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KIENZAN, 400, 3.0F, 0xFF70F6, 0xFF70F6);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 2.3F, 0xFF70F6, 0xFF70F6);

        }

    }



}
