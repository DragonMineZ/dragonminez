package com.dragonminez.common.network;

import com.dragonminez.client.gui.character.PartyStatsCache;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class PartyPackets {

	private PartyPackets() {}

	public record MemberStats(UUID id, String name, boolean online, boolean leader,
							  String race, String characterClass, int level,
							  int strength, int strikePower, int resistance, int vitality,
							  int kiPower, int energy) {

		public void encode(FriendlyByteBuf buf) {
			buf.writeUUID(id);
			buf.writeUtf(name);
			buf.writeBoolean(online);
			buf.writeBoolean(leader);
			buf.writeUtf(race);
			buf.writeUtf(characterClass);
			buf.writeVarInt(level);
			buf.writeVarInt(strength);
			buf.writeVarInt(strikePower);
			buf.writeVarInt(resistance);
			buf.writeVarInt(vitality);
			buf.writeVarInt(kiPower);
			buf.writeVarInt(energy);
		}

		public static MemberStats decode(FriendlyByteBuf buf) {
			return new MemberStats(buf.readUUID(), buf.readUtf(), buf.readBoolean(), buf.readBoolean(),
					buf.readUtf(), buf.readUtf(), buf.readVarInt(),
					buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
					buf.readVarInt(), buf.readVarInt());
		}

		public static MemberStats offline(UUID id, String name, boolean leader) {
			return new MemberStats(id, name, false, leader, "", "", 0, 0, 0, 0, 0, 0, 0);
		}
	}

	public static class RequestStatsC2S {

		public RequestStatsC2S() {}

		public RequestStatsC2S(FriendlyByteBuf buf) {}

		public void encode(FriendlyByteBuf buf) {}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				ServerPlayer player = ctx.get().getSender();
				if (player == null) return;
				NetworkHandler.sendToPlayer(new StatsS2C(collect(player)), player);
			});
			ctx.get().setPacketHandled(true);
		}

		private static List<MemberStats> collect(ServerPlayer viewer) {
			List<MemberStats> roster = new ArrayList<>();
			UUID leaderId = PartyManager.getPartyLeaderId(viewer);

			for (UUID id : PartyManager.getPartyMemberIds(viewer)) {
				ServerPlayer member = viewer.getServer().getPlayerList().getPlayer(id);
				boolean leader = id.equals(leaderId);

				if (member == null) {
					roster.add(MemberStats.offline(id, "", leader));
					continue;
				}

				StatsData data = StatsProvider.get(StatsCapability.INSTANCE, member).orElse(null);
				if (data == null) {
					roster.add(MemberStats.offline(id, member.getGameProfile().getName(), leader));
					continue;
				}

				roster.add(new MemberStats(id, member.getGameProfile().getName(), true, leader,
						data.getCharacter().getRaceName(), data.getCharacter().getCharacterClass(), data.getLevel(),
						data.getStats().getStrength(), data.getStats().getStrikePower(),
						data.getStats().getResistance(), data.getStats().getVitality(),
						data.getStats().getKiPower(), data.getStats().getEnergy()));
			}
			return roster;
		}
	}

	public static class StatsS2C {

		private final List<MemberStats> members;

		public StatsS2C(List<MemberStats> members) {
			this.members = members == null ? new ArrayList<>() : members;
		}

		public List<MemberStats> getMembers() {
			return members;
		}

		public static void encode(StatsS2C msg, FriendlyByteBuf buf) {
			buf.writeVarInt(msg.members.size());
			for (MemberStats member : msg.members) member.encode(buf);
		}

		public static StatsS2C decode(FriendlyByteBuf buf) {
			int size = buf.readVarInt();
			List<MemberStats> members = new ArrayList<>(size);
			for (int i = 0; i < size; i++) members.add(MemberStats.decode(buf));
			return new StatsS2C(members);
		}

		public static void handle(StatsS2C msg, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> PartyStatsCache.accept(msg.getMembers())));
			ctx.get().setPacketHandled(true);
		}
	}
}
