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

            this.setAllowedCombos(150, ComboType.GUM_PUNCH, ComboType.GUM_EXPAND, ComboType.AIR);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 120, 1.0F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.MAJIN_CANDY, 200, 1.0F, 0xFF82F3, 0xFF1AEC);

            this.setEvade(true, 60);
            this.setWildSense(true, 100);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.setDefaultMovementSpeed(0.35D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4.5D);
            this.setDefaultAttackSpeed(4.5D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.45D);

        }

    }

    public static class EvilBuuEntity extends DBSagasEntity {

        public EvilBuuEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF82F3);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.GUM_PUNCH, ComboType.AIR, ComboType.BASIC);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 120, 1.0F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.MAJIN_CANDY, 200, 1.0F, 0xFF82F3, 0xFF1AEC);

            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 250);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.30D);

        }

    }

    public static class SuperBuuEntity extends DBSagasEntity {

        public SuperBuuEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF82F3);
            this.setKiBlastSpeed(2.0f);

            this.setAllowedCombos(150, ComboType.GUM_PUNCH, ComboType.AIR, ComboType.BASIC, ComboType.GUM_EXPAND);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 120, 2.0F, 0xFF82F3, 0xFF1AEC, 0xDE0BCD);

            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300 //uff
                    , 1.5F, 0xFF82F3, 0xFF1AEC);


            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 250);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.setDefaultMovementSpeed(0.35D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4.5D);
            this.setDefaultAttackSpeed(4.5D);


            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.30D);



        }

        @Override
        public String getGeckolibModelName() {
            return "saga_superbuu";
        }
    }

    public static class SuperBuuPiccoloEntity extends DBSagasEntity {

        public SuperBuuPiccoloEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF82F3);
            this.setKiBlastSpeed(2.2f);

            this.setAllowedCombos(150, ComboType.GUM_PUNCH, ComboType.AIR, ComboType.BASIC, ComboType.GUM_EXPAND);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 120, 2.0F, 0xFF82F3, 0xFF1AEC, 0xDE0BCD);
            this.addKiSkill(KiSkillType.MAKANKOSAPPO, 400, 1.5F);
            this.addKiSkill(KiSkillType.DEATH_BALL, 200, 1.2F);


            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 250);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.setDefaultMovementSpeed(0.35D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4.5D);
            this.setDefaultAttackSpeed(4.5D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.30D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_superbuu";
        }
    }

    public static class SuperBuuGotenksEntity extends DBSagasEntity {

        public SuperBuuGotenksEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF82F3);
            this.setKiBlastSpeed(2.2f);

            this.setAllowedCombos(150, ComboType.GUM_PUNCH, ComboType.AIR, ComboType.BASIC, ComboType.GUM_EXPAND,
                    ComboType.METEOR_COMBINATION, ComboType.RAPID_KICKS);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 120, 2.0F, 0xFF82F3, 0xFF1AEC, 0xDE0BCD);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 450, 2.0F);
            this.addKiSkill(KiSkillType.MAKANKOSAPPO, 250, 2.0F);



            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 250);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.setDefaultMovementSpeed(0.35D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4.5D);
            this.setDefaultAttackSpeed(4.5D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.30D);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_superbuu";
        }
    }

    public static class SuperBuuGohanEntity extends DBSagasEntity {

        public SuperBuuGohanEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF82F3);
            this.setKiBlastSpeed(2.2f);
            this.setLightning(true);
            this.setLightningColor(0xDE0B0B);

            this.setAllowedCombos(120, ComboType.GUM_PUNCH, ComboType.AIR, ComboType.BASIC, ComboType.GUM_EXPAND,
                    ComboType.METEOR_COMBINATION, ComboType.KI_CHARGE_ATTACK);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 120, 2.0F, 0xFF82F3, 0xFF1AEC, 0xDE0BCD);
            this.addKiSkill(KiSkillType.KAMEHAMEHA, 450, 2.0F);
            this.addKiSkill(KiSkillType.MASENKO, 250, 2.0F);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 300 //uff
                    , 0.5F, 0xFF82F3, 0xFF1AEC);



            this.setEvade(true, 60);
            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.setDefaultMovementSpeed(0.35D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(4.5D);
            this.setDefaultAttackSpeed(4.5D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.35D);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_superbuu";
        }
    }

    public static class KidBuuEntity extends DBSagasEntity {

        public KidBuuEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setDBZStyle(0);
            this.setAuraColor(0xFF82F3);
            this.setKiBlastSpeed(2.2f);

            this.setAllowedCombos(60, ComboType.GUM_PUNCH, ComboType.AIR, ComboType.BASIC, ComboType.GUM_EXPAND, ComboType.RAPID_KICKS, ComboType.SLEEP_RECOVERY);

            this.addKiSkill(KiSkillType.KI_SMALL, 40, 1.5F, 0xFF82F3, 0xFF1AEC);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 400, 2.0F, 0xFF82F3, 0xFF1AEC, 0xDE0BCD);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 250, 1.5F);
            this.addKiSkill(KiSkillType.BIG_BANG, 850, 5.0F, 0x9E2386, 0xFF52E2, 0xFF00D1);


            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);

            this.setisKid(true);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.45D);
            this.setDefaultMovementSpeed(0.45D);

            this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(8.0D);
            this.setDefaultAttackSpeed(8.0D);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.35D);



        }

    }

}
