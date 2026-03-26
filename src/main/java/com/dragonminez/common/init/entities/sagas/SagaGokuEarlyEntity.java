package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class SagaGokuEarlyEntity extends DBSagasEntity{

    public SagaGokuEarlyEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        String entityName = ForgeRegistries.ENTITY_TYPES.getKey(pEntityType).getPath();

        if (entityName != null && entityName.contains("noweights")) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
            this.setCanFly(false);
            this.setEvade(true, 100);
            this.setAuraColor(0xFFFFFF);
            if (this instanceof IBattlePower bp) {
                bp.setBattlePower(416);
            }
        } else {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25D);
            this.setCanFly(true);
            this.setCombo(2, 200);
            this.setWildSense(true, 160);
            this.setAuraColor(0xF52727);
        }

        //KiDamage
        this.setKiBlastDamage(12.0F);
        //KiSpeed
        this.setKiBlastSpeed(1.0F);

        //Primer Estilo
        this.setDBZStyle(0);

        //this.setKiCharge(true);
        //this.setLightning(true);

        //Lanza un kamehameha
        this.setKiHame(true, 160, 20f);

    }

    @Override
    public String getGeckolibModelName() {
        return "saga_goku";
    }
}
