package com.dragonminez.common.init.entities.masters;

import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class MasterKaiosamaEntity extends MastersEntity {

    public MasterKaiosamaEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setPersistenceRequired();
		this.masterName = "kingkai";
    }
}
