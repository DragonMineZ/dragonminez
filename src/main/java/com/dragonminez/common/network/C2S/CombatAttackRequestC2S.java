package com.dragonminez.common.network.C2S;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.combat.logic.knockback.ConfigurableKnockback;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.PlayerAttackProperties;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.util.SoundHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.MeleeAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.network.NetworkEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.UUID;
import java.util.function.Supplier;

public class CombatAttackRequestC2S {

	private final int comboCount;
	private final boolean isSneaking;
	private final int selectedSlot;
	private final int[] entityIds;

	public CombatAttackRequestC2S(int comboCount, boolean isSneaking, int selectedSlot, int[] entityIds) {
		this.comboCount = comboCount;
		this.isSneaking = isSneaking;
		this.selectedSlot = selectedSlot;
		this.entityIds = entityIds;
	}

	public CombatAttackRequestC2S(FriendlyByteBuf buffer) {
		this.comboCount = buffer.readInt();
		this.isSneaking = buffer.readBoolean();
		this.selectedSlot = buffer.readInt();
		int length = buffer.readInt();
		this.entityIds = new int[length];
		for (int i = 0; i < length; i++) {
			this.entityIds[i] = buffer.readInt();
		}
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(this.comboCount);
		buffer.writeBoolean(this.isSneaking);
		buffer.writeInt(this.selectedSlot);
		buffer.writeInt(this.entityIds.length);
		for (int id : this.entityIds) {
			buffer.writeInt(id);
		}
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					if (stats.getStatus().isStunned()) return;
					processAttackRequest(player, this);
				});
			}
		});

		ctx.get().setPacketHandled(true);
	}

	public int getComboCount() { return comboCount; }
	public boolean isSneaking() { return isSneaking; }
	public int getSelectedSlot() { return selectedSlot; }
	public int[] getEntityIds() { return entityIds; }

	private static final UUID SWEEPING_MODIFIER_UUID = UUID.fromString("99435a26-9fa8-48b4-a8eb-8438bf228eb3");

	public static void processAttackRequest(ServerPlayer player, CombatAttackRequestC2S request) {
		player.server.execute(() -> {
			int comboCount = request.getComboCount();
			int selectedSlot = request.getSelectedSlot();
			int[] entityIds = request.getEntityIds();

			if (selectedSlot != player.getInventory().selected) {
				LogUtil.warn(Env.SERVER, "Player {} tried to attack with mismatched selected slot", player.getName().getString());
				return;
			}

			((PlayerAttackProperties) player).setComboCount(comboCount);

			var hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);
			if (hand == null) {
				((PlayerAttackProperties) player).setComboCount(-1);
				return;
			}

			if (player.level() instanceof ServerLevel serverLevel) {
				SoundHelper.playSound(serverLevel, player, player, hand.attack().swingSound());
			}

			float cooldownTicks = PlayerAttackHelper.getAttackCooldownTicksCapped(player);
			float animSpeedMultiplier = 12.0F / Math.max(cooldownTicks, 0.001F);
			animSpeedMultiplier = Math.max(0.55F, Math.min(1.35F, animSpeedMultiplier));
			String animName = hand.attack() != null ? hand.attack().animation() : "";
			boolean isOffhand = hand.isOffHand();
			var animPacket = new MeleeAnimationS2C(player.getId(), animName, isOffhand, animSpeedMultiplier);
			NetworkHandler.sendToTrackingEntity(animPacket, player);

			if (hand.isOffHand()) PlayerAttackHelper.setAttributesForOffHandAttack(player, true);

			Multimap<Attribute, AttributeModifier> comboAttributes = null;

			if (hand.attack().damageMultiplier() != 1.0) {
				comboAttributes = HashMultimap.create();
				double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
				double extraDamage = (baseDamage * hand.attack().damageMultiplier()) - baseDamage;
				comboAttributes.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "Combo damage multiplier", extraDamage, AttributeModifier.Operation.ADDITION));
				player.getAttributes().addTransientAttributeModifiers(comboAttributes);
			}

			Multimap<Attribute, AttributeModifier> dualWieldingAttributes = null;
			float dualWieldingMultiplier = PlayerAttackHelper.getDualWieldingAttackDamageMultiplier(player, hand);

			if (dualWieldingMultiplier != 1.0F) {
				dualWieldingAttributes = HashMultimap.create();
				double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
				double extraDamage = (baseDamage * dualWieldingMultiplier) - baseDamage;
				dualWieldingAttributes.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "Dual wielding damage multiplier", extraDamage, AttributeModifier.Operation.ADDITION));
				player.getAttributes().addTransientAttributeModifiers(dualWieldingAttributes);
			}

			Multimap<Attribute, AttributeModifier> sweepingModifiers = HashMultimap.create();
			int sweepingLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWEEPING_EDGE, hand.itemStack());
			if (sweepingLevel > 0) {
				sweepingModifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(SWEEPING_MODIFIER_UUID, "Disable sweeping during attack", -100.0, AttributeModifier.Operation.ADDITION));
				player.getAttributes().addTransientAttributeModifiers(sweepingModifiers);
			}

			boolean firstHit = true;
			for (int id : entityIds) {
				Entity entity = player.level().getEntity(id);
				if (entity == null) continue;

				if (!TargetHelper.isAttackableMount(entity) && player.getVehicle() == entity) continue;

				double maxRange = PlayerAttackHelper.getEffectiveAttackRange(player, hand.attributes().attackRange());
				TargetHelper.Relation relation = TargetHelper.getRelation(player, entity);
				if (!TargetHelper.canAttack(player, entity, maxRange + 4.0D)) continue;

				if (player.distanceToSqr(entity) <= (maxRange * maxRange) + 16.0) {

					if (firstHit) {
						player.getPersistentData().putBoolean("dmz_first_hit", true);
						firstHit = false;
					} else {
						player.getPersistentData().putBoolean("dmz_first_hit", false);
					}

					TargetHelper.onSuccessfulAttack(player, entity, relation);
					player.attack(entity);
				}

				if (entity instanceof LivingEntity livingEntity) {
					((ConfigurableKnockback) livingEntity).setKnockbackMultiplier(1F);
				}
			}

			player.getPersistentData().remove("dmz_first_hit");
			player.resetLastActionTime();

			if (comboAttributes != null) player.getAttributes().removeAttributeModifiers(comboAttributes);
			if (dualWieldingAttributes != null) player.getAttributes().removeAttributeModifiers(dualWieldingAttributes);
			if (hand.isOffHand()) PlayerAttackHelper.setAttributesForOffHandAttack(player, false);
			if (!sweepingModifiers.isEmpty()) {
				player.getAttributes().removeAttributeModifiers(sweepingModifiers);
			}

			((PlayerAttackProperties) player).setComboCount(-1);
		});
	}
}
