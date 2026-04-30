package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaCellEntity {

    public static class SagaImperfectCellEntity extends DBSagasEntity {

        public SagaImperfectCellEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(750000000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);
            this.setTextureVariant(0);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.ANDROID_ABSORPTION);

            this.addKiSkill(KiSkillType.KAMEHAMEHA, 250, 1.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFF554, 0xFFF554);

            this.setEvade(true, 150);
            this.setWildSense(true, 250);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);

        }
    }

    public static class SagaSemiPerfectCellEntity extends DBSagasEntity {

        public SagaSemiPerfectCellEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(1450000000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFFFF);
            this.setTextureVariant(0);
            this.setKiBlastSpeed(2.0f);
            this.setScaleVal(1.2f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.BIG_BANG, 250, 2.0F, 0xC71414, 0x8F0707);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xB8213D, 0x8C0A22);

            this.setEvade(true, 150);
            this.setWildSense(true, 250);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }
    }

    public static class SagaPerfectCellEntity extends DBSagasEntity {

        public SagaPerfectCellEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(2100000000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFC42);
            this.setTextureVariant(0);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_EXPLOSION, 450, 1.0F, 0x9132D1, 0x5732D1);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 300, 1.5F);
            this.addKiSkill(KiSkillType.MAKANKOSAPPO, 150, 1.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFEF80, 0xFFEF80);

            this.setEvade(true, 150);
            this.setWildSense(true, 250);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.setDefaultMovementSpeed(0.35D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(5.0D);
            this.setDefaultAttackSpeed(5.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);


        }

        @Override
        public String getGeckolibModelName() {
            return "saga_cell_perfect";
        }
    }

    public static class SagaSuperPerfectCellEntity extends DBSagasEntity {

        public SagaSuperPerfectCellEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(2100000000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFC42);
            this.setLightning(true);
            this.setTextureVariant(0);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);

            this.addKiSkill(KiSkillType.KI_EXPLOSION, 450, 1.0F, 0x9132D1, 0x5732D1);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 300, 1.8F);
            this.addKiSkill(KiSkillType.MAKANKOSAPPO, 150, 1.0F);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFEF80, 0xFFEF80);

            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.setDefaultMovementSpeed(0.35D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(8.0D);
            this.setDefaultAttackSpeed(8.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_cell_perfect";
        }
    }

    public static class SagaCellJREntity extends DBSagasEntity {

        public SagaCellJREntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(2100000000);
            }
            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFFFC42);
            this.setTextureVariant(0);
            this.setKiBlastSpeed(2.0f);
            this.setisKid(true);

            this.setAllowedCombos(150, ComboType.AIR, ComboType.BASIC);

            this.addKiSkill(KiSkillType.KAMEHAMEHA, 300, 1.5F);
            this.addKiSkill(KiSkillType.KI_SMALL, 80, 1.0F, 0xFFEF80, 0xFFEF80);

            this.setEvade(true, 150);
            this.setWildSense(true, 250);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);


        }
    }
}
