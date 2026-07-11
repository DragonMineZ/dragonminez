package com.dragonminez.common.network.C2S;

import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.WishManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

public class GrantWishC2S {
	private final String dragonType;
	private final List<Integer> selectedWishIndices;

	public GrantWishC2S(String dragonType, List<Integer> selectedWishIndices) {
		this.dragonType = dragonType;
		this.selectedWishIndices = selectedWishIndices;
	}

	public static void encode(GrantWishC2S msg, FriendlyByteBuf buf) {
		buf.writeUtf(msg.dragonType);
		buf.writeCollection(msg.selectedWishIndices, FriendlyByteBuf::writeInt);
	}

	public static GrantWishC2S decode(FriendlyByteBuf buf) {
		String dragon = buf.readUtf();
		List<Integer> indices = buf.readList(FriendlyByteBuf::readInt);
		return new GrantWishC2S(dragon, indices);
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			if (player == null) return;
			ServerLevel level = player.serverLevel();
			DragonWishEntity dragon = level.getEntitiesOfClass(DragonWishEntity.class,
							player.getBoundingBox().inflate(50.0),
							e -> !e.hasGrantedWish() && e.getOwnerName().equals(player.getName().getString()))
					.stream().findFirst().orElse(null);
			if (dragon == null) return;

			DragonDefinition definition = dragon.getDragonDefinition();
			if (definition == null) return;

			List<Wish> allWishes = WishManager.getAllWishes().get(definition.getWishScreenId());
			if (allWishes == null || allWishes.isEmpty()) return;

			int maxWishes = Math.max(0, definition.getWishCount());
			List<Wish> wishesToGrant = new ArrayList<>();
			for (int index : new LinkedHashSet<>(selectedWishIndices)) {
				if (wishesToGrant.size() >= maxWishes) break;
				if (index >= 0 && index < allWishes.size()) {
					wishesToGrant.add(allWishes.get(index));
				}
			}
			if (wishesToGrant.isEmpty()) return;
			dragon.setGrantedWish(true);

			for (Wish wish : wishesToGrant) {
				wish.grant(player);
			}
		});
		context.get().setPacketHandled(true);
	}
}
