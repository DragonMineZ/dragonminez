package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaPiccoloEntity extends DBSagasEntity{

    public SagaPiccoloEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        if (this instanceof IBattlePower bp) {
            bp.setBattlePower(230);
        }
        //GLOBAL
        this.setCanFly(true);
        this.setDBZStyle(0);
        this.setAuraColor(0xFFFFFF);
        this.setTextureVariant(0); //Ejemplo para cambiar de variante de textura


        this.setKiSmall(80, 0xFFFF00);
        this.setSecondarySkill(3, 250, 1.0f);
        this.setEvade(true, 150);
    }

    @Override
    public String getGeckolibModelName() {
        return "saga_piccolo";
    }
}
