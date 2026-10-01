package com.dragonminez.common.network.C2S;

import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.init.entities.dragon.DragonWishEntity;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.common.wish.WishManager;
import io.netty.handler.codec.DecoderException;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class GrantWishC2S {
	private static final int MAX_WISHES = 64;
	private static final int MAX_TARGETS = 256;

	private final int dragonEntityId;
	private final String dragonType;
	private final List<Integer> selectedWishIndices;
	private final List<List<UUID>> selectedTargets;

	public GrantWishC2S(int dragonEntityId, String dragonType, List<Integer> selectedWishIndices, List<List<UUID>> selectedTargets) {
		this.dragonEntityId = dragonEntityId;
		this.dragonType = dragonType;
		this.selectedWishIndices = selectedWishIndices;
		this.selectedTargets = selectedTargets;
	}

	public static void encode(GrantWishC2S msg, FriendlyByteBuf buf) {
		buf.writeVarInt(msg.dragonEntityId);
		buf.writeUtf(msg.dragonType);
		buf.writeVarInt(msg.selectedWishIndices.size());
		for (int i = 0; i < msg.selectedWishIndices.size(); i++) {
			buf.writeInt(msg.selectedWishIndices.get(i));
			List<UUID> targets = i < msg.selectedTargets.size() ? msg.selectedTargets.get(i) : List.of();
			buf.writeCollection(targets, FriendlyByteBuf::writeUUID);
		}
	}

	public static GrantWishC2S decode(FriendlyByteBuf buf) {
		int dragonEntityId = buf.readVarInt();
		String dragon = buf.readUtf();
		int count = buf.readVarInt();
		if (count < 0 || count > MAX_WISHES) throw new DecoderException("GrantWishC2S: invalid wish count " + count);
		List<Integer> indices = new ArrayList<>(count);
		List<List<UUID>> targets = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			indices.add(buf.readInt());
			targets.add(buf.readCollection(FriendlyByteBuf.<List<UUID>>limitValue(ArrayList::new, MAX_TARGETS), FriendlyByteBuf::readUUID));
		}
		return new GrantWishC2S(dragonEntityId, dragon, indices, targets);
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			ServerPlayer player = context.get().getSender();
			if (player == null || StorageManager.isLoadPending(player)) return;
			ServerLevel level = player.serverLevel();
			Entity entity = level.getEntity(dragonEntityId);
			if (!(entity instanceof DragonWishEntity dragon) || dragon.isRemoved() || dragon.hasGrantedWish()) return;
			if (!dragon.getOwnerName().equals(player.getName().getString())) return;
			if (!player.getBoundingBox().inflate(50.0).intersects(dragon.getBoundingBox())) return;

			DragonDefinition definition = dragon.getDragonDefinition();
			if (definition == null || !definition.getWishScreenId().equals(dragonType)) return;

			List<Wish> allWishes = WishManager.getAllWishes().get(definition.getWishScreenId());
			if (allWishes == null || allWishes.isEmpty()) return;

			int maxWishes = Math.max(0, definition.getWishCount());
			List<Wish> wishesToGrant = new ArrayList<>();
			List<List<ServerPlayer>> targetsToGrant = new ArrayList<>();
			Set<Integer> used = new HashSet<>();
			for (int i = 0; i < selectedWishIndices.size(); i++) {
				if (wishesToGrant.size() >= maxWishes) break;
				int index = selectedWishIndices.get(i);
				if (index < 0 || index >= allWishes.size()) continue;
				Wish wish = allWishes.get(index);
				if (!wish.isRepeatable() && !used.add(index)) continue;
				wishesToGrant.add(wish);
				targetsToGrant.add(resolveTargets(player, wish, i < selectedTargets.size() ? selectedTargets.get(i) : List.of()));
			}
			if (wishesToGrant.isEmpty()) return;
			dragon.setGrantedWish(true);

			for (int i = 0; i < wishesToGrant.size(); i++) {
				Wish wish = wishesToGrant.get(i);
				if (wish.getMaxTargets() > 0) wish.grant(player, targetsToGrant.get(i));
				else wish.grant(player);
			}
		});
		context.get().setPacketHandled(true);
	}

	private static List<ServerPlayer> resolveTargets(ServerPlayer wisher, Wish wish, List<UUID> requested) {
		List<ServerPlayer> targets = new ArrayList<>();
		if (wish.getMaxTargets() <= 0) return targets;
		Set<UUID> seen = new HashSet<>();
		for (UUID id : requested) {
			if (targets.size() >= wish.getMaxTargets()) break;
			if (!seen.add(id)) continue;
			ServerPlayer target = wisher.getServer().getPlayerList().getPlayer(id);
			if (target != null && isRevivable(target)) targets.add(target);
		}
		return targets;
	}

	public static boolean isRevivable(ServerPlayer player) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		return data != null && data.getStatus().isHasCreatedCharacter() && !data.getStatus().isAlive();
	}
}
