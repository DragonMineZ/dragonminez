package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FlyToggleC2S {

    private final boolean fromGround;

    public FlyToggleC2S(boolean fromGround) {
        this.fromGround = fromGround;
    }

    public FlyToggleC2S(FriendlyByteBuf buf) {
        this.fromGround = buf.readBoolean();
    }

    public static void encode(FlyToggleC2S msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.fromGround);
    }

    public static FlyToggleC2S decode(FriendlyByteBuf buf) {
        return new FlyToggleC2S(buf.readBoolean());
    }

    public static void handle(FlyToggleC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                Skill flySkill = data.getSkills().getSkill("fly");
                if (flySkill == null || flySkill.getLevel() <= 0) return;

                int flyLevel = flySkill.getLevel();
                int maxEnergy = data.getMaxEnergy();

                double energyCostPercent = Math.max(0.01, 0.04 - (flyLevel * 0.003));
                int energyCost = (int) Math.ceil(maxEnergy * energyCostPercent);

                if (!flySkill.isActive()) {
                    if (data.getResources().getCurrentEnergy() < energyCost) {
                        return;
                    }
                }

                flySkill.setActive(!flySkill.isActive());

                if (flySkill.isActive()) {
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                } else {
                    if (!player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                }

                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
