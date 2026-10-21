package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.alignment.AlignmentBand;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.item.WeightItem;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.DialogueResultS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.server.util.BabaReviveService;
import com.dragonminez.server.world.dimension.OtherworldTournamentGrounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.function.Supplier;

public class NPCActionC2S {

	private final String npcName;
	private final int actionId;
	private final int value;

	public NPCActionC2S(String npcName, int actionId) {
		this(npcName, actionId, 0);
	}

	public NPCActionC2S(String npcName, int actionId, int value) {
		this.npcName = npcName;
		this.actionId = actionId;
		this.value = value;
	}

	public NPCActionC2S(FriendlyByteBuf buf) {
		this.npcName = buf.readUtf(64);
		this.actionId = buf.readInt();
		this.value = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(this.npcName, 64);
		buf.writeInt(this.actionId);
		buf.writeInt(this.value);
	}

	public static void handle(NPCActionC2S packet, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || StorageManager.isLoadPending(player)) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "npc_action", player.level().getGameTime(), 4L)) {
				reply(player, packet, false);
				return;
			}

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				boolean shadowDummySpar = "popo".equals(packet.npcName) && packet.actionId == 1;

				if (shadowDummySpar) {
					String master = resolveNearestMasterName(player);
					Component blocker = NpcDispositionService.getServiceBlocker(player, master);
					if (blocker != null) {
						LogUtil.warn(Env.SERVER, "Shadow clone FAILED to spawn (training blocked by alignment) for player {} at master {}",
								player.getGameProfile().getName(), master);
						player.displayClientMessage(blocker, true);
						reply(player, packet, false);
						return;
					}
				}
				if (shadowDummySpar ? !isAnyMasterInRange(player) : !isNpcInRange(player, packet.npcName)) {
					if (shadowDummySpar) {
						LogUtil.warn(Env.SERVER, "Shadow clone FAILED to spawn (no master/quest-NPC in range) for player {}",
								player.getGameProfile().getName());
					}
					reply(player, packet, false);
					return;
				}
				boolean success;
				try {
					success = perform(player, data, packet);
				} catch (RuntimeException exception) {
					LogUtil.error(Env.SERVER, "Failed to handle NPC action " + packet.actionId + " of '" + packet.npcName
							+ "' requested by " + player.getGameProfile().getName(), exception);
					success = false;
				}
				StatsSyncS2C.sendRequested(player);
				reply(player, packet, success);
			});
		});
		context.setPacketHandled(true);
	}

	private static boolean perform(ServerPlayer player, StatsData data, NPCActionC2S packet) {
		return switch (packet.npcName) {
			case "karin" -> handleKarin(player, data, packet.actionId);
			case "guru" -> handleGuru(data, packet.actionId);
			case "dende" -> handleDende(player, data, packet.actionId);
			case "enma" -> handleEnma(player, data, packet.actionId);
			case "baba" -> handleBaba(player, data, packet.actionId);
			case "popo" -> handlePopo(player, packet.actionId);
			case "gero" -> handleGero(player, data, packet.actionId);
			case "piccolo" -> handlePiccolo(player, data, packet.actionId, packet.value);
			case "roshi" -> packet.actionId == 2 && giveWeight(player, packet.value, MainItems.WEIGHT_TURTLE_SHELL.get());
			case "kingkai" -> packet.actionId == 2 && giveWeight(player, packet.value, MainItems.WORKOUT_WEIGHTS.get());
			case "babidi" -> handleBabidi(player, data, packet.actionId);
			case "grandkai" -> {
				if (packet.actionId == 1) OtherworldTournamentGrounds.teleportFromGrandKai(player);
				yield packet.actionId == 1;
			}
			case "otherworld_announcer" -> {
				if (packet.actionId == 1) OtherworldTournamentGrounds.teleportBackToGrandKai(player);
				yield packet.actionId == 1;
			}
			default -> false;
		};
	}

	private static void reply(ServerPlayer player, NPCActionC2S packet, boolean success) {
		NetworkHandler.sendToPlayer(new DialogueResultS2C(packet.npcName, DialogueResultS2C.npcAction(packet.actionId), success), player);
	}

	private static final double NPC_INTERACTION_RANGE = 8.0;
	private static final double POPO_DUMMY_CLEANUP_RANGE = 128.0;
	private static final String TAG_POPO_SPAR = "dmz_popo_spar";
	private static final long WEIGHT_REQUEST_COOLDOWN_TICKS = 200L;

	public static boolean isNpcInRange(ServerPlayer player, String npcName) {
		return player.serverLevel().getEntitiesOfClass(MastersEntity.class,
						player.getBoundingBox().inflate(NPC_INTERACTION_RANGE),
						npc -> npcName.equals(npc.getMasterName()))
				.stream().findFirst().isPresent();
	}

	private static boolean isAnyMasterInRange(ServerPlayer player) {
		AABB range = player.getBoundingBox().inflate(NPC_INTERACTION_RANGE);
		boolean hasMaster = !player.serverLevel().getEntitiesOfClass(MastersEntity.class, range,
						npc -> npc.getMasterName() != null && !npc.getMasterName().isBlank())
				.isEmpty();
		if (hasMaster) return true;
		return !player.serverLevel().getEntitiesOfClass(QuestNPCEntity.class, range,
						npc -> npc.getNpcId() != null && !npc.getNpcId().isBlank())
				.isEmpty();
	}

	private static boolean handleKarin(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			if (data.getCooldowns().hasCooldown(Cooldowns.KARIN_NIMBUS)) return false;
			if (player.getInventory().hasAnyOf(Set.of(MainItems.NUBE_ITEM.get(), MainItems.NUBE_NEGRA_ITEM.get()))) return false;
			if (data.getResources().getAlignment() > 50) {
				player.addItem(new ItemStack(MainItems.NUBE_ITEM.get()));
			} else {
				player.addItem(new ItemStack(MainItems.NUBE_NEGRA_ITEM.get()));
			}
			data.getCooldowns().setCooldown(Cooldowns.KARIN_NIMBUS, Integer.MAX_VALUE);
			return true;
		}
		if (action == 2) {
			if (data.getCooldowns().hasCooldown(Cooldowns.SENZU_KARIN)) return false;
			player.addItem(new ItemStack(MainItems.SENZU_BEAN.get(), ConfigManager.getServerConfig().getGameplay().getSenzuGiftAmount()));
			data.getCooldowns().addCooldown(Cooldowns.SENZU_KARIN, ConfigManager.getServerConfig().getGameplay().getSenzuGiftCooldownTicks());
			return true;
		}
		return false;
	}

	private static boolean handleGuru(StatsData data, int action) {
		if (action != 1) return false;
		if (data.getResources().getAlignment() < 50 || data.getBaseSkills().getSkillLevel("potentialunlock") != 10) return false;
		data.grantSkillLevel("potentialunlock", 11);
		return true;
	}

	private static boolean handleDende(ServerPlayer player, StatsData data, int action) {
		if (action == 1) return heal(player, data);
		if (action == 2) {
			data.resetPlayerProgress(player, null, false, true);
			return true;
		}
		if (action == 3) {
			var raceCharacter = ConfigManager.getRaceCharacter(data.getCharacter().getRace());
			if (raceCharacter == null || !raceCharacter.getHasSaiyanTail()) return false;
			data.getCharacter().setHasSaiyanTail(!data.getCharacter().isHasSaiyanTail());
			return true;
		}
		if (action == 4) {
			if (AlignmentBand.fromValue(data.getResources().getAlignment()) != AlignmentBand.GOOD) return false;
			if (data.getCooldowns().hasCooldown(Cooldowns.KAMI_BLESS)) return false;
			var gameplay = ConfigManager.getServerConfig().getGameplay();
			player.addEffect(new MobEffectInstance(MainEffects.KAMI_BLESS.get(), gameplay.getKamiBlessDurationSeconds() * 20, 0, false, false, true));
			data.getCooldowns().setCooldown(Cooldowns.KAMI_BLESS, gameplay.getKamiBlessCooldownSeconds() * 20);
			return true;
		}
		return false;
	}

	private static boolean heal(ServerPlayer player, StatsData data) {
		if (BabaReviveService.isHealingBlocked(data)) return false;
		player.setHealth(player.getMaxHealth());
		data.getResources().setCurrentPoise(data.getMaxPoise());
		data.getResources().setCurrentEnergy(data.getMaxEnergy());
		data.getResources().setCurrentStamina(data.getMaxStamina());
		return true;
	}

	private static boolean handleEnma(ServerPlayer player, StatsData data, int action) {
		if (action != 1 || !BabaReviveService.canEnmaReturn(data)) return false;
		BabaReviveService.handleEnmaReturn(player, data);
		return true;
	}

	private static boolean handleBaba(ServerPlayer player, StatsData data, int action) {
		if (action != 1) return false;
		boolean before = data.getStatus().isAlive() || BabaReviveService.isTempReviveActive(data);
		BabaReviveService.handleBabaRevive(player, data);
		return !before && (data.getStatus().isAlive() || BabaReviveService.isTempReviveActive(data));
	}

	private static boolean handlePopo(ServerPlayer player, int action) {
		if (action == 1) {
			if (!PacketRateLimiter.allow(player.getUUID(), "popo_spar", player.level().getGameTime(), 40L)) return false;
			discardPopoDummies(player);
			String master = resolveNearestMasterName(player);
			String playerName = player.getGameProfile().getName();
			ServerLevel level = player.serverLevel();
			EntityType<?> entityType = MainEntities.SHADOW_DUMMY.get();
			if (!(entityType.create(level) instanceof ShadowDummyEntity shadowDummy)) {
				LogUtil.warn(Env.SERVER, "Shadow clone FAILED to spawn (entity creation returned null) for player {} at master {}", playerName, master);
				return false;
			}
			shadowDummy.setPos(player.getX(), player.getY(), player.getZ());
			shadowDummy.copyStatsFromPlayer(player);
			shadowDummy.getPersistentData().putString("dmz_quest_owner", player.getStringUUID());
			shadowDummy.getPersistentData().putBoolean(TAG_POPO_SPAR, true);
			if (level.addFreshEntity(shadowDummy)) {
				LogUtil.info(Env.SERVER, "Shadow clone spawned for player {} at master {} ({}, {}, {})",
						playerName, master, (int) player.getX(), (int) player.getY(), (int) player.getZ());
				return true;
			}
			shadowDummy.discard();
			LogUtil.warn(Env.SERVER, "Shadow clone FAILED to spawn (addFreshEntity rejected, e.g. protected/spawn-blocked area) for player {} at master {}", playerName, master);
		}
		return false;
	}

	private static void discardPopoDummies(ServerPlayer player) {
		String owner = player.getStringUUID();
		player.serverLevel().getEntitiesOfClass(ShadowDummyEntity.class, player.getBoundingBox().inflate(POPO_DUMMY_CLEANUP_RANGE),
						dummy -> owner.equals(dummy.getPersistentData().getString("dmz_quest_owner"))
								&& dummy.getPersistentData().getBoolean(TAG_POPO_SPAR))
				.forEach(ShadowDummyEntity::discard);
	}

	private static String resolveNearestMasterName(ServerPlayer player) {
		AABB range = player.getBoundingBox().inflate(NPC_INTERACTION_RANGE);
		return player.serverLevel().getEntitiesOfClass(MastersEntity.class, range,
						npc -> npc.getMasterName() != null && !npc.getMasterName().isBlank())
				.stream().map(MastersEntity::getMasterName).findFirst()
				.orElseGet(() -> player.serverLevel().getEntitiesOfClass(QuestNPCEntity.class, range,
								npc -> npc.getNpcId() != null && !npc.getNpcId().isBlank())
						.stream().map(QuestNPCEntity::getNpcId).findFirst()
						.orElse("unknown"));
	}

	private static boolean handleGero(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			boolean canBeUpgraded = ConfigManager.getRaceCharacter(
					data.getCharacter().getRaceName()
			).getFormSkillTpCosts("androidforms").length > 0;
			if (!canBeUpgraded) {
				player.sendSystemMessage(Component.translatable("message.dragonminez.gero.not_human"));
				return false;
			}

			if (data.getStatus().isAndroidUpgraded()) {
				player.sendSystemMessage(Component.translatable("message.dragonminez.gero.already_android"));
				return false;
			}

			if (data.getStatus().isFused() || data.getStatus().getFusionPartnerUUID() != null) {
				player.sendSystemMessage(Component.translatable("message.dragonminez.fusion.action_blocked"));
				return false;
			}

			data.getStatus().setAndroidUpgraded(true);

			data.getSkills().setSkillLevel("androidforms", 1);
			data.getSkills().removeSkill("superforms");
			data.getSkills().removeSkill("legendaryforms");
			data.updateTransformationSkillLimits(data.getCharacter().getRaceName());
			data.getCharacter().setSelectedFormGroup("androidforms");
			data.getCharacter().setSelectedForm("androidbase");
			data.getCharacter().setActiveForm("androidforms", "androidbase");
			data.getCharacter().clearActiveStackForm();
			player.refreshDimensions();
			return true;
		}
		return false;
	}

	private static boolean handlePiccolo(ServerPlayer player, StatsData data, int action, int value) {
		if (action == 1) return heal(player, data);
		if (action == 2) return giveWeight(player, value, MainItems.WEIGHT_PICCOLO_CAPE.get());
		return false;
	}

	private static boolean giveWeight(ServerPlayer player, int value, Item itemStack) {
		if (!PacketRateLimiter.allow(player.getUUID(), "npc_weight", player.level().getGameTime(), WEIGHT_REQUEST_COOLDOWN_TICKS)) return false;
		Double maxWeight = ConfigManager.getServerConfig().getGravity().getMaxWeightRequestable();
		int weight = Math.max(1, Math.min(maxWeight.intValue(), value));
		ItemStack weightStack = new ItemStack(itemStack);
		WeightItem.setWeight(weightStack, weight);
		player.addItem(weightStack);
		return true;
	}

	private static final String OLDKAI_ZSWORD_COOLDOWN = Cooldowns.OLDKAI_ZSWORD;

	public static boolean meetsOldKaiRequirements(StatsData data) {
		return data.getResources().getAlignment() > 61 && data.getBaseSkills().getSkillLevel("potentialunlock") >= 10;
	}

	public static void grantOldKaiChallengeReward(ServerPlayer player, StatsData data) {
		if (meetsOldKaiRequirements(data)) {
			data.grantSkillLevel("ultimate", 1);
			player.sendSystemMessage(Component.translatable("message.dragonminez.oldkai.ultimate"));
		}

		if (data.getCooldowns().hasCooldown(OLDKAI_ZSWORD_COOLDOWN)) {
			return;
		}

		ItemStack stack = new ItemStack(MainItems.Z_SWORD.get(), 1);
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity drop = player.drop(stack, false);
			if (drop != null) drop.setNoPickUpDelay();
		}
		data.getCooldowns().addCooldown(OLDKAI_ZSWORD_COOLDOWN, Integer.MAX_VALUE);
	}

	private static boolean handleBabidi(ServerPlayer player, StatsData data, int action) {
		if (action != 1) return false;
		if (data.getEffects().hasEffect("majin")) {
			player.sendSystemMessage(Component.translatable("message.dragonminez.babidi.already"));
			return false;
		}
		if (data.getResources().getAlignment() >= 39) {
			player.sendSystemMessage(Component.translatable("message.dragonminez.babidi.too_good"));
			return false;
		}
		data.getEffects().addEffect("majin", ConfigManager.getServerConfig().getGameplay().getMajinPower(), -1);
		return true;
	}
}
