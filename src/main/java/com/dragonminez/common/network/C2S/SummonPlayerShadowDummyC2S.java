package com.dragonminez.common.network.C2S;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.EntitiesEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SummonPlayerShadowDummyC2S {
	public static final String TAG_PLAYER_SHADOW = "dmz_player_shadow";
	public static final String BONUS_KEY = "dmz_player_shadow_dummy";
	public static final UUID SHADOW_HP_MODIFIER_UUID = UUID.fromString("4a9e6f8b-2c1d-4e3f-a5b6-c7d8e9f01234");

	private final int percent;

	public SummonPlayerShadowDummyC2S(int percent) {
		this.percent = percent;
	}

	public SummonPlayerShadowDummyC2S(FriendlyByteBuf buf) {
		this.percent = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(this.percent);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				boolean hasKill = data.getStatus().getShadowDummyKillCount() > 0;
				boolean hasKiControl = data.getSkills().hasSkill("kicontrol");
				boolean hasKiManip = data.getSkills().getSkillLevel("kimanipulation") >= 5;
				if (!hasKill || !hasKiControl || !hasKiManip) {
					player.sendSystemMessage(Component.translatable("gui.dragonminez.shadow_dummy.requirements_not_met"));
					return;
				}

				int pct = net.minecraft.util.Mth.clamp(percent, 25, 75);

				clearPlayerShadowDummy(player, data);

				ServerLevel level = player.serverLevel();
				EntityType<?> entityType = MainEntities.SHADOW_DUMMY.get();
				if (!(entityType.create(level) instanceof ShadowDummyEntity dummy)) return;

				dummy.setPos(player.getX(), player.getY(), player.getZ());
				dummy.copyStatsFromPlayerWithPercent(player, pct);
				dummy.getPersistentData().putString("dmz_quest_owner", player.getStringUUID());
				dummy.getPersistentData().putBoolean(TAG_PLAYER_SHADOW, true);
				dummy.getPersistentData().putInt("dmz_shadow_percent", pct);
				level.addFreshEntity(dummy);

				data.getStatus().setActiveShadowDummyUUID(dummy.getUUID());
				data.getStatus().setShadowDummyPercent(pct);
				applyPenalties(player, data, pct);

				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		ctx.get().setPacketHandled(true);
	}

	public static void clearPlayerShadowDummy(ServerPlayer player, StatsData data) {
		if (!data.getStatus().hasActiveShadowDummy()) return;

		discardDummy(player.getServer(), data.getStatus().getActiveShadowDummyUUID());
		restoreOwner(player, data);
	}

	private static void discardDummy(net.minecraft.server.MinecraftServer server, UUID dummyUUID) {
		if (server == null || dummyUUID == null) return;
		for (ServerLevel level : server.getAllLevels()) {
			net.minecraft.world.entity.Entity e = level.getEntity(dummyUUID);
			if (e != null) {
				e.discard();
				return;
			}
		}
	}

	private static void restoreOwner(ServerPlayer player, StatsData data) {
		removePenalties(player, data);
		data.getStatus().setActiveShadowDummyUUID(null);
		data.getStatus().setShadowDummyPercent(0);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	public static void dismissByDummy(ShadowDummyEntity dummy) {
		if (!dummy.getPersistentData().getBoolean(TAG_PLAYER_SHADOW)) {
			dummy.discard();
			return;
		}
		net.minecraft.server.MinecraftServer server = dummy.getServer();
		ServerPlayer owner = null;
		if (server != null) {
			try {
				owner = server.getPlayerList().getPlayer(UUID.fromString(dummy.getPersistentData().getString("dmz_quest_owner")));
			} catch (Exception ignored) {}
		}
		if (owner != null) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, owner).orElse(null);
			if (data != null && data.getStatus().hasActiveShadowDummy()
					&& dummy.getUUID().equals(data.getStatus().getActiveShadowDummyUUID())) {
				restoreOwner(owner, data);
			}
		}
		dummy.discard();
	}

	public static void applyPenalties(ServerPlayer player, StatsData data, int pct) {
		double factor = 1.0 - (pct / 100.0);

		var bonus = data.getBonusStats();
		bonus.addBonus("STR", BONUS_KEY, "*", factor, true);
		bonus.addBonus("SKP", BONUS_KEY, "*", factor, true);
		bonus.addBonus("PWR", BONUS_KEY, "*", factor, true);
		bonus.addBonus("DEF", BONUS_KEY, "*", factor, true);
		bonus.addBonus("STM", BONUS_KEY, "*", factor, true);

		var maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealthAttr != null) {
			maxHealthAttr.removeModifier(SHADOW_HP_MODIFIER_UUID);
			maxHealthAttr.addPermanentModifier(new AttributeModifier(SHADOW_HP_MODIFIER_UUID, "Shadow Dummy HP Penalty", -(pct / 100.0), AttributeModifier.Operation.MULTIPLY_TOTAL));
			float newMax = (float) maxHealthAttr.getValue();
			if (player.getHealth() > newMax) player.setHealth(newMax);
		}
	}

	public static void removePenalties(ServerPlayer player, StatsData data) {
		data.getBonusStats().removeAllBonuses(BONUS_KEY);

		var maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealthAttr != null) maxHealthAttr.removeModifier(SHADOW_HP_MODIFIER_UUID);
	}
}
