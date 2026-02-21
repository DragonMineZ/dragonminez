package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;

public class UsedForms {
    private final Map<String, List<String>> usedForms = new HashMap<>();

    public UsedForms() {
    }

    public List<String> getFormGroup(String formGroup) {
        if (!usedForms.containsKey(formGroup)) {
            usedForms.put(formGroup, new ArrayList<>());
        }
        return usedForms.get(formGroup);
    }

    public void putForm(String formGroup, String formName) {
        List<String> existingGroupForms = usedForms.get(formGroup);
        if (existingGroupForms == null) {
            List<String> groupForms = new ArrayList<>();
            groupForms.add(formName);
            usedForms.put(formGroup, groupForms);
        } else if (!existingGroupForms.contains(formName)) {
            List<String> groupForms = new ArrayList<>(existingGroupForms);
            groupForms.add(formName);
            usedForms.put(formGroup, groupForms);
        }
    }

    public void clear() {
        usedForms.clear();
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        for (Map.Entry<String, List<String>> entry : usedForms.entrySet()) {
            nbt.putString(entry.getKey(), String.join(":", entry.getValue()));
        }
        return nbt;
    }

    public void load(CompoundTag nbt) {
        usedForms.clear();
        for (String key : nbt.getAllKeys()) {
            usedForms.put(key, Arrays.stream(nbt.getString(key).split(":")).toList());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(usedForms.size());
        for (Map.Entry<String, List<String>> entry : usedForms.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(String.join(":", entry.getValue()));
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        usedForms.clear();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            List<String> value = Arrays.stream(buf.readUtf().split(":")).toList();
            usedForms.put(key, value);
        }
    }

    public void copyFrom(UsedForms other) {
        this.usedForms.clear();
        this.usedForms.putAll(other.usedForms);
    }
}

