package com.dragonminez.common.stats;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;
import java.util.stream.Collectors;

public class Effects {
    private final Map<String, Effect> effectMap = new HashMap<>();

    public Effects() {}

    public void addEffect(String name, double power, int duration) {
        effectMap.put(name.toLowerCase(), new Effect(name.toLowerCase(), power, duration));
    }

    public void removeEffect(String name) {
        effectMap.remove(name.toLowerCase());
    }

	public void removeAllEffects() {
		effectMap.clear();
	}

    public boolean hasEffect(String name) {
        return effectMap.containsKey(name.toLowerCase());
    }

    public Effect getEffect(String name) {
        return effectMap.get(name.toLowerCase());
    }

    public double getEffectPower(String name) {
        Effect effect = effectMap.get(name.toLowerCase());
        return effect != null ? effect.getPower() : 0.0;
    }

    public int getEffectDuration(String name) {
        Effect effect = effectMap.get(name.toLowerCase());
        return effect != null ? effect.getDuration() : 0;
    }

    public List<Effect> getAllEffects() {
        return new ArrayList<>(effectMap.values());
    }

    public List<Effect> getEffectsSortedByDuration() {
        return effectMap.values().stream()
                .sorted(Comparator.comparingInt(Effect::getDuration).reversed())
                .collect(Collectors.toList());
    }

    public double getTotalEffectMultiplier() {
        if (effectMap.isEmpty()) return 1.0;

        boolean isMultiplicative = ConfigManager.getServerConfig().getGameplay().getMultiplicationInsteadOfAdditionForMultipliers();
        double totalMultiplier = 1.0;
        for (Effect effect : effectMap.values()) {
            if (isMultiplicative) totalMultiplier *= effect.getPower();
            else totalMultiplier += (effect.getPower() - 1.0);
        }
        return totalMultiplier;
    }

    public void tick() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Effect> entry : effectMap.entrySet()) {
            Effect effect = entry.getValue();
            if (!effect.isPermanent()) {
                effect.tick();
                if (effect.isExpired()) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        for (String name : toRemove) {
            effectMap.remove(name);
        }
    }

    public void clear() {
        effectMap.clear();
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        ListTag effectsList = new ListTag();

        for (Effect effect : effectMap.values()) {
            effectsList.add(effect.save());
        }

        nbt.put("EffectsList", effectsList);
        return nbt;
    }

    public void load(CompoundTag nbt) {
        effectMap.clear();
        if (nbt.contains("EffectsList", Tag.TAG_LIST)) {
            ListTag effectsList = nbt.getList("EffectsList", Tag.TAG_COMPOUND);

            for (int i = 0; i < effectsList.size(); i++) {
                CompoundTag effectTag = effectsList.getCompound(i);
                Effect effect = Effect.load(effectTag);
                effectMap.put(effect.getName().toLowerCase(), effect);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(effectMap.size());
        for (Effect effect : effectMap.values()) {
            effect.toBytes(buf);
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        effectMap.clear();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            Effect effect = Effect.fromBytes(buf);
            effectMap.put(effect.getName().toLowerCase(), effect);
        }
    }

    public void copyFrom(Effects other) {
        this.effectMap.clear();
        for (Map.Entry<String, Effect> entry : other.effectMap.entrySet()) {
            this.effectMap.put(entry.getKey(), entry.getValue().copy());
        }
    }
}

