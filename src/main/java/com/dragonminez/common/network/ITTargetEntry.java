package com.dragonminez.common.network;

import net.minecraft.network.FriendlyByteBuf;

public class ITTargetEntry {
	public enum Type {
		MASTER, PARTY, EXTERNAL
	}

	private final Type type;
	private final String id;
	private final String name;
	private final String dimension;
	private final boolean reachable;

	public ITTargetEntry(Type type, String id, String name, String dimension, boolean reachable) {
		this.type = type;
		this.id = id;
		this.name = name;
		this.dimension = dimension;
		this.reachable = reachable;
	}

	public Type getType() { return type; }
	public String getId() { return id; }
	public String getName() { return name; }
	public String getDimension() { return dimension; }
	public boolean isReachable() { return reachable; }

	public int getPriority() {
		return switch (type) {
			case MASTER -> 1;
			case PARTY -> 2;
			case EXTERNAL -> 3;
		};
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeEnum(type);
		buf.writeUtf(id);
		buf.writeUtf(name);
		buf.writeUtf(dimension);
		buf.writeBoolean(reachable);
	}

	public static ITTargetEntry read(FriendlyByteBuf buf) {
		Type type = buf.readEnum(Type.class);
		String id = buf.readUtf();
		String name = buf.readUtf();
		String dimension = buf.readUtf();
		boolean reachable = buf.readBoolean();
		return new ITTargetEntry(type, id, name, dimension, reachable);
	}
}
