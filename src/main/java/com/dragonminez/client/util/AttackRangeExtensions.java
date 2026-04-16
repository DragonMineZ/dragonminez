package com.dragonminez.client.util;

import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AttackRangeExtensions {
	public record Context(Player player, double attackRange) { }

	public record Modifier(double value, Operation operation) {
		public int operationOrder() { return operation.order; }
	}

	public enum Operation {
		ADD(0), MULTIPLY(1);
		public final int order;
		Operation(int order) { this.order = order; }
		public int getOrder() { return order; }
	}

	private static final ArrayList<Function<Context, Modifier>> sources = new ArrayList<>();

	public static void register(Function<Context, Modifier> source) {
		sources.add(source);
		sources.sort((a, b) -> {
			var modA = a.apply(new Context(null, 0));
			var modB = b.apply(new Context(null, 0));
			if (modA == null && modB == null) return 0;
			if (modA == null) return 1;
			if (modB == null) return -1;
			return Integer.compare(modA.operationOrder(), modB.operationOrder());
		});
	}

	public static List<Function<Context, Modifier>> sources() {
		return sources;
	}
}