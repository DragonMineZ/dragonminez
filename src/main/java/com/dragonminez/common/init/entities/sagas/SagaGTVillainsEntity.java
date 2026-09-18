package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.MainEntities;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaGTVillainsEntity {

    /*
        BLACK STAR DRAGON BALLS [ LEDGIC - PARA PARA BROTHERS - LUUD - RILLDO ]
     */

    public static class LedgicEntity extends DBSagasEntity {

        public LedgicEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x8A5CFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(1);
            this.setScaleVal(1.1F);
            this.setEvade(true, 120);
            this.setAllowedCombos(160, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_SMALL, 60, 1.3F, 0xE1BEE7, 0x8E24AA);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 220, 1.3F, 0xE1BEE7, 0x8E24AA);

            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.25D);
        }
    }

    /** Bon, Don and Son Para: three registry entries on one model, told apart by their textures. */
    public static class ParaParaEntity extends DBSagasEntity {

        public ParaParaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFD166);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.setAllowedCombos(160, ComboType.BASIC, ComboType.RAPID_KICKS);
            this.addKiSkill(KiSkillType.KI_SMALL, 70, 1.1F, 0xFFF3C4, 0xFFD166);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            this.setDefaultMovementSpeed(0.3D);
        }

        @Override
        public String getGeckolibModelName() {
            return "saga_para_para";
        }
    }

    public static class LuudEntity extends DBSagasEntity {

        public LuudEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setAuraColor(0x9BE7C4);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(1);
            this.setScaleVal(5.0F);
            this.addKiSkill(KiSkillType.KI_LASER, 160, 2.5F, 0xFFFFFF, 0xFF1744);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 2.0F, 0xE0FFF4, 0x4DD9A5);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 300, 15.5F);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2D);
            this.setDefaultMovementSpeed(0.2D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        }

        @Override
        public boolean hasHitboxParts() {
            return true;
        }

        @Override
        protected EntityDimensions getCoreDimensions() {
            return EntityDimensions.scalable(2.5F, 5.0F);
        }

        @Override
        protected DBSagasPart[] createHitboxParts() {
            // Sized for a ~2-block humanoid at 5x (~10 blocks). Tune once Luud's model exists.
            return new DBSagasPart[] {
                    new DBSagasPart(this, "legs", 3.0F, 3.75F, 0.0F, 0.0F, 1.9F),
                    new DBSagasPart(this, "torso", 3.5F, 3.75F, 0.0F, 0.0F, 5.6F),
                    new DBSagasPart(this, "head", 3.0F, 2.5F, 0.0F, 0.0F, 8.75F)
            };
        }
    }

    public static class RilldoEntity extends DBSagasEntity {

        public RilldoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xC0C0C0);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(2);
            this.setEvade(true, 80);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xF5F5F5, 0x9E9E9E);
            this.addKiSkill(KiSkillType.KI_LASER, 180, 1.4F, 0xF5F5F5, 0x9E9E9E);

            this.setWildSense(true, 150);
        }
    }

    public static class MetalRilldoEntity extends DBSagasEntity {

        public MetalRilldoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xB0C4DE);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(2);
            this.setScaleVal(1.1F);
            this.setEvade(true, 70);
            this.setAllowedCombos(140, ComboType.BASIC, ComboType.AIR, ComboType.ANDROID_ABSORPTION);
            this.addKiSkill(KiSkillType.KI_LASER, 160, 1.6F, 0xECEFF1, 0x607D8B);
            this.addKiSkill(KiSkillType.KI_BARRIER, 260, 2.3F, 0xECEFF1, 0x90A4AE);

            this.setWildSense(true, 120);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
        }
    }

    public static class HyperRilldoEntity extends DBSagasEntity {

        public HyperRilldoEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x8FA3B8);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(1);
            this.setScaleVal(2.0F);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_LASER, 160, 2.0F, 0xECEFF1, 0x607D8B);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 360, 1.5F, 0xECEFF1, 0x607D8B);
            this.addKiSkill(KiSkillType.OOZARU_ROAR, 400, 10.0F);

            this.setWildSense(true, 150);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.24D);
            this.setDefaultMovementSpeed(0.24D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8D);
        }
    }

    /*
        BABY [ SUPER BABY VEGETA 1 - SUPER BABY VEGETA 2 - GOLDEN GREAT APE | TRUE FORM ]
     */

    public static class SuperBabyVegetaEntity extends DBSagasEntity {

        public SuperBabyVegetaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xF2C12E);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(2);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xFFF3C4, 0xF2C12E);
            this.addKiSkill(KiSkillType.GALICK_GUN, 300, 1.6F);
            this.addKiSkill(KiSkillType.BIG_BANG, 380, 1.7F);

            this.setWildSense(true, 120);
            this.setZanzoken(true, 250);
        }
    }

    public static class SuperBabyVegeta2Entity extends DBSagasEntity {

        public SuperBabyVegeta2Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xF2C12E);
            this.setLightning(true);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(2);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xFFF3C4, 0xF2C12E);
            // Revenge Final Flash / Revenge Death Ball
            this.addKiSkill(KiSkillType.FINAL_FLASH, 380, 2.2F);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 2.0F, 0xFFB3B3, 0xFF2E2E);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 180);
        }

        @Override
        protected boolean hasTransformation() {
            return true;
        }

        @Override
        public EntityType<? extends DBSagasEntity> getNextTransform() {
            return MainEntities.SAGA_BABY_GOLDEN_OZARU.get();
        }
    }

    /** Reuses the great ape model and attributes; the golden look is only its texture. */
    public static class BabyGoldenOzaruEntity extends SagaOzaruEntity {

        public BabyGoldenOzaruEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setAuraColor(0xFFC94A);
            this.addKiSkill(KiSkillType.DEATH_BALL, 500, 3.0F, 0xFFB3B3, 0xFF2E2E);
        }
    }

    public static class BabyEntity extends DBSagasEntity {

        public BabyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x9C27B0);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(2);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.ANDROID_ABSORPTION, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_SMALL, 50, 1.3F, 0xE9D2FF, 0x9C27B0);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.4F, 0xE9D2FF, 0x9C27B0);
            this.addKiSkill(KiSkillType.DEATH_BALL, 450, 1.6F, 0xE9D2FF, 0x6A1B9A);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 200);
        }
    }

    /*
        SUPER 17
     */

    public static class Super17Entity extends DBSagasEntity {

        public Super17Entity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x2E8B57);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(0);
            this.setScaleVal(1.05F);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.ANDROID_ABSORPTION, ComboType.BASIC, ComboType.AIR);
            // Super Electric Strike / Hell's Storm barrier
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 300, 2.2F, 0xE8F5E9, 0x00C853);
            this.addKiSkill(KiSkillType.KI_BARRIER, 240, 2.3F, 0xB9F6CA, 0x00E676);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xE8F5E9, 0x00C853);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 200);
        }
    }

    /*
        SHADOW DRAGONS [ LIANG - WU - LIU - QI - NEO (SI) - EIS (SAN) - SYN (YI) - OMEGA ]
     */

    /** Two-Star Dragon (Haze Shenron): pollution and poison smog. */
    public static class LiangXingLongEntity extends DBSagasEntity {

        public LiangXingLongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x7B8B3A);
            this.setKiBlastSpeed(1.5F);
            this.setDBZStyle(2);
            this.setScaleVal(1.1F);
            this.setEvade(true, 80);
            this.setAllowedCombos(150, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 280, 1.8F, 0xDCE775, 0x827717);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 400, 1.3F, 0xC0CA33, 0x616A1E);

            this.setWildSense(true, 120);
        }
    }

    /** Five-Star Dragon (Rage Shenron): electricity. */
    public static class WuXingLongEntity extends DBSagasEntity {

        public WuXingLongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFFE45C);
            this.setLightning(true);
            this.setLightningColor(0xFFEB3B);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(1);
            this.setScaleVal(1.2F);
            this.setAllowedCombos(130, ComboType.BASIC, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.KI_LASER, 150, 1.6F, 0xFFFDE7, 0xFFEB3B);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.5F, 0xFFFDE7, 0xFFEB3B);

            this.setWildSense(true, 120);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
        }
    }

    /** Six-Star Dragon (Oceanus Shenron): wind and typhoons. */
    public static class LiuXingLongEntity extends DBSagasEntity {

        public LiuXingLongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0x4FC3F7);
            this.setKiBlastSpeed(1.6F);
            this.setDBZStyle(2);
            this.setEvade(true, 60);
            this.setAllowedCombos(140, ComboType.AIR, ComboType.RAPID_KICKS);
            this.addKiSkill(KiSkillType.BLUE_HURRICANE, 400, 1.5F);
            this.addKiSkill(KiSkillType.KI_AIR_VOLLEY, 260, 0.8F, 0xE0F7FA, 0x26C6DA);

            this.setWildSense(true, 100);
            this.setZanzoken(true, 250);
        }
    }

    /** Seven-Star Dragon (Naturon Shenron): absorbs whatever it touches. */
    public static class QiXingLongEntity extends DBSagasEntity {

        public QiXingLongEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(false);
            this.setAuraColor(0x6D8B3A);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(1);
            this.setScaleVal(1.3F);
            this.setAllowedCombos(140, ComboType.ANDROID_ABSORPTION, ComboType.BASIC);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 220, 1.6F, 0xD7CCC8, 0x6D4C41);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 420, 1.3F, 0xD7CCC8, 0x6D4C41);

            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22D);
            this.setDefaultMovementSpeed(0.22D);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.7D);
        }
    }

    /** Four-Star Dragon (Si Xing Long / Nuova Shenron): fire and the Nova Star. */
    public static class NeoShenronEntity extends DBSagasEntity {

        public NeoShenronEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xFF7A1A);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(0);
            this.setScaleVal(1.1F);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 260, 1.8F, 0xFFE0B2, 0xFF6D00);
            this.addKiSkill(KiSkillType.KI_EXPLOSION, 380, 1.4F, 0xFFCC80, 0xFF3D00);
            this.addKiSkill(KiSkillType.DEATH_BALL, 480, 1.8F, 0xFFF3E0, 0xFF9100);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 220);
        }
    }

    /** Three-Star Dragon (San Xing Long / Eis Shenron): ice. */
    public static class EisShenronEntity extends DBSagasEntity {

        public EisShenronEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xB3E5FC);
            this.setKiBlastSpeed(1.7F);
            this.setDBZStyle(2);
            this.setScaleVal(1.1F);
            this.setEvade(true, 50);
            this.setAllowedCombos(130, ComboType.BASIC, ComboType.AIR);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 180, 1.6F, 0xE1F5FE, 0x81D4FA);
            this.addKiSkill(KiSkillType.KI_LASER, 170, 1.6F, 0xE1F5FE, 0x4FC3F7);
            this.addKiSkill(KiSkillType.KI_BARRIER, 260, 2.3F, 0xE1F5FE, 0x4FC3F7);

            this.setWildSense(true, 80);
            this.setZanzoken(true, 220);
        }
    }

    /** One-Star Dragon (Yi Xing Long / Syn Shenron) before he swallows the other Dragon Balls. */
    public static class SynShenronEntity extends DBSagasEntity {

        public SynShenronEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xB71C1C);
            this.setLightning(true);
            this.setLightningColor(0xFF5252);
            this.setKiBlastSpeed(1.8F);
            this.setDBZStyle(2);
            this.setScaleVal(1.2F);
            this.setEvade(true, 40);
            this.setAllowedCombos(120, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK);
            // Dragon Thunder / Negative Karma Ball
            this.addKiSkill(KiSkillType.KI_LASER, 150, 1.8F, 0xFFEBEE, 0xD50000);
            this.addKiSkill(KiSkillType.KI_VOLLEY, 200, 1.6F, 0xFFCDD2, 0xB71C1C);
            this.addKiSkill(KiSkillType.DEATH_BALL, 460, 2.0F, 0x311B92, 0xB71C1C);

            this.setWildSense(true, 60);
            this.setZanzoken(true, 180);
        }
    }

    /** Syn Shenron after absorbing the Dragon Balls: every Shadow Dragon's power at once. */
    public static class OmegaShenronEntity extends DBSagasEntity {

        public OmegaShenronEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            this.setCanFly(true);
            this.setAuraColor(0xC62828);
            this.setLightning(true);
            this.setLightningColor(0xFF5252);
            this.setKiBlastSpeed(2.0F);
            this.setDBZStyle(2);
            this.setScaleVal(1.3F);
            this.setEvade(true, 30);
            this.setAllowedCombos(100, ComboType.BASIC, ComboType.AIR, ComboType.KI_CHARGE_ATTACK, ComboType.METEOR_COMBINATION);
            // Minus Energy Power Ball, Dragon Thunder, Ice Slash, Dragon Typhoon, Nova Star
            this.addKiSkill(KiSkillType.DEATH_BALL, 420, 3.0F, 0x1A0033, 0x7B1FA2);
            this.addKiSkill(KiSkillType.KI_LASER, 150, 2.0F, 0xFFEBEE, 0xD50000);
            this.addKiSkill(KiSkillType.KI_BARRIER, 280, 2.3F, 0xE1F5FE, 0x4FC3F7);
            this.addKiSkill(KiSkillType.BLUE_HURRICANE, 420, 1.5F);
            this.addKiSkill(KiSkillType.GENERIC_KI_WAVE, 260, 2.0F, 0xFFE0B2, 0xFF6D00);

            this.setWildSense(true, 40);
            this.setZanzoken(true, 120);
            this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5D);
        }
    }
}
