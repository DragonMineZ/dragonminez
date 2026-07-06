package com.dragonminez.common.init.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.level.Level;

public class AllMastersEntity {

    public static class MasterBeerus extends MastersEntity {
        public MasterBeerus(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "beerus";
        }
    }

    public static class MasterWhis extends MastersEntity {
        public MasterWhis(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "whis";
        }
    }

    public static class MasterPiccolo extends MastersEntity {
        public MasterPiccolo(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "piccolo";
        }
    }

    public static class MasterGohan extends MastersEntity {
        public MasterGohan(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "gohan";
        }
    }

    public static class MasterBabidi extends MastersEntity {
        public MasterBabidi(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "babidi";
        }
    }

    public static class MasterOldKai extends MastersEntity {
        public MasterOldKai(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "oldkai";
            this.setScaleVal(0.8f);
        }
    }

    public static class MasterCell extends MastersEntity {
        public MasterCell(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "cell";
            this.setScaleVal(1.1f);
        }
    }

    public static class MasterVegeta extends MastersEntity {
        public MasterVegeta(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "vegeta";
        }
    }

    public static class MasterFrieza extends MastersEntity {
        public MasterFrieza(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "frieza";
            this.setScaleVal(0.9f);
        }
    }

    public static class MasterTrunks extends MastersEntity {
        public MasterTrunks(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "trunks";
        }
    }

    public static class MasterYamcha extends MastersEntity {
        public MasterYamcha(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "yamcha";
        }
    }

    public static class MasterKrillin extends MastersEntity {
        public MasterKrillin(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "krillin";
            this.setScaleVal(0.8f);
        }
    }

    public static class MasterDendeEntity extends MastersEntity {
        public MasterDendeEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "dende";
        }
    }

    public static class MasterEnmaEntity extends MastersEntity {
        public MasterEnmaEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.lookControl = new LookControl(this) {
                @Override
                public void tick() {
                }
            };
            this.masterName = "enma";
        }

        @Override
        public void tick() {
            super.tick();
            if (this.getYRot() != 180.0F) this.setYRot(180.0F);
            if (this.getYHeadRot() != 180.0F) this.setYHeadRot(180.0F);
            if (this.yBodyRot != 180.0F) this.yBodyRot = 180.0F;
            if (this.yHeadRot != 180.0F) this.yHeadRot = 180.0F;
        }
    }

    public static class MasterGeroEntity extends MastersEntity {
        public MasterGeroEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "gero";
        }
    }

    public static class MasterGokuEntity extends MastersEntity {
        public MasterGokuEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "goku";
        }
    }

    public static class MasterGuruEntity extends MastersEntity {
        public MasterGuruEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.lookControl = new LookControl(this) {
                @Override
                public void tick() {
                }
            };
            this.masterName = "guru";
        }

        @Override
        public void tick() {
            super.tick();
            // West-facing yaw (0=south, 90=west, 180=north, 270=east).
            if (this.getYRot() != 90.0F) this.setYRot(90.0F);
            if (this.getYHeadRot() != 90.0F) this.setYHeadRot(90.0F);
            if (this.yBodyRot != 90.0F) this.yBodyRot = 90.0F;
            if (this.yHeadRot != 90.0F) this.yHeadRot = 90.0F;
        }
    }

    public static class MasterKaiosamaEntity extends MastersEntity {
        public MasterKaiosamaEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "kingkai";
        }
    }

    public static class MasterKarinEntity extends MastersEntity {
        public MasterKarinEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "karin";
        }
    }

    public static class MasterPopoEntity extends MastersEntity {
        public MasterPopoEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "popo";
        }
    }

    public static class MasterRoshiEntity extends MastersEntity {
        public MasterRoshiEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "roshi";
        }
    }

    public static class MasterToribotEntity extends MastersEntity {
        public MasterToribotEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "toribot";
        }
    }

    public static class MasterUranaiEntity extends MastersEntity {
        public MasterUranaiEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setPersistenceRequired();
            this.masterName = "baba";
        }
    }




}
