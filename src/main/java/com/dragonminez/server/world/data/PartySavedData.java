package com.dragonminez.server.world.data;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartySavedData extends SavedData {
	private static final String FILE_NAME = "dragonminez_parties";

	private final Map<UUID, PartyInstance> parties = new HashMap<>();
	private final Map<UUID, UUID> playerPartyMap = new HashMap<>();

	public PartySavedData() {
	}

	public static PartySavedData get(MinecraftServer server) {
		DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
		return storage.computeIfAbsent(PartySavedData::load, PartySavedData::new, FILE_NAME);
	}

	public static PartySavedData load(CompoundTag tag) {
		PartySavedData data = new PartySavedData();
		ListTag partiesList = tag.getList("Parties", Tag.TAG_COMPOUND);
		for (int i = 0; i < partiesList.size(); i++) {
			try {
				CompoundTag partyTag = partiesList.getCompound(i);
				if (!partyTag.hasUUID("PartyId")) continue;
				UUID partyId = partyTag.getUUID("PartyId");
				if (data.parties.containsKey(partyId)) continue;

				List<UUID> members = new ArrayList<>();
				ListTag membersList = partyTag.getList("Members", Tag.TAG_COMPOUND);
				for (int j = 0; j < membersList.size(); j++) {
					CompoundTag mTag = membersList.getCompound(j);
					if (!mTag.hasUUID("Id")) continue;
					UUID memberId = mTag.getUUID("Id");
					if (members.contains(memberId) || data.playerPartyMap.containsKey(memberId)) continue;
					members.add(memberId);
				}
				if (members.isEmpty()) continue;

				UUID leaderId = partyTag.hasUUID("LeaderId") ? partyTag.getUUID("LeaderId") : null;
				UUID ownerId = partyTag.hasUUID("OwnerId") ? partyTag.getUUID("OwnerId") : leaderId;
				if (ownerId == null || !members.contains(ownerId)) ownerId = members.get(0);
				if (leaderId == null || !members.contains(leaderId)) leaderId = ownerId;
				boolean pvpEnabled = partyTag.contains("PvpEnabled") && partyTag.getBoolean("PvpEnabled");

				PartyInstance instance = new PartyInstance(partyId, ownerId, leaderId, members, pvpEnabled);
				data.parties.put(partyId, instance);
				for (UUID memberId : members) {
					data.playerPartyMap.put(memberId, partyId);
				}
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "Skipping a malformed party entry in " + FILE_NAME, e);
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag partiesList = new ListTag();
		for (PartyInstance instance : parties.values()) {
			CompoundTag partyTag = new CompoundTag();
			partyTag.putUUID("PartyId", instance.getPartyId());
			partyTag.putUUID("OwnerId", instance.getOwnerId());
			partyTag.putUUID("LeaderId", instance.getLeaderId());
			partyTag.putBoolean("PvpEnabled", instance.isPvpEnabled());

			ListTag membersList = new ListTag();
			for (UUID memberId : instance.getMembers()) {
				CompoundTag mTag = new CompoundTag();
				mTag.putUUID("Id", memberId);
				membersList.add(mTag);
			}
			partyTag.put("Members", membersList);
			partiesList.add(partyTag);
		}
		tag.put("Parties", partiesList);
		return tag;
	}

	public PartyInstance getPartyOf(UUID playerId) {
		UUID partyId = playerPartyMap.get(playerId);
		return partyId != null ? parties.get(partyId) : null;
	}

	public PartyInstance getParty(UUID partyId) {
		return partyId != null ? parties.get(partyId) : null;
	}

	public Collection<PartyInstance> getParties() {
		return Collections.unmodifiableCollection(parties.values());
	}

	public PartyInstance createParty(UUID ownerId) {
		removePlayer(ownerId);
		UUID partyId = UUID.randomUUID();
		PartyInstance party = new PartyInstance(partyId, ownerId, ownerId, List.of(ownerId), false);
		parties.put(partyId, party);
		playerPartyMap.put(ownerId, partyId);
		setDirty();
		return party;
	}

	public void removePlayer(UUID playerId) {
		UUID partyId = playerPartyMap.remove(playerId);
		if (partyId == null) return;
		PartyInstance party = parties.get(partyId);
		if (party != null) {
			party.members.remove(playerId);
			if (party.members.isEmpty()) {
				parties.remove(partyId);
			} else {
				if (playerId.equals(party.ownerId)) party.ownerId = party.members.get(0);
				if (playerId.equals(party.leaderId)) party.leaderId = party.ownerId;
			}
		}
		setDirty();
	}

	public void removeParty(UUID partyId) {
		PartyInstance party = parties.remove(partyId);
		if (party == null) return;
		for (UUID memberId : party.members) {
			playerPartyMap.remove(memberId, partyId);
		}
		setDirty();
	}

	public void addPlayerToParty(UUID partyId, UUID playerId) {
		PartyInstance party = parties.get(partyId);
		if (party == null || party.members.contains(playerId)) return;
		UUID previous = playerPartyMap.get(playerId);
		if (previous != null && !previous.equals(partyId)) removePlayer(playerId);
		party.members.add(playerId);
		playerPartyMap.put(playerId, partyId);
		setDirty();
	}

	public static class PartyInstance {
		private final UUID partyId;
		private UUID ownerId;
		private UUID leaderId;
		private final List<UUID> members;
		@Setter
		private boolean pvpEnabled;

		public PartyInstance(UUID partyId, UUID ownerId, UUID leaderId, List<UUID> members, boolean pvpEnabled) {
			this.partyId = partyId;
			this.ownerId = ownerId;
			this.leaderId = leaderId;
			this.members = new ArrayList<>(members);
			this.pvpEnabled = pvpEnabled;
		}

		public UUID getPartyId() { return partyId; }
		public UUID getOwnerId() { return ownerId; }
		public UUID getLeaderId() { return leaderId; }
		public List<UUID> getMembers() { return Collections.unmodifiableList(members); }
		public boolean isPvpEnabled() { return pvpEnabled; }
		public boolean isMember(UUID playerId) { return playerId != null && members.contains(playerId); }

		public void setOwnerId(UUID ownerId) {
			if (isMember(ownerId)) this.ownerId = ownerId;
		}

		public void setLeaderId(UUID leaderId) {
			if (isMember(leaderId)) this.leaderId = leaderId;
		}
	}
}
