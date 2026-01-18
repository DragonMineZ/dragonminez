package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.entities.dragon.PorungaEntity;
import com.dragonminez.common.init.entities.dragon.ShenronEntity;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.events.DragonBallsHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
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

			List<Wish> allWishes = WishManager.getAllWishes().get(this.dragonType);
			List<Wish> wishesToGrant = new ArrayList<>();
			if (allWishes != null) {
				for (int index : selectedWishIndices) {
					if (index >= 0 && index < allWishes.size()) {
						wishesToGrant.add(allWishes.get(index));
					}
				}
			}

			for (Wish wish : wishesToGrant) {
				wish.grant(player);
			}

			List<Entity> dragons = level.getEntities(player, player.getBoundingBox().inflate(50),
					e -> e instanceof ShenronEntity || e instanceof PorungaEntity);

			if (!dragons.isEmpty()) {
				Entity dragon = dragons.get(0);

				if (dragon instanceof ShenronEntity Shenron) {
					Shenron.setGrantedWish(true);
				} else if (dragon instanceof PorungaEntity porunga) {
					porunga.setGrantedWish(true);
				}
			}

			boolean isNamek = this.dragonType.equalsIgnoreCase("porunga");
			DragonBallsHandler.scatterDragonBalls(level, isNamek);
		});
		context.get().setPacketHandled(true);
	}
}
