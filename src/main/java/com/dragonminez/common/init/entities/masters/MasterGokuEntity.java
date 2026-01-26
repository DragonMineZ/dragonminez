package com.dragonminez.common.init.entities.masters;

import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class MasterGokuEntity extends MastersEntity {

    public MasterGokuEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
		this.masterName = "goku";
    }
}
