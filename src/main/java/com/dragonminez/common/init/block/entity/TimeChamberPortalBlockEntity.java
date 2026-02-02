package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.init.MainBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TimeChamberPortalBlockEntity extends BlockEntity {
	private BlockPos cachedTargetPos = null;

	public TimeChamberPortalBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(MainBlockEntities.TIME_CHAMBER_PORTAL.get(), pPos, pBlockState);
	}

	public boolean hasCachedTarget() {
		return cachedTargetPos != null;
	}

	public BlockPos getCachedTarget() {
		return cachedTargetPos;
	}

	public void setCachedTarget(BlockPos target) {
		this.cachedTargetPos = target;
		this.setChanged();
	}

	@Override
	public void load(CompoundTag pTag) {
		super.load(pTag);
		if (pTag.contains("TargetPos")) this.cachedTargetPos = NbtUtils.readBlockPos(pTag.getCompound("TargetPos"));

	}

	@Override
	protected void saveAdditional(CompoundTag pTag) {
		super.saveAdditional(pTag);
		if (this.cachedTargetPos != null) pTag.put("TargetPos", NbtUtils.writeBlockPos(this.cachedTargetPos));
	}
}