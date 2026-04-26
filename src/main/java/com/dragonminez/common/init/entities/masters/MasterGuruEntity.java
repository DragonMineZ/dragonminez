package com.dragonminez.common.init.entities.masters;

import com.dragonminez.common.init.entities.MastersEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.level.Level;

public class MasterGuruEntity extends MastersEntity {

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
		if (this.getYRot() != 0.0F) this.setYRot(0.0F);
		if (this.getYHeadRot() != 0.0F) this.setYHeadRot(0.0F);
		if (this.yBodyRot != 0.0F) this.yBodyRot = 0.0F;
		if (this.yHeadRot != 0.0F) this.yHeadRot = 0.0F;
	}
}
