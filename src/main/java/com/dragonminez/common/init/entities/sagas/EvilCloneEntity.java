package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.util.HalloweenEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public abstract class EvilCloneEntity extends DBSagasEntity {

    private static final int KI_COLOR_MAIN = 0xC0141E;
    private static final int KI_COLOR_BORDER = 0x4A0008;

    protected EvilCloneEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(1200);
        }

        this.setCanFly(false);
        this.setAuraColor(KI_COLOR_MAIN);
        this.setKiBlastSpeed(1.2F);
        this.setDBZStyle(0);
        this.addKiSkill(KiSkillType.KI_SMALL, 70, 1.0F, KI_COLOR_MAIN, KI_COLOR_BORDER);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1800.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 90.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(EntityAttributes.KI_BLAST_DAMAGE, 140.0D)
                .add(EntityAttributes.FLY_SPEED, 0.0D)
                .add(EntityAttributes.KI_BLAST_SPEED, 0.6D);
    }

    public static boolean canSpawnAtNight(EntityType<? extends Mob> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.NATURAL && !HalloweenEvent.isActive()) return false;
        if (!Monster.checkMonsterSpawnRules((EntityType<? extends Monster>) type, level, reason, pos, random)) return false;
        return reason != MobSpawnType.NATURAL || level.getLevel().isNight();
    }

    public static class Goku extends EvilCloneEntity {
        public Goku(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
        }

        @Override
        public String getGeckolibModelName() {return "saga_goku_ssj2";}
    }

    public static class Vegeta extends EvilCloneEntity {
        public Vegeta(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
        }

        @Override
        public String getGeckolibModelName() {return "saga_vegeta";}
    }

    public static class Piccolo extends EvilCloneEntity {
        public Piccolo(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
        }

        @Override
        public String getGeckolibModelName() {return "saga_piccolo_kami";}
    }

    public static class Nappa extends EvilCloneEntity {
        public Nappa(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.20D);
            this.setDefaultMovementSpeed(0.20D);
            this.setDBZStyle(1);
        }

        @Override
        public String getGeckolibModelName() {return "saga_nappa";}
    }

    public static class Krillin extends EvilCloneEntity {
        public Krillin(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setisKid(true);
        }

        @Override
        public String getGeckolibModelName() {return "saga_vegeta";}
    }
}
