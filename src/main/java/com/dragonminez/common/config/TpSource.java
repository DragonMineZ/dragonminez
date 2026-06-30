package com.dragonminez.common.config;

import com.google.gson.annotations.SerializedName;

public enum TpSource {
	@SerializedName("story") STORY,
	@SerializedName("passive") PASSIVE,
	@SerializedName("travel") TRAVEL,
	@SerializedName("mined") MINED,
	@SerializedName("crafted") CRAFTED,
	@SerializedName("kill") KILL,
	@SerializedName("hit") HIT
}
