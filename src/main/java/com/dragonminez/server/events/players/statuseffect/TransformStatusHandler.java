package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TransformStatusHandler implements IStatusEffectHandler {
    private static final String LOG_PREFIX = "[DMZ-FORM-EFFECTS] ";
    private static final String TAG_ROOT = "dmzTransformMobEffects";
    private static final String TAG_LAST_FORM = "lastForm";
    private static final String TAG_LAST_FORM_GROUP = "lastFormGroup";
    private static final String TAG_LAST_STACK_FORM = "lastStackForm";
    private static final String TAG_LAST_STACK_GROUP = "lastStackGroup";

    @Override
    public void handleStatusEffects(ServerPlayer player, StatsData data) {
        if (data.getStatus().isActionCharging()) {
            if (data.getStatus().getSelectedAction().equals(ActionMode.FORM)) {
                if (!player.hasEffect(MainEffects.TRANSFORM.get())) {
                    player.addEffect(new MobEffectInstance(MainEffects.TRANSFORM.get(), -1, 0, false, false, true));
                }
            } else if (data.getStatus().getSelectedAction().equals(ActionMode.STACK)) {
                if (!player.hasEffect(MainEffects.STACK_TRANSFORM.get())) {
                    player.addEffect(new MobEffectInstance(MainEffects.STACK_TRANSFORM.get(), -1, 0, false, false, true));
                }
            }
        } else {
            player.removeEffect(MainEffects.TRANSFORM.get());
            player.removeEffect(MainEffects.STACK_TRANSFORM.get());
        }
    }

    @Override
    public void onPlayerTick(ServerPlayer player, StatsData data) {
        if (data.getCharacter().getActiveForm() == null || data.getCharacter().getActiveForm().isEmpty() || data.getCharacter().getActiveForm().equals("base")) {
            player.removeEffect(MainEffects.TRANSFORM.get());
        }
        if (data.getCharacter().getActiveStackForm() == null || data.getCharacter().getActiveStackForm().isEmpty() || data.getCharacter().getActiveStackForm().equals("base")) {
            player.removeEffect(MainEffects.STACK_TRANSFORM.get());
        }

        CompoundTag effectTag = getOrCreateEffectTag(player);

        String activeForm = normalizeFormName(data.getCharacter().getActiveForm(), true);
        String activeFormGroup = normalizeGroupName(data.getCharacter().getActiveFormGroup());
        String activeStackForm = normalizeFormName(data.getCharacter().getActiveStackForm(), false);
        String activeStackGroup = normalizeGroupName(data.getCharacter().getActiveStackFormGroup());

        String lastForm = decodeNullable(effectTag.getString(TAG_LAST_FORM));
        String lastFormGroup = decodeNullable(effectTag.getString(TAG_LAST_FORM_GROUP));
        String lastStackForm = decodeNullable(effectTag.getString(TAG_LAST_STACK_FORM));
        String lastStackGroup = decodeNullable(effectTag.getString(TAG_LAST_STACK_GROUP));

        boolean formChanged = !Objects.equals(activeForm, lastForm) || !Objects.equals(activeFormGroup, lastFormGroup);
        boolean stackChanged = !Objects.equals(activeStackForm, lastStackForm) || !Objects.equals(activeStackGroup, lastStackGroup);

        if (formChanged || stackChanged) {
            LogUtil.info(Env.SERVER,
                    LOG_PREFIX + "State change for {} -> formGroup='{}', form='{}', stackGroup='{}', stackForm='{}'",
                    player.getScoreboardName(),
                    safe(activeFormGroup),
                    safe(activeForm),
                    safe(activeStackGroup),
                    safe(activeStackForm));
        }

        if (formChanged && activeForm != null) {
            MinecraftForge.EVENT_BUS.post(new DMZEvent.FormChangeEvent(player, lastFormGroup, lastForm, activeFormGroup, activeForm));
        }
        if (stackChanged && activeStackForm != null) {
            MinecraftForge.EVENT_BUS.post(new DMZEvent.StackFormChangeEvent(player, lastStackGroup, lastStackForm, activeStackGroup, activeStackForm));
        }

        FormConfig.FormData formData = resolveRegularFormData(data, activeFormGroup, activeForm, player);
        FormConfig.FormData stackFormData = resolveStackFormData(activeStackGroup, activeStackForm, player);

        if (formChanged) {
            applyTemporaryEffects(player, formData, "form", activeFormGroup, activeForm);
        }
        if (stackChanged) {
            applyTemporaryEffects(player, stackFormData, "stack", activeStackGroup, activeStackForm);
        }

        Map<MobEffect, PersistentEffectAccumulator> persistentEffects = new HashMap<>();
        collectPersistentEffects(persistentEffects, formData, "form", activeFormGroup, activeForm, player);
        collectPersistentEffects(persistentEffects, stackFormData, "stack", activeStackGroup, activeStackForm, player);

        Set<MobEffect> desiredPersistentTypes = persistentEffects.keySet();
        Set<MobEffect> previousPersistentTypes = readTrackedPersistentEffects(effectTag);

        for (MobEffect effect : previousPersistentTypes) {
            if (!desiredPersistentTypes.contains(effect) && player.hasEffect(effect)) {
                LogUtil.info(Env.SERVER, LOG_PREFIX + "Removing persistent effect '{}' from {} because no active transformation now provides it.", getEffectId(effect), player.getScoreboardName());
                player.removeEffect(effect);
            }
        }

        for (Map.Entry<MobEffect, PersistentEffectAccumulator> entry : persistentEffects.entrySet()) {
            MobEffect effect = entry.getKey();
            PersistentEffectAccumulator accumulator = entry.getValue();
            MobEffectInstance instance = new MobEffectInstance(effect, -1, accumulator.getFinalAmplifier(), accumulator.ambient, accumulator.visible, accumulator.showIcon);
            boolean added = player.addEffect(instance);
            LogUtil.info(Env.SERVER,
                    LOG_PREFIX + "Applied persistent effect '{}' to {} from {} source(s): amplifier={}, ambient={}, visible={}, icon={}, result={}",
                    getEffectId(effect),
                    player.getScoreboardName(),
                    accumulator.sources,
                    accumulator.getFinalAmplifier(),
                    accumulator.ambient,
                    accumulator.visible,
                    accumulator.showIcon,
                    added);
        }

        writeTrackedPersistentEffects(effectTag, desiredPersistentTypes);
        effectTag.putString(TAG_LAST_FORM, encodeNullable(activeForm));
        effectTag.putString(TAG_LAST_FORM_GROUP, encodeNullable(activeFormGroup));
        effectTag.putString(TAG_LAST_STACK_FORM, encodeNullable(activeStackForm));
        effectTag.putString(TAG_LAST_STACK_GROUP, encodeNullable(activeStackGroup));
    }

    @Override
    public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {
    }

    private FormConfig.FormData resolveRegularFormData(StatsData data, String group, String form, ServerPlayer player) {
        if (group == null || form == null) {
            return null;
        }

        FormConfig config = ConfigManager.getFormGroup(data.getCharacter().getRaceName(), group);
        if (config == null) {
            LogUtil.warn(Env.SERVER, LOG_PREFIX + "Could not resolve regular form group '{}' for {}.", group, player.getScoreboardName());
            return null;
        }

        FormConfig.FormData formData = config.getForm(form);
        if (formData == null) {
            formData = config.getFormByKey(form);
        }

        if (formData == null) {
            LogUtil.warn(Env.SERVER, LOG_PREFIX + "Could not resolve regular form '{}' in group '{}' for {}.", form, group, player.getScoreboardName());
            return null;
        }

        return formData;
    }

    private FormConfig.FormData resolveStackFormData(String group, String form, ServerPlayer player) {
        if (group == null || form == null) {
            return null;
        }

        FormConfig config = ConfigManager.getStackFormGroup(group);
        if (config == null) {
            LogUtil.warn(Env.SERVER, LOG_PREFIX + "Could not resolve stack form group '{}' for {}.", group, player.getScoreboardName());
            return null;
        }

        FormConfig.FormData formData = config.getForm(form);
        if (formData == null) {
            formData = config.getFormByKey(form);
        }

        if (formData == null) {
            LogUtil.warn(Env.SERVER, LOG_PREFIX + "Could not resolve stack form '{}' in group '{}' for {}.", form, group, player.getScoreboardName());
            return null;
        }

        return formData;
    }

    private void applyTemporaryEffects(ServerPlayer player, FormConfig.FormData formData, String sourceType, String group, String form) {
        if (formData == null) {
            return;
        }

        for (FormConfig.FormData.MobEffectConfig effectConfig : formData.getMobEffects()) {
            if (effectConfig == null || effectConfig.isPersistent()) {
                continue;
            }

            MobEffect effect = resolveMobEffect(effectConfig.getEffectId());
            if (effect == null) {
                LogUtil.warn(Env.SERVER,
                        LOG_PREFIX + "Failed to resolve temporary effect '{}' for {} source '{}'/'{}'.",
                        effectConfig != null ? effectConfig.getEffectId() : "<null>",
                        sourceType,
                        safe(group),
                        safe(form));
                continue;
            }

            MobEffectInstance instance = new MobEffectInstance(
                    effect,
                    effectConfig.getDurationTicks(),
                    effectConfig.getAmplifier(),
                    effectConfig.isAmbient(),
                    effectConfig.isVisible(),
                    effectConfig.isShowIcon());

            boolean added = player.addEffect(instance);
            LogUtil.info(Env.SERVER,
                    LOG_PREFIX + "Applied temporary effect '{}' to {} from {} '{}'/'{}': duration={}, amplifier={}, ambient={}, visible={}, icon={}, result={}",
                    getEffectId(effect),
                    player.getScoreboardName(),
                    sourceType,
                    safe(group),
                    safe(form),
                    effectConfig.getDurationTicks(),
                    effectConfig.getAmplifier(),
                    effectConfig.isAmbient(),
                    effectConfig.isVisible(),
                    effectConfig.isShowIcon(),
                    added);
        }
    }

    private void collectPersistentEffects(Map<MobEffect, PersistentEffectAccumulator> persistentEffects, FormConfig.FormData formData, String sourceType, String group, String form, ServerPlayer player) {
        if (formData == null) {
            return;
        }

        for (FormConfig.FormData.MobEffectConfig effectConfig : formData.getMobEffects()) {
            if (effectConfig == null || !effectConfig.isPersistent()) {
                continue;
            }

            MobEffect effect = resolveMobEffect(effectConfig.getEffectId());
            if (effect == null) {
                LogUtil.warn(Env.SERVER,
                        LOG_PREFIX + "Failed to resolve persistent effect '{}' for {} source '{}'/'{}'.",
                        effectConfig != null ? effectConfig.getEffectId() : "<null>",
                        sourceType,
                        safe(group),
                        safe(form));
                continue;
            }

            PersistentEffectAccumulator accumulator = persistentEffects.computeIfAbsent(effect, ignored -> new PersistentEffectAccumulator());
            accumulator.totalLevels += effectConfig.getAmplifier() + 1;
            accumulator.ambient = accumulator.ambient || effectConfig.isAmbient();
            accumulator.visible = accumulator.visible || effectConfig.isVisible();
            accumulator.showIcon = accumulator.showIcon || effectConfig.isShowIcon();
            accumulator.sources++;

            LogUtil.info(Env.SERVER,
                    LOG_PREFIX + "Queued persistent effect '{}' for {} from {} '{}'/'{}': rawAmplifier={}, cumulativeLevel={}, cumulativeAmplifier={}",
                    getEffectId(effect),
                    player.getScoreboardName(),
                    sourceType,
                    safe(group),
                    safe(form),
                    effectConfig.getAmplifier(),
                    accumulator.totalLevels,
                    Math.max(0, accumulator.totalLevels - 1));
        }
    }

    private CompoundTag getOrCreateEffectTag(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(TAG_ROOT)) {
            persistentData.put(TAG_ROOT, new CompoundTag());
        }
        return persistentData.getCompound(TAG_ROOT);
    }

    /**
     * Removes every persistent (infinite-duration) MobEffect that active transformations applied and were tracked on
     * the player, then wipes the tracking tag. Needed on full character resets: the normal cleanup path
     * ({@link #onPlayerTick}) stops running once {@code hasCreatedCharacter} becomes false, so the -1 duration effects
     * would otherwise linger forever onto the next character.
     */
    public static void clearAllPersistentFormEffects(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(TAG_ROOT)) {
            return;
        }
        CompoundTag effectTag = persistentData.getCompound(TAG_ROOT);
        for (MobEffect effect : readTrackedPersistentEffects(effectTag)) {
            if (player.hasEffect(effect)) {
                player.removeEffect(effect);
                LogUtil.info(Env.SERVER, LOG_PREFIX + "Removing persistent effect '{}' from {} due to character reset.", getEffectId(effect), player.getScoreboardName());
            }
        }
        persistentData.remove(TAG_ROOT);
    }

    private static Set<MobEffect> readTrackedPersistentEffects(CompoundTag effectTag) {
        Set<MobEffect> tracked = new HashSet<>();
        if (!effectTag.contains("trackedPersistentEffects")) {
            return tracked;
        }

        String raw = effectTag.getString("trackedPersistentEffects");
        if (raw == null || raw.isEmpty()) {
            return tracked;
        }

        for (String part : raw.split(";")) {
            if (part == null || part.isBlank()) {
                continue;
            }
            MobEffect effect = resolveMobEffect(part.trim());
            if (effect != null) {
                tracked.add(effect);
            }
        }
        return tracked;
    }

    private void writeTrackedPersistentEffects(CompoundTag effectTag, Set<MobEffect> effects) {
        StringBuilder builder = new StringBuilder();
        for (MobEffect effect : effects) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(getEffectId(effect));
        }
        effectTag.putString("trackedPersistentEffects", builder.toString());
    }

    private static MobEffect resolveMobEffect(String rawEffectId) {
        if (rawEffectId == null || rawEffectId.isBlank()) {
            return null;
        }

        String normalized = normalizeEffectId(rawEffectId.trim().toLowerCase());
        ResourceLocation location = ResourceLocation.tryParse(normalized);
        if (location == null) {
            return null;
        }
        return ForgeRegistries.MOB_EFFECTS.getValue(location);
    }

    private static String normalizeEffectId(String effectId) {
        return switch (effectId) {
            case "minecraft:movement_speed" -> "minecraft:speed";
            case "minecraft:damage_boost" -> "minecraft:strength";
            case "minecraft:jump" -> "minecraft:jump_boost";
            case "minecraft:dig_speed" -> "minecraft:haste";
            case "minecraft:dig_slowdown" -> "minecraft:mining_fatigue";
            case "minecraft:heal" -> "minecraft:instant_health";
            case "minecraft:harm" -> "minecraft:instant_damage";
            default -> effectId;
        };
    }

    private static String getEffectId(MobEffect effect) {
        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return key != null ? key.toString() : "unknown";
    }

    private String normalizeFormName(String form, boolean regular) {
        if (form == null || form.isBlank()) {
            return null;
        }
        if (regular && "base".equalsIgnoreCase(form)) {
            return null;
        }
        return form;
    }

    private String normalizeGroupName(String group) {
        if (group == null || group.isBlank()) {
            return null;
        }
        return group;
    }

    private String safe(String value) {
        return value != null ? value : "<none>";
    }

    private String encodeNullable(String value) {
        return value != null ? value : "";
    }

    private String decodeNullable(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static class PersistentEffectAccumulator {
        private int totalLevels;
        private boolean ambient;
        private boolean visible;
        private boolean showIcon;
        private int sources;

        private int getFinalAmplifier() {
            return Math.max(0, totalLevels - 1);
        }
    }
}

