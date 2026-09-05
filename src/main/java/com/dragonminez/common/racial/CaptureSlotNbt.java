package com.dragonminez.common.racial;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class CaptureSlotNbt {

	private CaptureSlotNbt() {
	}

	static CompoundTag toNbt(String bonusName, UUID sourceId, String sourceName,
							  Map<String, Integer> grantedStats, long acquiredAtGameTime) {
		CompoundTag tag = new CompoundTag();
		tag.putString("BonusName", bonusName);
		if (sourceId != null) tag.putUUID("SourceId", sourceId);
		tag.putString("SourceName", sourceName == null ? "" : sourceName);
		CompoundTag statsTag = new CompoundTag();
		for (Map.Entry<String, Integer> entry : grantedStats.entrySet()) statsTag.putInt(entry.getKey(), entry.getValue());
		tag.put("GrantedStats", statsTag);
		tag.putLong("AcquiredAtGameTime", acquiredAtGameTime);
		return tag;
	}

	static Fields fromNbt(CompoundTag tag) {
		String bonusName = tag.getString("BonusName");
		UUID sourceId = tag.hasUUID("SourceId") ? tag.getUUID("SourceId") : null;
		String sourceName = tag.getString("SourceName");
		Map<String, Integer> grantedStats = new HashMap<>();
		CompoundTag statsTag = tag.getCompound("GrantedStats");
		for (String key : statsTag.getAllKeys()) grantedStats.put(key, statsTag.getInt(key));
		long acquiredAtGameTime = tag.getLong("AcquiredAtGameTime");
		return new Fields(bonusName, sourceId, sourceName, grantedStats, acquiredAtGameTime);
	}

	record Fields(String bonusName, UUID sourceId, String sourceName, Map<String, Integer> grantedStats,
				  long acquiredAtGameTime) {
	}
}
