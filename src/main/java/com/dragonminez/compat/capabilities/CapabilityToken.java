package com.dragonminez.compat.capabilities;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Compatibility shim for Forge CapabilityToken type capture.
 */
public abstract class CapabilityToken<T> {
	@SuppressWarnings("unchecked")
	Class<T> capture() {
		Type superClass = getClass().getGenericSuperclass();
		if (superClass instanceof ParameterizedType parameterized) {
			Type arg = parameterized.getActualTypeArguments()[0];
			if (arg instanceof Class<?> clazz) {
				return (Class<T>) clazz;
			}
		}
		throw new IllegalStateException("CapabilityToken must be used as an anonymous class with a concrete type argument");
	}
}
