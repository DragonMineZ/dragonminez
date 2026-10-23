package com.dragonminez.common.network.S2C;

import com.dragonminez.common.network.ClientPacketHandler;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.StatsDeltaSync;
import com.dragonminez.common.network.StatsGroupsPacket;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.SkinPixels;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class StatsSyncS2C implements StatsGroupsPacket {

    private static final long REQUESTED_SYNC_MIN_INTERVAL_TICKS = 2L;
    private static final String QUEST_DATA_KEY = "PlayerQuestData";
    private static final String QUEST_STATE_KEY = "questState";

    private final int playerId;
    private final CompoundTag nbt;

    public StatsSyncS2C(ServerPlayer player) {
        this.playerId = player.getId();
        this.nbt = new CompoundTag();

        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            this.nbt.merge(data.save());
            this.nbt.getCompound("Character").remove(SkinPixels.NBT_KEY);
        });
    }

    public static void sendRequested(ServerPlayer player) {
        if (!PacketRateLimiter.allow(player.getUUID(), "requested_stats_sync", player.level().getGameTime(), REQUESTED_SYNC_MIN_INTERVAL_TICKS)) {
            StatsDeltaSync.reset(player);
            return;
        }
        NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
    }

    public StatsSyncS2C(int playerId, CompoundTag nbt) {
        this.playerId = playerId;
        this.nbt = nbt;
    }

    @Override
    public int playerId() {
        return this.playerId;
    }

    @Override
    public CompoundTag groups() {
        return this.nbt;
    }

    public StatsSyncS2C forTrackers() {
        return new StatsSyncS2C(this.playerId, withoutQuestHistory(this.nbt));
    }

    public static CompoundTag withoutQuestHistory(CompoundTag nbt) {
        if (!nbt.contains(QUEST_DATA_KEY, Tag.TAG_COMPOUND)) return nbt;
        CompoundTag questData = shallowCopy(nbt.getCompound(QUEST_DATA_KEY));
        if (!questData.contains(QUEST_STATE_KEY, Tag.TAG_COMPOUND)) return nbt;
        questData.put(QUEST_STATE_KEY, new CompoundTag());
        CompoundTag stripped = shallowCopy(nbt);
        stripped.put(QUEST_DATA_KEY, questData);
        return stripped;
    }

    private static CompoundTag shallowCopy(CompoundTag source) {
        CompoundTag copy = new CompoundTag();
        for (String key : source.getAllKeys()) copy.put(key, source.get(key));
        return copy;
    }

    public static void encode(StatsSyncS2C msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerId);
        buf.writeNbt(msg.nbt);
    }

    public static StatsSyncS2C decode(FriendlyByteBuf buf) {
        return new StatsSyncS2C(
                buf.readInt(),
                buf.readNbt()
        );
    }

    public static void handle(StatsSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStatsSyncPacket(msg.playerId, msg.nbt));
        });
        ctx.get().setPacketHandled(true);
    }
}
