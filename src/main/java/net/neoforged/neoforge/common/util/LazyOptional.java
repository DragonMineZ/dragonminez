package com.dragonminez.compat.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Compatibility shim for Forge LazyOptional on NeoForge 1.21 (removed upstream).
 * Backed by {@link Optional}; keeps existing DragonMineZ call sites compiling.
 */
public final class LazyOptional<T> {
	private final Optional<T> value;
	private boolean valid = true;

	private LazyOptional(Optional<T> value) {
		this.value = value;
	}

	public static <T> LazyOptional<T> of(Supplier<T> supplier) {
		return new LazyOptional<>(Optional.ofNullable(supplier.get()));
	}

	public static <T> LazyOptional<T> empty() {
		return new LazyOptional<>(Optional.empty());
	}

	public void ifPresent(Consumer<? super T> consumer) {
		if (valid) value.ifPresent(consumer);
	}

	public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
		return valid ? value.map(mapper) : Optional.empty();
	}

	public Optional<T> resolve() {
		return valid ? value : Optional.empty();
	}

	public boolean isPresent() {
		return valid && value.isPresent();
	}

	public T orElse(T other) {
		return valid ? value.orElse(other) : other;
	}

	public T orElseThrow(Supplier<? extends RuntimeException> exceptionSupplier) {
		if (!valid || value.isEmpty()) throw exceptionSupplier.get();
		return value.get();
	}

	@SuppressWarnings("unchecked")
	public <X> LazyOptional<X> cast() {
		return (LazyOptional<X>) this;
	}

	public void invalidate() {
		valid = false;
	}
}
