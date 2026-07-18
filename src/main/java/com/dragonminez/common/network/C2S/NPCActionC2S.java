package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.item.WeightItem;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.ShadowDummyEntity;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

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
		this.npcName = buf.readUtf();
		this.actionId = buf.readInt();
		this.value = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeUtf(this.npcName);
		buf.writeInt(this.actionId);
		buf.writeInt(this.value);
	}

	public static void handle(NPCActionC2S packet, Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				boolean shadowDummySpar = "popo".equals(packet.npcName) && packet.actionId == 1;

				Component blocker = NpcDispositionService.getServiceBlocker(player, packet.npcName);
				if (blocker != null) {
					if (shadowDummySpar) {
						LogUtil.warn(Env.SERVER, "Shadow clone FAILED to spawn (service blocked) for player {} at master {}",
								player.getGameProfile().getName(), resolveNearestMasterName(player));
					}
					player.displayClientMessage(blocker, true);
					return;
				}
				if (shadowDummySpar ? !isAnyMasterInRange(player) : !isNpcInRange(player, packet.npcName)) {
					if (shadowDummySpar) {
						LogUtil.warn(Env.SERVER, "Shadow clone FAILED to spawn (no master/quest-NPC in range) for player {}",
								player.getGameProfile().getName());
					}
					return;
				}
				switch (packet.npcName) {
					case "karin" -> handleKarin(player, data, packet.actionId);
					case "guru" -> handleGuru(player, data, packet.actionId);
					case "dende" -> handleDende(player, data, packet.actionId);
					case "enma" -> handleEnma(player, data, packet.actionId);
					case "baba" -> handleBaba(player, data, packet.actionId);
					case "popo" -> handlePopo(player, data, packet.actionId);
					case "gero" -> handleGero(player, data, packet.actionId);
					case "piccolo" -> handlePiccolo(player, data, packet.actionId, packet.value);
					case "roshi" -> { if (packet.actionId == 2) giveWeight(player, packet.value, "message.dragonminez.roshi.weight_given", MainItems.WEIGHT_TURTLE_SHELL.get()); }
					case "kingkai" -> { if (packet.actionId == 2) giveWeight(player, packet.value, "message.dragonminez.kingkai.weight_given", MainItems.WORKOUT_WEIGHTS.get()); }
					case "oldkai" -> handleOldKai(player, data, packet.actionId);
					case "babidi" -> handleBabidi(player, data, packet.actionId);
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		});
		context.setPacketHandled(true);
	}

	private static final double NPC_INTERACTION_RANGE = 8.0;

	private static boolean isNpcInRange(ServerPlayer player, String npcName) {
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

	private static void handleKarin(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			if (data.getResources().getAlignment() > 50) {
				player.addItem(new ItemStack(MainItems.NUBE_ITEM.get()));
			} else {
				player.addItem(new ItemStack(MainItems.NUBE_NEGRA_ITEM.get()));
			}
		} else if (action == 2) {
			if (!data.getCooldowns().hasCooldown(Cooldowns.SENZU_KARIN)) {
				player.addItem(
						new ItemStack(
								MainItems.SENZU_BEAN.get(),
								ConfigManager.getServerConfig().getGameplay().getSenzuGiftAmount()
						)
				);
				data.getCooldowns().addCooldown(
						Cooldowns.SENZU_KARIN,
						ConfigManager.getServerConfig().getGameplay().getSenzuGiftCooldownTicks()
				);
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
			data.resetPlayerProgress(player, null, false, true);
		} else if (action == 3) {
			data.getCharacter().setHasSaiyanTail(!data.getCharacter().isHasSaiyanTail());
		}
	}

	private static void handleEnma(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			if (data.getCooldowns().hasCooldown(Cooldowns.REVIVE_BABA)) {
				int seconds = data.getCooldowns().getCooldown(Cooldowns.REVIVE_BABA) / 20;
				player.sendSystemMessage(Component.translatable("gui.dragonminez.lines.enma.revive", player.getName(), seconds));
				return;
			}
			ServerLevel targetLevel = player.server.getLevel(Level.OVERWORLD);
			if (targetLevel == null) return;
			if (player.getRespawnPosition() != null)
				player.teleportTo(targetLevel, player.getRespawnPosition().getX(), player.getRespawnPosition().getY(), player.getRespawnPosition().getZ(), player.getYRot(), player.getXRot());
			else
				player.teleportTo(targetLevel, targetLevel.getSharedSpawnPos().getX(), targetLevel.getSharedSpawnPos().getY(), targetLevel.getSharedSpawnPos().getZ(), player.getYRot(), player.getXRot());
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
			String master = resolveNearestMasterName(player);
			String playerName = player.getGameProfile().getName();
			ServerLevel level = player.serverLevel();
			EntityType<?> entityType = MainEntities.SHADOW_DUMMY.get();
			if (!(entityType.create(level) instanceof ShadowDummyEntity shadowDummy)) {
				LogUtil.warn(Env.SERVER, "Shadow clone FAILED to spawn (entity creation returned null) for player {} at master {}", playerName, master);
				return;
			}
			shadowDummy.setPos(player.getX(), player.getY(), player.getZ());
			shadowDummy.copyStatsFromPlayer(player);
			shadowDummy.getPersistentData().putString("dmz_quest_owner", player.getStringUUID());
			if (level.addFreshEntity(shadowDummy)) {
				LogUtil.info(Env.SERVER, "Shadow clone spawned for player {} at master {} ({}, {}, {})",
						playerName, master, (int) player.getX(), (int) player.getY(), (int) player.getZ());
			} else {
				shadowDummy.discard();
				LogUtil.warn(Env.SERVER, "Shadow clone FAILED to spawn (addFreshEntity rejected, e.g. protected/spawn-blocked area) for player {} at master {}", playerName, master);
			}
		}
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
			data.getSkills().removeSkill("superforms");
			data.getSkills().removeSkill("legendaryforms");
			data.updateTransformationSkillLimits(data.getCharacter().getRaceName());
			data.getCharacter().setSelectedFormGroup("androidforms");
			data.getCharacter().setSelectedForm("androidbase");
			data.getCharacter().setActiveForm("androidforms", "androidbase");
			data.getCharacter().clearActiveStackForm();
			player.refreshDimensions();
			player.sendSystemMessage(Component.translatable("message.dragonminez.gero.upgrade_success"));
		}
	}

	private static final int MAX_WEIGHT_REQUEST = 100000;

	private static void handlePiccolo(ServerPlayer player, StatsData data, int action, int value) {
		if (action == 1) {
			player.setHealth(player.getMaxHealth());
			data.getResources().setCurrentPoise(data.getMaxPoise());
			data.getResources().setCurrentEnergy(data.getMaxEnergy());
			data.getResources().setCurrentStamina(data.getMaxStamina());
		} else if (action == 2) {
			giveWeight(player, value, "message.dragonminez.piccolo.weight_given", MainItems.WEIGHT_PICCOLO_CAPE.get());
		}
	}

	private static void giveWeight(ServerPlayer player, int value, String messageKey, Item itemStack) {
		int weight = Math.max(1, Math.min(MAX_WEIGHT_REQUEST, value));
		ItemStack weightStack = new ItemStack(itemStack);
		WeightItem.setWeight(weightStack, weight);
		player.addItem(weightStack);
		player.sendSystemMessage(Component.translatable(messageKey, weight));
	}

	private static final String OLDKAI_ZSWORD_COOLDOWN = "OldKaiZSword";

	private static void handleOldKai(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			if (data.getResources().getAlignment() > 61 && data.getSkills().getSkillLevel("potentialunlock") >= 10) {
				data.getSkills().setSkillLevel("ultimate", 1);
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
	}

	private static void handleBabidi(ServerPlayer player, StatsData data, int action) {
		if (action == 1) {
			if (data.getEffects().hasEffect("majin")) {
				player.sendSystemMessage(Component.translatable("message.dragonminez.babidi.already"));
				return;
			}
			if (data.getResources().getAlignment() >= 39) {
				player.sendSystemMessage(Component.translatable("message.dragonminez.babidi.too_good"));
				return;
			}
			data.getEffects().addEffect("majin", ConfigManager.getServerConfig().getGameplay().getMajinPower(), -1);
			player.sendSystemMessage(Component.translatable("message.dragonminez.babidi.marked"));
		}
	}
}
