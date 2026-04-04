package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaZFightersEntity {

    public static class SagaKrillinEntity extends DBSagasEntity {

        public SagaKrillinEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);

            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(13000);
            }

            this.setCanFly(true);
            this.setAuraColor(0xFFFFFF);
            this.setKiBlastSpeed(1.4F);
            this.setDBZStyle(0);
            this.setEvade(true, 100);
            this.addKiSkill(KiSkillType.KIENZAN, 100, 1.4F, 0xFFFB73, 0xFFFB73);

        }

        @Override
        public String getGeckolibModelName() {
            return "saga_vegeta";
        }

    }
}
