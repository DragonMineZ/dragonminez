package com.dragonminez.common.network.C2S;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.events.players.StatsEvents;
import com.dragonminez.server.util.FusionLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NPCActionC2S {

	private final String npcName;
	private final int actionId;

	public NPCActionC2S(String npcName, int actionId) {
		this.npcName = npcName;
		this.actionId = actionId;
	}

	public NPCActionC2S(FriendlyByteBuf buf) {
		this.npcName = buf.readUtf();
		this.actionId = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(this.npcName);
		buf.writeInt(this.actionId);
	}

	public static void handle(NPCActionC2S packet, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				switch (packet.npcName) {
					case "karin" -> handleKarin(player, data, packet.actionId);
					case "guru" -> handleGuru(player, data, packet.actionId);
					case "dende" -> handleDende(player, data, packet.actionId);
					case "enma" -> handleEnma(player, data, packet.actionId);
					case "baba" -> handleBaba(player, data, packet.actionId);
					case "popo" -> handlePopo(player, data, packet.actionId);
					case "gero" -> handleGero(player, data, packet.actionId);
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}

	private static void handleKarin(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			if (data.getResources().getAlignment() > 50) {
				player.addItem(new ItemStack(MainItems.NUBE_ITEM.get()));
			} else {
				player.addItem(new ItemStack(MainItems.NUBE_NEGRA_ITEM.get()));
			}
		} else if (action == 2) {
			if (!data.getCooldowns().hasCooldown(Cooldowns.SENZU_KARIN)) {
				player.addItem(new ItemStack(MainItems.SENZU_BEAN.get(), 5));
				data.getCooldowns().addCooldown(Cooldowns.SENZU_KARIN, 900 * 20);
			}
		}
	}

	private static void handleGuru(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			if (data.getResources().getAlignment() >= 50 && data.getSkills().getSkillLevel("potentialunlock") == 10) {
				data.getSkills().addSkillLevel("potentialunlock", 1);
			}
		}
	}

	private static void handleDende(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			player.setHealth(player.getMaxHealth());
			data.getResources().setCurrentPoise(data.getMaxPoise());
			data.getResources().setCurrentEnergy(data.getMaxEnergy());
			data.getResources().setCurrentStamina(data.getMaxStamina());
		} else if (action == 2) {
			var stats = data.getStats();
			stats.setStrength(5);
			stats.setStrikePower(5);
			stats.setResistance(5);
			stats.setVitality(5);
			stats.setKiPower(5);
			stats.setEnergy(5);
			if (data.getStatus().isFused()) FusionLogic.endFusion(player, data, false);
			data.getResources().setTrainingPoints(0);
			data.getResources().setRacialSkillCount(0);
			data.getResources().setPowerRelease(0);
			data.getStatus().setAndroidUpgraded(false);
			data.getStatus().setInKaioPlanet(false);
			data.getSkills().removeAllSkills();
			data.getEffects().removeAllEffects();
			data.getCooldowns().clearCooldowns();
			data.getBonusStats().clearAllStats();
			data.getCharacter().clearActiveForm();
			data.getCharacter().clearActiveStackForm();
			data.getCharacter().setHasSaiyanTail(true);
			data.getStatus().setCreatedCharacter(false);

			player.refreshDimensions();
			player.setHealth(20.0F);
			player.getAttribute(Attributes.MAX_HEALTH).removePermanentModifier(StatsEvents.DMZ_HEALTH_MODIFIER_UUID);
			player.setHealth(20.0F);
		}
	}

	private static void handleEnma(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			ServerLevel targetLevel = player.server.getLevel(Level.OVERWORLD);
			if (targetLevel == null) return;
			if (player.getRespawnPosition() != null) player.teleportTo(targetLevel, player.getRespawnPosition().getX(), player.getRespawnPosition().getY(), player.getRespawnPosition().getZ(), player.getYRot(), player.getXRot());
			else player.teleportTo(targetLevel, targetLevel.getSharedSpawnPos().getX(), targetLevel.getSharedSpawnPos().getY(), targetLevel.getSharedSpawnPos().getZ(), player.getYRot(), player.getXRot());
		}
	}

	private static void handleBaba(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			if (!data.getCooldowns().hasCooldown(Cooldowns.REVIVE_BABA)) {
				data.getStatus().setAlive(true);
				player.sendSystemMessage(Component.translatable("gui.dragonminez.lines.baba.revived"));
			}
		}
	}

	private static void handlePopo(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			ServerLevel level = player.serverLevel();
			EntityType<?> entityType = MainEntities.SHADOW_DUMMY.get();
			if (entityType.create(level) instanceof ShadowDummyEntity shadowDummy) {
				shadowDummy.setPos(player.getX(), player.getY(), player.getZ());
				shadowDummy.copyStatsFromPlayer(player);
				shadowDummy.getPersistentData().putString("dmz_quest_owner", player.getStringUUID());
				level.addFreshEntity(shadowDummy);
			}
		}
	}

	private static void handleGero(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			boolean canBeUpgraded = ConfigManager.getRaceCharacter(
					data.getCharacter().getRaceName()
			).getFormSkillTpCosts("androidforms").length > 0;
			if (!canBeUpgraded) {
				player.sendSystemMessage(Component.translatable("message.dragonminez.gero.not_human"));
				return;
			}

			if (data.getStatus().isAndroidUpgraded()) {
				player.sendSystemMessage(Component.translatable("message.dragonminez.gero.already_android"));
				return;
			}

			data.getStatus().setAndroidUpgraded(true);

			data.getSkills().setSkillLevel("androidforms", 1);
			data.getSkills().removeSkill("superform");
			data.getSkills().removeSkill("legendaryforms");
			data.updateTransformationSkillLimits(data.getCharacter().getRaceName());
			data.getCharacter().setSelectedFormGroup("androidforms");
			data.getCharacter().setActiveForm("androidforms", "androidbase");
			player.refreshDimensions();
			player.sendSystemMessage(Component.translatable("message.dragonminez.gero.upgrade_success"));
		}
	}
}