package com.dragonminez.common.stats.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecondaryStatEffects {
	public static final String STR = "STR";
	public static final String SKP = "SKP";
	public static final String DEF = "DEF";
	public static final String PWR = "PWR";
	public static final String STM_REGEN = "STM_REGEN";
	public static final String HP_REGEN = "HP_REGEN";
	public static final String ENE_REGEN = "ENE_REGEN";

	private final Map<String, Mod> mods = new HashMap<>();

	public static class Mod {
		private final String stat;
		private double factor;
		private int duration;

		public Mod(String stat, double factor, int duration) {
			this.stat = stat;
			this.factor = factor;
			this.duration = duration;
		}

		public String getStat() { return stat; }
		public double getFactor() { return factor; }
		public int getDuration() { return duration; }
	}

	public void apply(String stat, double factor, int durationTicks) {
		if (stat == null || durationTicks <= 0 || factor == 0.0) return;
		String key = stat.toUpperCase();
		Mod existing = mods.get(key);
		if (existing == null) {
			mods.put(key, new Mod(key, factor, durationTicks));
			return;
		}
		if (Math.abs(factor) >= Math.abs(existing.factor)) existing.factor = factor;
		existing.duration = Math.max(existing.duration, durationTicks);
	}

	public double getMultiplier(String stat) {
		if (stat == null) return 1.0;
		Mod mod = mods.get(stat.toUpperCase());
		return mod == null ? 1.0 : Math.max(0.0, 1.0 + mod.factor);
	}

	public boolean hasModifier(String stat) {
		return stat != null && mods.containsKey(stat.toUpperCase());
	}

	public List<Mod> getActiveModifiers() {
		return new ArrayList<>(mods.values());
	}

	public boolean isEmpty() {
		return mods.isEmpty();
	}

	public void tick() {
		if (mods.isEmpty()) return;
		mods.values().removeIf(mod -> {
			if (mod.duration > 0) mod.duration--;
			return mod.duration <= 0;
		});
	}

	public void clear() {
		mods.clear();
	}

	public CompoundTag save() {
		CompoundTag nbt = new CompoundTag();
		ListTag list = new ListTag();
		for (Mod mod : mods.values()) {
			CompoundTag modTag = new CompoundTag();
			modTag.putString("Stat", mod.stat);
			modTag.putDouble("Factor", mod.factor);
			modTag.putInt("Duration", mod.duration);
			list.add(modTag);
		}
		nbt.put("Modifiers", list);
		return nbt;
	}

	public void load(CompoundTag nbt) {
		mods.clear();
		if (!nbt.contains("Modifiers", Tag.TAG_LIST)) return;
		ListTag list = nbt.getList("Modifiers", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag modTag = list.getCompound(i);
			String stat = modTag.getString("Stat");
			if (stat == null || stat.isEmpty()) continue;
			mods.put(stat.toUpperCase(), new Mod(stat.toUpperCase(), modTag.getDouble("Factor"), modTag.getInt("Duration")));
		}
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(mods.size());
		for (Mod mod : mods.values()) {
			buf.writeUtf(mod.stat);
			buf.writeDouble(mod.factor);
			buf.writeInt(mod.duration);
		}
	}

	public void fromBytes(FriendlyByteBuf buf) {
		mods.clear();
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			String stat = buf.readUtf().toUpperCase();
			double factor = buf.readDouble();
			int duration = buf.readInt();
			mods.put(stat, new Mod(stat, factor, duration));
		}
	}

	public void copyFrom(SecondaryStatEffects other) {
		this.mods.clear();
		for (Mod mod : other.mods.values()) {
			this.mods.put(mod.stat, new Mod(mod.stat, mod.factor, mod.duration));
		}
	}
}
