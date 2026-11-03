package com.dragonminez.common.init.item.consumables;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;


public class MasteryCapsuleItem extends Item implements CapsuleUseTracker.SummaryItem {

	private static final double MASTERY_GAIN = 5.0;

	public MasteryCapsuleItem() {
		super(new Properties());
	}

	@Override
	public @NotNull Component getName(@NotNull ItemStack stack) {
		return Component.translatable("item.dragonminez.mastery_capsule");
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack capsule = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.5F, 1.0F);
		if (level.isClientSide) {
			return InteractionResultHolder.fail(capsule);
		}

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) {
				player.displayClientMessage(Component.translatable("error.dmz.createcharacter").withStyle(ChatFormatting.RED), true);
				return;
			}
			applyMastery(player, data, capsule);
		});
		return InteractionResultHolder.sidedSuccess(capsule, level.isClientSide());
	}

	private void applyMastery(Player player, StatsData data, ItemStack capsule) {
		Character character = data.getCharacter();
		String group = character.getActiveFormGroup();
		String form = character.getActiveForm();

		if (form == null || form.isEmpty() || form.equalsIgnoreCase("base") || group == null || group.isEmpty()) {
			player.displayClientMessage(Component.translatable("item.dragonminez.mastery_capsule.no_form").withStyle(ChatFormatting.RED), true);
			return;
		}

		var formConfig = ConfigManager.getFormGroup(character.getRaceName(), group);
		if (formConfig == null || formConfig.getForm(form) == null) {
			player.displayClientMessage(Component.translatable("item.dragonminez.mastery_capsule.no_form").withStyle(ChatFormatting.RED), true);
			return;
		}

		double maxMastery = formConfig.getForm(form).getMaxMastery();
		if (character.getFormMasteries().hasMaxMastery(group, form, maxMastery)) {
			player.displayClientMessage(Component.translatable("item.dragonminez.mastery_capsule.max").withStyle(ChatFormatting.RED), true);
			return;
		}

		character.getFormMasteries().addMastery(group, form, MASTERY_GAIN, maxMastery);
		capsule.shrink(1);
		int total = CapsuleUseTracker.add(CapsuleUseTracker.getStreak(player, this), group + ":" + form, (int) MASTERY_GAIN);
		player.displayClientMessage(Component.translatable("item.dragonminez.mastery_capsule.use", total).withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	@Override
	public Component getStreakSummary(ServerPlayer player, Map<String, Integer> totals) {
		if (totals.isEmpty()) return null;
		// Key format is "group:form", set in applyMastery
		String[] key = totals.keySet().iterator().next().split(":", 2);
		if (key.length < 2) return null;
		String group = key[0];
		String form = key[1];

		return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
			Character character = data.getCharacter();
			var formConfig = ConfigManager.getFormGroup(character.getRaceName(), group);
			if (formConfig == null || formConfig.getForm(form) == null) return null;
			int mastery = (int) character.getFormMasteries().getMastery(group, form);
			int maxMastery = (int) (double) formConfig.getForm(form).getMaxMastery();
			return (Component) Component.translatable("item.dragonminez.mastery_capsule.current", mastery, maxMastery)
					.withStyle(ChatFormatting.RED);
		}).orElse(null);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
		tooltip.add(Component.translatable("item.dragonminez.mastery_capsule.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
