package com.dragonminez.common.init.entities.ai;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class PlayerRead {

    public static final PlayerRead NONE = new PlayerRead(false, false, false, false, false, 0.0F, false, false, false);

    public final boolean present;
    public final boolean blocking;
    public final boolean stunned;
    public final boolean knockedDown;
    public final boolean transforming;
    public final float chargePercent;
    public final boolean casting;
    public final boolean committedCast;
    public final boolean firing;

    private PlayerRead(boolean present, boolean blocking, boolean stunned, boolean knockedDown, boolean transforming,
                       float chargePercent, boolean casting, boolean committedCast, boolean firing) {
        this.present = present;
        this.blocking = blocking;
        this.stunned = stunned;
        this.knockedDown = knockedDown;
        this.transforming = transforming;
        this.chargePercent = chargePercent;
        this.casting = casting;
        this.committedCast = committedCast;
        this.firing = firing;
    }

    public boolean helpless() {
        return this.stunned || this.knockedDown;
    }

    public boolean exposed() {
        return this.helpless() || this.transforming || this.committedCast;
    }

    public static PlayerRead of(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer sp)) return NONE;
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, sp).resolve().orElse(null);
        if (data == null) return NONE;
        boolean casting = data.getTechniques().isTechniqueCharging();
        KiAttackData.KiType chargingType = TechniqueDispatcher.getChargingKiType(data);
        boolean committed = chargingType != null && TechniqueDispatcher.restrictsMovementWhileCharging(chargingType);
        return new PlayerRead(true,
                data.getStatus().isBlocking(),
                data.getStatus().isStunned(),
                data.getStatus().isKnockedDown(),
                data.getStatus().isActionCharging(),
                data.getTechniques().getTechniqueChargePercent(),
                casting,
                committed,
                TechniqueDispatcher.isFiringKiAttack(sp));
    }
}
