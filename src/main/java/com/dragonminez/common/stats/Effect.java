package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class Effect {
    private final String name;
    private double power;
    private int duration;

    public Effect(String name, double power, int duration) {
        this.name = name;
        this.power = power;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isPermanent() {
        return duration == -1;
    }

    public void tick() {
        if (duration > 0) {
            duration--;
        }
    }

    public boolean isExpired() {
        return duration == 0;
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Name", name);
        nbt.putDouble("Power", power);
        nbt.putInt("Duration", duration);
        return nbt;
    }

    public static Effect load(CompoundTag nbt) {
        String name = nbt.getString("Name");
        double power = nbt.getDouble("Power");
        int duration = nbt.getInt("Duration");
        return new Effect(name, power, duration);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeDouble(power);
        buf.writeInt(duration);
    }

    public static Effect fromBytes(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        double power = buf.readDouble();
        int duration = buf.readInt();
        return new Effect(name, power, duration);
    }

    public Effect copy() {
        return new Effect(this.name, this.power, this.duration);
    }
}

