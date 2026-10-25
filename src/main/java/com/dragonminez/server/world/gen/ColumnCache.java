package com.dragonminez.server.world.gen;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public final class ColumnCache implements DensityFunction {
	public static final KeyDispatchDataCodec<ColumnCache> CODEC = KeyDispatchDataCodec.of(
			DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").xmap(ColumnCache::new, ColumnCache::argument));

	private final DensityFunction argument;
	private final Column[] columns = new Column[16];

	public ColumnCache(DensityFunction argument) {
		this.argument = argument;
	}

	public DensityFunction argument() {
		return this.argument;
	}

	@Override
	public double compute(FunctionContext context) {
		int x = context.blockX();
		int z = context.blockZ();
		int slot = (x & 3) | (z & 3) << 2;
		Column column = this.columns[slot];
		if (column != null && column.x == x && column.z == z) return column.value;
		double value = this.argument.compute(context);
		this.columns[slot] = new Column(x, z, value);
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

	private record Column(int x, int z, double value) {
	}
}
