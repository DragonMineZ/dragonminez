package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.alignment.AlignmentBand;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class SagaOgreEntity extends DBSagasEntity {

    private static final int MAX_NEARBY_OGRES = 4;
    private static final double NEARBY_OGRE_RADIUS = 48.0D;

    public SagaOgreEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        this.setCanFly(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DBSagasEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.targetSelector.removeAllGoals(goal -> true);
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, SagaOgreEntity::isEvil));
    }

    private static boolean isEvil(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;

        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (data == null) return false;

        return AlignmentBand.fromValue(data.getResources().getAlignment()) == AlignmentBand.EVIL;
    }

    public static boolean canSpawnInHell(EntityType<? extends SagaOgreEntity> entity, ServerLevelAccessor world, MobSpawnType spawn, BlockPos pos, RandomSource random) {
        BlockState ground = world.getBlockState(pos.below());
        boolean hellGround = ground.is(MainBlocks.HELL_GROUND.get())
                || ground.is(MainBlocks.HELL_STONE.get())
                || ground.is(MainBlocks.HELL_DEEPSTONE.get());
        if (!hellGround) return false;

        return world.getEntitiesOfClass(SagaOgreEntity.class, new AABB(pos).inflate(NEARBY_OGRE_RADIUS)).size() < MAX_NEARBY_OGRES;
    }

    @Override
    public String getGeckolibModelName() {
        return "saga_ogre";
    }
}
