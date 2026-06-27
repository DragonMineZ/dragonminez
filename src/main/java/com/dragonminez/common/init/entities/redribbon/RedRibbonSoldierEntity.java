package com.dragonminez.common.init.entities.redribbon;

import com.dragonminez.common.util.BetaWhitelist;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

public class RedRibbonSoldierEntity extends RedRibbonEntity {
    private static final EntityDataAccessor<String> SKIN_OWNER = SynchedEntityData.defineId(RedRibbonSoldierEntity.class, EntityDataSerializers.STRING);
    private static final String NBT_SKIN_OWNER = "SkinOwner";

    public RedRibbonSoldierEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.ATTACK_DAMAGE, 3.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN_OWNER, "");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putString(NBT_SKIN_OWNER, getSkinOwner());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains(NBT_SKIN_OWNER)) setSkinOwner(pCompound.getString(NBT_SKIN_OWNER));
        else rerollSkinOwner();
    }

    public String getSkinOwner() {
        return this.entityData.get(SKIN_OWNER);
    }

    public void setSkinOwner(String skinOwner) {
        this.entityData.set(SKIN_OWNER, skinOwner == null ? "" : skinOwner);
    }

    public void rerollSkinOwner() {
        setSkinOwner(BetaWhitelist.getRandomBetatester(this.random));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        if (getSkinOwner().isEmpty()) rerollSkinOwner();
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }
}
