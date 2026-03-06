package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenRecustomizeS2C;
import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;

public class ReCustomizeWish extends Wish {

	public ReCustomizeWish(String name, String description) {
		super(name, description, "recustomize");
	}

	@Override
	public void grant(ServerPlayer player) {
		NetworkHandler.sendToPlayer(new OpenRecustomizeS2C(), player);
	}

	@Override
	public String toJson() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this, ReCustomizeWish.class);
	}
}