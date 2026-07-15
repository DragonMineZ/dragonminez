package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;

public enum TpSource {
	@SerializedName(value = "story", alternate = "STORY") STORY,
	@SerializedName(value = "passive", alternate = "PASSIVE") PASSIVE,
	@SerializedName(value = "travel", alternate = "TRAVEL") TRAVEL,
	@SerializedName(value = "mined", alternate = "MINED") MINED,
	@SerializedName(value = "crafted", alternate = "CRAFTED") CRAFTED,
	@SerializedName(value = "kill", alternate = "KILL") KILL,
	@SerializedName(value = "hit", alternate = "HIT") HIT
}
