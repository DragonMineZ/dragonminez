package com.dragonminez.server.world.gen;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public final class ColumnCache implements DensityFunction {
	public static final KeyDispatchDataCodec<ColumnCache> CODEC = KeyDispatchDataCodec.of(
			DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").xmap(ColumnCache::new, ColumnCache::argument));

	private static final int SLOTS = 16;
	private static final long EMPTY = Long.MIN_VALUE;
	private static final long UNOWNED = -1L;

	private final DensityFunction argument;
	private final AtomicLong owner = new AtomicLong(UNOWNED);
	private final long[] keys = new long[SLOTS];
	private final double[] values = new double[SLOTS];

	public ColumnCache(DensityFunction argument) {
		this.argument = argument;
		Arrays.fill(this.keys, EMPTY);
	}

	public DensityFunction argument() {
		return this.argument;
	}

	@Override
	public double compute(FunctionContext context) {
		long thread = Thread.currentThread().getId();
		if (this.owner.get() != thread && !this.owner.compareAndSet(UNOWNED, thread)) {
			return this.argument.compute(context);
		}
		int x = context.blockX();
		int z = context.blockZ();
		long key = (long) x << 32 | (z & 0xFFFFFFFFL);
		int slot = ((x ^ x >> 2) & 3) | ((z ^ z >> 2) & 3) << 2;
		if (this.keys[slot] == key) return this.values[slot];
		double value = this.argument.compute(context);
		this.keys[slot] = key;
		this.values[slot] = value;
		return value;
	}

	@Override
	public void fillArray(double[] values, ContextProvider provider) {
		provider.fillAllDirectly(values, this);
	}

	@Override
	public DensityFunction mapAll(Visitor visitor) {
		return visitor.apply(new ColumnCache(this.argument.mapAll(visitor)));
	}

	@Override
	public double minValue() {
		return this.argument.minValue();
	}

	@Override
	public double maxValue() {
		return this.argument.maxValue();
	}

	@Override
	public KeyDispatchDataCodec<? extends DensityFunction> codec() {
		return CODEC;
	}
}
