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
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(pTag, registries);
        if (pTag.contains("TargetPos")) {
            this.cachedTargetPos = NbtUtils.readBlockPos(pTag, "TargetPos").orElse(null);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(pTag, registries);
        if (this.cachedTargetPos != null) {
            pTag.put("TargetPos", NbtUtils.writeBlockPos(this.cachedTargetPos));
        }
    }
}