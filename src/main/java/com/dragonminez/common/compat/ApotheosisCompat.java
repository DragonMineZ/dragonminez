package com.dragonminez.common.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ApotheosisCompat {
	public static final String MOD_ID = "apotheosis";
	private static final Logger LOGGER = LogUtils.getLogger();
	private static Boolean loaded;
	private ApotheosisCompat() {}

	public static boolean isLoaded() {
		if (loaded == null) loaded = ModList.get().isLoaded(MOD_ID);
		return loaded;
	}

	public static void copyRarity(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyRarity(from, to);
	}

	public static void copyAffixes(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyAffixes(from, to);
	}

	public static void copySockets(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copySockets(from, to);
	}

	public static void copyGems(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyGems(from, to);
	}

	private static final class Impl {
		static void copyRarity(ItemStack from, ItemStack to) {
			try {
				Object rarity = invokeStatic("dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper", "getRarity", from);
				if ((boolean) invoke(rarity, "isBound")) {
					invokeStatic("dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper", "setRarity", to, invoke(rarity, "get"));
				}
			} catch (ReflectiveOperationException | LinkageError exception) {
				LOGGER.warn("Unable to copy Apotheosis item rarity", exception);
			}
		}

		static void copyAffixes(ItemStack from, ItemStack to) {
			copy("dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper", "getAffixes", "setAffixes", from, to);
		}

		static void copySockets(ItemStack from, ItemStack to) {
			copy("dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper", "getSockets", "setSockets", from, to);
		}

		static void copyGems(ItemStack from, ItemStack to) {
			copy("dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper", "getGems", "setGems", from, to);
		}

		private static void copy(String className, String getter, String setter, ItemStack from, ItemStack to) {
			try {
				Object value = invokeStatic(className, getter, from);
				invokeStatic(className, setter, to, value);
			} catch (ReflectiveOperationException | LinkageError exception) {
				LOGGER.warn("Unable to copy Apotheosis item data using {}", getter, exception);
			}
		}

		private static Object invokeStatic(String className, String name, Object... arguments) throws ReflectiveOperationException {
			Class<?> type = Class.forName(className);
			return invokeMethod(type, null, name, arguments);
		}

		private static Object invoke(Object target, String name, Object... arguments) throws ReflectiveOperationException {
			return invokeMethod(target.getClass(), target, name, arguments);
		}

		private static Object invokeMethod(Class<?> type, Object target, String name, Object... arguments) throws ReflectiveOperationException {
			for (Method method : type.getMethods()) {
				if (!method.getName().equals(name) || method.getParameterCount() != arguments.length
						|| (target == null) != Modifier.isStatic(method.getModifiers())) continue;
				Class<?>[] parameters = method.getParameterTypes();
				boolean compatible = true;
				for (int index = 0; index < parameters.length; index++) {
					if (!supports(parameters[index], arguments[index])) {
						compatible = false;
						break;
					}
				}
				if (compatible) return method.invoke(target, arguments);
			}
			throw new NoSuchMethodException(type.getName() + "." + name);
		}

		private static boolean supports(Class<?> parameter, Object argument) {
			if (argument == null) return !parameter.isPrimitive();
			if (!parameter.isPrimitive()) return parameter.isInstance(argument);
			return parameter == boolean.class && argument instanceof Boolean
					|| parameter == int.class && argument instanceof Integer
					|| parameter == long.class && argument instanceof Long
					|| parameter == float.class && argument instanceof Float
					|| parameter == double.class && argument instanceof Double
					|| parameter == short.class && argument instanceof Short
					|| parameter == byte.class && argument instanceof Byte
					|| parameter == char.class && argument instanceof Character;
		}
	}
}
