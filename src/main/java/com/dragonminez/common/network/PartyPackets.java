package com.dragonminez.common.network;

import com.dragonminez.client.gui.character.PartyStatsCache;
import com.dragonminez.client.gui.hud.PartyHudCache;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
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

	public static final int HUD_MAX_MEMBERS = 3;
	public static final int SYNC_MAX_MEMBERS = 16;

	public record HudMember(UUID id, String name, float health, float maxHealth, float energy, float maxEnergy,
							boolean sameDimension, float distance, double x, double y, double z, boolean marker, int auraRgb) {

		public void encode(FriendlyByteBuf buf) {
			buf.writeUUID(id);
			buf.writeUtf(name);
			buf.writeFloat(health);
			buf.writeFloat(maxHealth);
			buf.writeFloat(energy);
			buf.writeFloat(maxEnergy);
			buf.writeBoolean(sameDimension);
			buf.writeFloat(distance);
			buf.writeDouble(x);
			buf.writeDouble(y);
			buf.writeDouble(z);
			buf.writeBoolean(marker);
			buf.writeInt(auraRgb);
		}

		public static HudMember decode(FriendlyByteBuf buf) {
			return new HudMember(buf.readUUID(), buf.readUtf(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
					buf.readBoolean(), buf.readFloat(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean(), buf.readInt());
		}
	}

	public static void sendHudSync(ServerPlayer viewer) {
		List<ServerPlayer> party = PartyManager.getAllPartyMembers(viewer);
		if (party.size() <= 1) return;

		StatsData viewerData = StatsProvider.get(StatsCapability.INSTANCE, viewer).orElse(null);
		double markerRange = ConfigManager.getServerConfig().getGameplay().getPartyMarkerRange();
		List<HudMember> members = new ArrayList<>();
		for (ServerPlayer member : party) {
			if (member == viewer) continue;
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, member).orElse(null);
			boolean sameDimension = member.level() == viewer.level();
			float distance = sameDimension ? member.distanceTo(viewer) : Float.MAX_VALUE;
			float energy = data != null ? data.getResources().getCurrentEnergy() : 0.0f;
			float maxEnergy = data != null ? data.getMaxEnergy() : 0.0f;
			boolean marker = sameDimension && distance <= markerRange && isMarkerVisible(viewer, viewerData, member, data);
			int auraRgb = data != null ? parseAuraRgb(data.getCharacter().getAuraColor()) : 0x4FC3FF;
			members.add(new HudMember(member.getUUID(), member.getGameProfile().getName(), member.getHealth(), member.getMaxHealth(),
					energy, maxEnergy, sameDimension, distance,
					marker ? member.getX() : 0.0D, marker ? member.getY() : 0.0D, marker ? member.getZ() : 0.0D, marker, auraRgb));
		}

		members.sort(Comparator.comparingDouble(HudMember::distance));
		if (members.size() > SYNC_MAX_MEMBERS) members = new ArrayList<>(members.subList(0, SYNC_MAX_MEMBERS));
		NetworkHandler.sendToPlayer(new HudSyncS2C(members), viewer);
	}

	private static boolean isMarkerVisible(ServerPlayer viewer, StatsData viewerData, ServerPlayer member, StatsData memberData) {
		if (memberData == null || viewerData == null || !memberData.getStatus().isHasCreatedCharacter()) return false;
		if (member.isSpectator() || !member.isAlive()) return false;
		if (TransformationsHelper.hasAntiKiCloak(member)) return false;
		if (TransformationsHelper.isInstantTransmissionBlocked(viewerData, memberData)) return false;
		return memberData.getResources().getPowerRelease() > 0;
	}

	private static int parseAuraRgb(String hex) {
		if (hex == null || hex.isEmpty()) return 0x4FC3FF;
		try {
			String value = hex.startsWith("#") ? hex.substring(1) : hex;
			return (int) (Long.parseLong(value, 16) & 0xFFFFFF);
		} catch (NumberFormatException e) {
			return 0x4FC3FF;
		}
	}

	public static class HudSyncS2C {

		private final List<HudMember> members;

		public HudSyncS2C(List<HudMember> members) {
			this.members = members == null ? new ArrayList<>() : members;
		}

		public static void encode(HudSyncS2C msg, FriendlyByteBuf buf) {
			buf.writeVarInt(msg.members.size());
			for (HudMember member : msg.members) member.encode(buf);
		}

		public static HudSyncS2C decode(FriendlyByteBuf buf) {
			int size = Math.min(buf.readVarInt(), SYNC_MAX_MEMBERS);
			List<HudMember> members = new ArrayList<>(size);
			for (int i = 0; i < size; i++) members.add(HudMember.decode(buf));
			return new HudSyncS2C(members);
		}

		public static void handle(HudSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> PartyHudCache.accept(msg.members)));
			ctx.get().setPacketHandled(true);
		}
	}
}
