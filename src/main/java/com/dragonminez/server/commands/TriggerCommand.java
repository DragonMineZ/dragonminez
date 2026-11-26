package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RacialDataSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.racial.impl.BioAndroidEvolution;
import com.dragonminez.common.racial.impl.HumanAdaptation;
import com.dragonminez.common.racial.impl.MajinAbsorption;
import com.dragonminez.common.racial.impl.NamekAssimilation;
import com.dragonminez.common.racial.impl.SaiyanZenkai;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.util.FalseSuperSaiyanHelper;
import com.dragonminez.common.util.FusionForms;
import com.dragonminez.common.util.TransformationItemCostHelper;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.StackForms;
import com.dragonminez.server.util.MutantManager;
import com.dragonminez.server.util.TriggerFormMarker;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TriggerCommand {

	private static final String VIRTUAL_SOURCE_NAME = "/dmztrigger";
	private static final List<String> RACIAL_COOLDOWNS = List.of(
			Cooldowns.ZENKAI, Cooldowns.ABSORPTION, Cooldowns.ASSIMILATION, Cooldowns.DRAIN, Cooldowns.ADRENALINE,
			Cooldowns.ANDROID_BARRIER_CD, Cooldowns.NAMEK_REGEN, Cooldowns.BIO_EXPLODE_CD, Cooldowns.GOD_RITUAL);
	private static final List<String> RACIAL_SLOT_COOLDOWN_PREFIXES = List.of("AbsorptionSlot_", "CellJrSlot_");

	@FunctionalInterface
	private interface TriggerAction {
		int run(CommandSourceStack source, ServerPlayer target, StatsData data) throws CommandSyntaxException;
	}

	private static final SuggestionProvider<CommandSourceStack> FORM_GROUP_SUGGESTIONS = (ctx, builder) -> {
		StatsData data = suggestionTarget(ctx);
		if (data == null) return builder.buildFuture();
		return SharedSuggestionProvider.suggest(escaped(FusionForms.allFormGroups(data).keySet()), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> FORM_SUGGESTIONS = (ctx, builder) -> {
		StatsData data = suggestionTarget(ctx);
		if (data == null) return builder.buildFuture();
		Map<String, FormConfig> groups = FusionForms.allFormGroups(data);
		String group = findKey(groups.keySet(), StringArgumentType.getString(ctx, "group"));
		if (group == null) return builder.buildFuture();
		return SharedSuggestionProvider.suggest(escaped(formNames(groups.get(group))), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> STACK_GROUP_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(escaped(ConfigManager.getAllStackForms().keySet()), builder);

	private static final SuggestionProvider<CommandSourceStack> STACK_FORM_SUGGESTIONS = (ctx, builder) -> {
		Map<String, FormConfig> groups = ConfigManager.getAllStackForms();
		String group = findKey(groups.keySet(), StringArgumentType.getString(ctx, "group"));
		if (group == null) return builder.buildFuture();
		return SharedSuggestionProvider.suggest(escaped(formNames(groups.get(group))), builder);
	};

	private static final SuggestionProvider<CommandSourceStack> MOB_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.stream()
					.filter(TriggerCommand::isLivingType)
					.map(BuiltInRegistries.ENTITY_TYPE::getKey), builder);

	private static final SuggestionProvider<CommandSourceStack> MAJIN_POTENCY_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of(format(racialConfig().getMajin().getAbsorptionStatCopy() * 100.0)), builder);

	private static final SuggestionProvider<CommandSourceStack> NAMEK_POTENCY_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of(format(racialConfig().getNamekian().getAssimilationStatBoost() * 100.0)), builder);

	private static final SuggestionProvider<CommandSourceStack> PERCENT_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(List.of("0", "25", "50", "75", "100"), builder);

	private static final SuggestionProvider<CommandSourceStack> RELEASE_SUGGESTIONS = (ctx, builder) -> {
		List<String> values = new ArrayList<>(List.of("0", "50", "100"));
		StatsData data = suggestionTarget(ctx);
		if (data != null && !values.contains(String.valueOf(data.getMaxPowerRelease()))) values.add(String.valueOf(data.getMaxPowerRelease()));
		return SharedSuggestionProvider.suggest(values, builder);
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmztrigger")
				.requires(source -> DMZPermissions.check(source, DMZPermissions.TRIGGER_SELF, DMZPermissions.TRIGGER_OTHERS))
				.then(Commands.argument("target", EntityArgument.player())

						.then(Commands.literal("form")
								.then(Commands.literal("base")
										.executes(ctx -> run(ctx, TriggerCommand::revertForm)))
								.then(Commands.argument("group", StringArgumentType.string()).suggests(FORM_GROUP_SUGGESTIONS)
										.then(Commands.argument("form", StringArgumentType.string()).suggests(FORM_SUGGESTIONS)
												.executes(ctx -> run(ctx, (source, target, data) -> triggerForm(source, target, data,
														StringArgumentType.getString(ctx, "group"), StringArgumentType.getString(ctx, "form")))))))

						.then(Commands.literal("stackform")
								.then(Commands.literal("clear")
										.executes(ctx -> run(ctx, TriggerCommand::clearStackForm)))
								.then(Commands.argument("group", StringArgumentType.string()).suggests(STACK_GROUP_SUGGESTIONS)
										.then(Commands.argument("form", StringArgumentType.string()).suggests(STACK_FORM_SUGGESTIONS)
												.executes(ctx -> run(ctx, (source, target, data) -> triggerStackForm(source, target, data,
														StringArgumentType.getString(ctx, "group"), StringArgumentType.getString(ctx, "form")))))))

						.then(Commands.literal("clear")
								.executes(ctx -> run(ctx, TriggerCommand::clearAll)))

						.then(Commands.literal("racialskill")
								.then(Commands.literal("saiyan")
										.executes(ctx -> run(ctx, (source, target, data) -> zenkai(source, target, data, 100.0, false)))
										.then(Commands.argument("potency", DoubleArgumentType.doubleArg(0.0, 100.0)).suggests(PERCENT_SUGGESTIONS)
												.executes(ctx -> run(ctx, (source, target, data) -> zenkai(source, target, data, DoubleArgumentType.getDouble(ctx, "potency"), false)))
												.then(Commands.literal("temp")
														.executes(ctx -> run(ctx, (source, target, data) -> zenkai(source, target, data, DoubleArgumentType.getDouble(ctx, "potency"), false))))
												.then(Commands.literal("real")
														.executes(ctx -> run(ctx, (source, target, data) -> zenkai(source, target, data, DoubleArgumentType.getDouble(ctx, "potency"), true))))))
								.then(Commands.literal("majin")
										.then(Commands.argument("potency", DoubleArgumentType.doubleArg(0.0, 100.0)).suggests(MAJIN_POTENCY_SUGGESTIONS)
												.then(Commands.argument("mob", ResourceLocationArgument.id()).suggests(MOB_SUGGESTIONS)
														.executes(ctx -> run(ctx, (source, target, data) -> absorb(source, target, data,
																DoubleArgumentType.getDouble(ctx, "potency"), ResourceLocationArgument.getId(ctx, "mob")))))))
								.then(Commands.literal("namekian")
										.executes(ctx -> run(ctx, (source, target, data) -> assimilate(source, target, data, racialConfig().getNamekian().getAssimilationStatBoost() * 100.0)))
										.then(Commands.argument("potency", DoubleArgumentType.doubleArg(0.0, 100.0)).suggests(NAMEK_POTENCY_SUGGESTIONS)
												.executes(ctx -> run(ctx, (source, target, data) -> assimilate(source, target, data, DoubleArgumentType.getDouble(ctx, "potency"))))))
								.then(Commands.literal("human")
										.then(Commands.literal("adrenaline")
												.executes(ctx -> run(ctx, TriggerCommand::adrenaline))))
								.then(Commands.literal("frostdemon")
										.then(Commands.literal("reserve")
												.then(Commands.argument("percent", DoubleArgumentType.doubleArg(0.0, 100.0)).suggests(PERCENT_SUGGESTIONS)
														.executes(ctx -> run(ctx, (source, target, data) -> reserve(source, target, data, DoubleArgumentType.getDouble(ctx, "percent")))))))
								.then(Commands.literal("bioandroid")
										.then(Commands.literal("explode")
												.executes(ctx -> run(ctx, TriggerCommand::explode)))
										.then(Commands.literal("celljr")
												.executes(ctx -> run(ctx, TriggerCommand::cellJr)))))

						.then(Commands.literal("cooldowns")
								.executes(ctx -> run(ctx, TriggerCommand::clearCooldowns)))

						.then(Commands.literal("rage")
								.then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0)).suggests(PERCENT_SUGGESTIONS)
										.executes(ctx -> run(ctx, (source, target, data) -> rage(source, target, data, DoubleArgumentType.getDouble(ctx, "value"))))))

						.then(Commands.literal("knockout")
								.executes(ctx -> run(ctx, (source, target, data) -> knockout(source, target, data, ConfigManager.getCombatConfig().getKnockdownDurationSeconds())))
								.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
										.executes(ctx -> run(ctx, (source, target, data) -> knockout(source, target, data, IntegerArgumentType.getInteger(ctx, "seconds"))))))

						.then(Commands.literal("release")
								.then(Commands.argument("value", IntegerArgumentType.integer(0)).suggests(RELEASE_SUGGESTIONS)
										.executes(ctx -> run(ctx, (source, target, data) -> release(source, target, data, IntegerArgumentType.getInteger(ctx, "value"))))))
				)
		);
	}

	private static int run(CommandContext<CommandSourceStack> ctx, TriggerAction action) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
		boolean self = source.getEntity() == target;
		if (!DMZPermissions.hasPermission(source, DMZPermissions.TRIGGER_OTHERS)
				&& !(self && DMZPermissions.hasPermission(source, DMZPermissions.TRIGGER_SELF))) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.others_denied"));
			return 0;
		}
		StatsData data = stats(target);
		if (data == null || !data.getStatus().isHasCreatedCharacter()) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.no_data", target.getName().getString()));
			return 0;
		}
		return action.run(source, target, data);
	}

	private static int triggerForm(CommandSourceStack source, ServerPlayer target, StatsData data, String groupInput, String formInput) {
		Map<String, FormConfig> groups = FusionForms.allFormGroups(data);
		String group = findKey(groups.keySet(), groupInput);
		if (group == null) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.form.unknown_group", groupInput, target.getName().getString()));
			return 0;
		}
		FormConfig.FormData form = groups.get(group).getForm(formInput);
		if (form == null) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.form.unknown_form", formInput, group));
			return 0;
		}

		var character = data.getCharacter();
		float[] resourceSnapshot = data.snapshotMultiplierResources();
		character.recordPreviousForm();
		character.setActiveForm(group, form.getName());
		data.restoreMultiplierGains(target, resourceSnapshot);
		TransformationItemCostHelper.clearFormDurationSecondsRemaining(target);
		TriggerFormMarker.markForm(target, group, form.getName());
		prepareRageState(data, group, form);

		if (!target.hasEffect(MainEffects.TRANSFORMED)) {
			target.addEffect(new MobEffectInstance(MainEffects.TRANSFORMED, -1, 0, false, false, true));
		}
		playSound(target, MainSounds.TRANSFORM_ON.get());
		target.sendSystemMessage(Component.translatable("message.dragonminez.transformation", activeFormName(data)), true);
		finish(target);

		success(source, Component.translatable("command.dragonminez.trigger.form.success", activeFormName(data), target.getName().getString()));
		return 1;
	}

	private static void prepareRageState(StatsData data, String group, FormConfig.FormData form) {
		if (FalseSuperSaiyanHelper.isActiveForm(data)) {
			data.getStatus().setForcedAura(true);
			data.getStatus().setForcedCharge(false);
			data.getStatus().setRageActive(true);
			data.getResources().setRage(100.0f);
		} else if (MutantManager.isMutant(data) && FusionForms.hasMutantFor(data, group) && TransformationsHelper.isRageBorrowedForm(data, group, form)) {
			data.getResources().setRage(100.0f);
			data.getStatus().setRageActive(true);
		}
	}

	private static int revertForm(CommandSourceStack source, ServerPlayer target, StatsData data) {
		revertFormState(target, data);
		finish(target);
		success(source, Component.translatable("command.dragonminez.trigger.form.base", target.getName().getString()));
		return 1;
	}

	private static void revertFormState(ServerPlayer target, StatsData data) {
		if (FalseSuperSaiyanHelper.isActiveForm(data)) {
			data.getResources().setRage(0.0f);
			data.getStatus().setRageActive(false);
			data.getStatus().setForcedAura(false);
			data.getStatus().setForcedCharge(false);
		}
		float[] resourceSnapshot = data.snapshotMultiplierResources();
		data.getCharacter().clearPreviousFormRecord();
		TransformationsHelper.revertToBaseForm(target, data, false);
		data.restoreMultiplierGains(target, resourceSnapshot);
		target.removeEffect(MainEffects.TRANSFORMED);
		TransformationItemCostHelper.clearFormDurationSecondsRemaining(target);
	}

	private static int triggerStackForm(CommandSourceStack source, ServerPlayer target, StatsData data, String groupInput, String formInput) {
		Map<String, FormConfig> groups = ConfigManager.getAllStackForms();
		String group = findKey(groups.keySet(), groupInput);
		if (group == null) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.stackform.unknown_group", groupInput));
			return 0;
		}
		FormConfig.FormData form = groups.get(group).getForm(formInput);
		if (form == null) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.form.unknown_form", formInput, group));
			return 0;
		}

		var character = data.getCharacter();
		float[] resourceSnapshot = data.snapshotMultiplierResources();
		character.recordPreviousStackForm();
		character.setActiveStackForm(group, form.getName());
		data.restoreMultiplierGains(target, resourceSnapshot);
		TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(target);
		TriggerFormMarker.markStack(target, group, form.getName());

		if (!target.hasEffect(MainEffects.STACK_TRANSFORMED)) {
			target.addEffect(new MobEffectInstance(MainEffects.STACK_TRANSFORMED, -1, 0, false, false, true));
		}
		playSound(target, MainSounds.TRANSFORM_ON.get());
		if (StackForms.GROUP_KAIOKEN.equalsIgnoreCase(group)) playSound(target, MainSounds.STACK_FORM.get());
		target.sendSystemMessage(Component.translatable("message.dragonminez.transformation", activeFormName(data)), true);
		finish(target);

		success(source, Component.translatable("command.dragonminez.trigger.form.success", activeFormName(data), target.getName().getString()));
		return 1;
	}

	private static int clearStackForm(CommandSourceStack source, ServerPlayer target, StatsData data) {
		clearStackFormState(target, data);
		finish(target);
		success(source, Component.translatable("command.dragonminez.trigger.stackform.clear", target.getName().getString()));
		return 1;
	}

	private static void clearStackFormState(ServerPlayer target, StatsData data) {
		float[] resourceSnapshot = data.snapshotMultiplierResources();
		data.getCharacter().clearPreviousStackFormRecord();
		data.getCharacter().clearActiveStackForm(target);
		data.restoreMultiplierGains(target, resourceSnapshot);
		target.removeEffect(MainEffects.STACK_TRANSFORMED);
		TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(target);
	}

	private static int clearAll(CommandSourceStack source, ServerPlayer target, StatsData data) {
		clearStackFormState(target, data);
		revertFormState(target, data);
		finish(target);
		success(source, Component.translatable("command.dragonminez.trigger.clear", target.getName().getString()));
		return 1;
	}

	private static int zenkai(CommandSourceStack source, ServerPlayer target, StatsData data, double percent, boolean real) {
		if (!hasRacial(source, target, data, "saiyan")) return 0;
		GeneralServerConfig.SaiyanRacialConfig config = racialConfig().getSaiyan();
		double potency = percent / 100.0;

		if (real) {
			boolean isMaxBuff = percent >= 100.0;
			boolean permanent = isMaxBuff && data.getRacialData().getZenkaiPermanentUses() < config.getPermanentMaxBuffs();
			if (!permanent) SaiyanZenkai.removeTemporaryBuff(data, config);
			target.setHealth(1.0F);
			SaiyanZenkai.trigger(target, data, config, potency, isMaxBuff);
			success(source, Component.translatable("command.dragonminez.trigger.racial.zenkai.real", format(percent), target.getName().getString()));
		} else {
			SaiyanZenkai.applyTemporaryBuff(target, data, config, potency);
			success(source, Component.translatable("command.dragonminez.trigger.racial.zenkai.temp", format(percent), target.getName().getString()));
		}
		return 1;
	}

	private static int absorb(CommandSourceStack source, ServerPlayer target, StatsData data, double percent, ResourceLocation mobId) {
		if (!hasRacial(source, target, data, "majin")) return 0;
		GeneralServerConfig.MajinRacialConfig config = racialConfig().getMajin();
		if (MajinAbsorption.effectiveSlotsUsed(data) >= config.getAbsorptionAmount()) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.racial.slots_full", target.getName().getString()));
			return 0;
		}

		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null);
		double maxHealth = baseMaxHealth(type);
		if (maxHealth <= 0.0) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.racial.majin.invalid_mob", mobId.toString()));
			return 0;
		}
		String mobName = type.getDescription().getString();

		MajinAbsorption.applyVirtualAbsorption(target, data, UUID.randomUUID(), mobName, maxHealth, percent / 100.0);
		success(source, Component.translatable("command.dragonminez.trigger.racial.majin.success",
				target.getName().getString(), mobName, format(maxHealth), format(percent)));
		return 1;
	}

	private static int assimilate(CommandSourceStack source, ServerPlayer target, StatsData data, double percent) {
		if (!hasRacial(source, target, data, "namekian")) return 0;
		GeneralServerConfig.NamekianRacialConfig config = racialConfig().getNamekian();
		if (data.getRacialData().getAssimilations().size() >= config.getAssimilationAmount()) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.racial.slots_full", target.getName().getString()));
			return 0;
		}

		NamekAssimilation.applyVirtualAssimilation(target, data, UUID.randomUUID(), VIRTUAL_SOURCE_NAME, percent / 100.0);
		success(source, Component.translatable("command.dragonminez.trigger.racial.namek.success", target.getName().getString(), format(percent)));
		return 1;
	}

	private static int adrenaline(CommandSourceStack source, ServerPlayer target, StatsData data) {
		if (!hasRacial(source, target, data, "human")) return 0;
		HumanAdaptation.triggerAdrenaline(target, data, racialConfig().getHuman());
		success(source, Component.translatable("command.dragonminez.trigger.racial.human.adrenaline", target.getName().getString()));
		return 1;
	}

	private static int reserve(CommandSourceStack source, ServerPlayer target, StatsData data, double percent) {
		if (!hasRacial(source, target, data, "frostdemon")) return 0;
		float maxReserve = (float) (data.getMaxEnergy() * racialConfig().getFrostdemon().getReserveMaxRatio());
		data.getRacialData().setEnergyReserve((float) (maxReserve * percent / 100.0));
		NetworkHandler.sendToTrackingEntityAndSelf(new RacialDataSyncS2C(target), target);
		success(source, Component.translatable("command.dragonminez.trigger.racial.frostdemon.reserve", target.getName().getString(), format(percent)));
		return 1;
	}

	private static int explode(CommandSourceStack source, ServerPlayer target, StatsData data) {
		if (!hasRacial(source, target, data, "bioandroid")) return 0;
		BioAndroidEvolution.triggerExplosion(target, data, racialConfig().getBioandroid());
		success(source, Component.translatable("command.dragonminez.trigger.racial.bioandroid.explode", target.getName().getString()));
		return 1;
	}

	private static int cellJr(CommandSourceStack source, ServerPlayer target, StatsData data) {
		if (!hasRacial(source, target, data, "bioandroid")) return 0;
		int before = data.getRacialData().getCellJrs().size();
		BioAndroidEvolution.summonCellJr(new RacialContext(target, data), target, data);
		if (data.getRacialData().getCellJrs().size() <= before) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.racial.bioandroid.celljr_limit", target.getName().getString()));
			return 0;
		}
		success(source, Component.translatable("command.dragonminez.trigger.racial.bioandroid.celljr", target.getName().getString()));
		return 1;
	}

	private static int clearCooldowns(CommandSourceStack source, ServerPlayer target, StatsData data) {
		Cooldowns cooldowns = data.getCooldowns();
		int cleared = 0;
		for (String key : RACIAL_COOLDOWNS) {
			if (!cooldowns.hasCooldown(key)) continue;
			cooldowns.removeCooldown(key);
			cleared++;
		}
		for (String key : new ArrayList<>(cooldowns.getAllCooldowns().keySet())) {
			if (RACIAL_SLOT_COOLDOWN_PREFIXES.stream().noneMatch(key::startsWith)) continue;
			cooldowns.removeCooldown(key);
			cleared++;
		}
		target.removeEffect(MainEffects.SAIYAN_PASSIVE);
		target.removeEffect(MainEffects.BIOANDROID_PASSIVE);
		target.removeEffect(MainEffects.BIOANDROID_EXPLODE_CD);
		finish(target);

		success(source, Component.translatable("command.dragonminez.trigger.cooldowns", cleared, target.getName().getString()));
		return 1;
	}

	private static int rage(CommandSourceStack source, ServerPlayer target, StatsData data, double value) {
		if (!MutantManager.isMutant(data) && !FalseSuperSaiyanHelper.ownsRageBar(data)) {
			source.sendFailure(Component.translatable("command.dragonminez.trigger.rage.no_meter", target.getName().getString()));
			return 0;
		}
		data.getResources().setRage((float) value);
		finish(target);
		success(source, Component.translatable("command.dragonminez.trigger.rage.success", target.getName().getString(), format(value)));
		return 1;
	}

	private static int knockout(CommandSourceStack source, ServerPlayer target, StatsData data, int seconds) {
		int ticks = seconds * 20;
		target.setHealth(1.0F);
		data.getStatus().setKnockedDown(true);
		data.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_DURATION, ticks);
		data.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_INVULN, ticks);
		data.getCharacter().clearActiveForm();
		data.getCharacter().clearActiveStackForm();
		finish(target);
		success(source, Component.translatable("command.dragonminez.trigger.knockout", target.getName().getString(), seconds));
		return 1;
	}

	private static int release(CommandSourceStack source, ServerPlayer target, StatsData data, int value) {
		int max = data.getMaxPowerRelease();
		int applied = Math.min(value, max);
		data.getResources().setPowerRelease(applied);
		finish(target);
		success(source, Component.translatable("command.dragonminez.trigger.release", target.getName().getString(), applied, max));
		return 1;
	}

	private static boolean hasRacial(CommandSourceStack source, ServerPlayer target, StatsData data, String racialId) {
		String racial = RacialRegistry.forPlayer(data).map(RacialAbility::id).orElse("");
		if (racialId.equals(racial)) return true;
		source.sendFailure(Component.translatable("command.dragonminez.trigger.racial.wrong_race", target.getName().getString(), racialId));
		return false;
	}

	@SuppressWarnings("unchecked")
	private static double baseMaxHealth(EntityType<?> type) {
		if (type == null || !isLivingType(type)) return 0.0;
		AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
		return supplier.hasAttribute(Attributes.MAX_HEALTH) ? supplier.getBaseValue(Attributes.MAX_HEALTH) : 0.0;
	}

	private static boolean isLivingType(EntityType<?> type) {
		return type != EntityType.PLAYER && type != EntityType.ARMOR_STAND && DefaultAttributes.hasSupplier(type);
	}

	private static Component activeFormName(StatsData data) {
		var character = data.getCharacter();
		Component formName = character.hasActiveForm()
				? Component.translatable(FusionForms.formTranslationKey(character.getRaceName(), character.getActiveFormGroup(), character.getActiveForm()))
				: null;
		if (!character.hasActiveStackForm()) return formName != null ? formName : Component.literal("base");

		String stackGroup = character.getActiveStackFormGroup();
		Component stackName = Component.empty()
				.append(Component.translatable("race.dragonminez.stack.group." + stackGroup))
				.append(Component.literal(" "))
				.append(Component.translatable("race.dragonminez.stack.form." + stackGroup + "." + character.getActiveStackForm()));
		if (formName == null) return stackName;
		return Component.empty().append(formName).append(Component.literal(" x ")).append(stackName);
	}

	private static void finish(ServerPlayer target) {
		target.refreshDimensions();
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(target), target);
	}

	private static void playSound(ServerPlayer player, SoundEvent sound) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	private static void success(CommandSourceStack source, Component message) {
		boolean log = ConfigManager.getServerConfig().getGameplay().getCommandOutputOnConsole();
		source.sendSuccess(() -> message, log);
	}

	private static StatsData suggestionTarget(CommandContext<CommandSourceStack> ctx) {
		try {
			return stats(EntityArgument.getPlayer(ctx, "target"));
		} catch (CommandSyntaxException | IllegalArgumentException e) {
			return null;
		}
	}

	private static StatsData stats(ServerPlayer player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
	}

	private static GeneralServerConfig.RacialSkillsConfig racialConfig() {
		return ConfigManager.getServerConfig().getRacialSkills();
	}

	private static String findKey(Collection<String> keys, String input) {
		if (input == null) return null;
		for (String key : keys) {
			if (key.equalsIgnoreCase(input)) return key;
		}
		return null;
	}

	private static List<String> formNames(FormConfig config) {
		List<String> names = new ArrayList<>();
		if (config == null) return names;
		for (FormConfig.FormData form : config.getForms().values()) {
			if (form != null && form.getName() != null && !form.getName().isEmpty()) names.add(form.getName());
		}
		return names;
	}

	private static List<String> escaped(Collection<String> values) {
		return values.stream().map(StringArgumentType::escapeIfRequired).toList();
	}

	private static String format(double value) {
		if (value == Math.rint(value)) return String.valueOf((long) value);
		return String.format(Locale.ROOT, "%.2f", value);
	}
}
