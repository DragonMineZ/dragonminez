package com.dragonminez.common.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class WeaponLevelingCompat {
	public static final String MOD_ID = "weaponleveling";
	private static final Logger LOGGER = LogUtils.getLogger();
	private static Boolean loaded;

	private WeaponLevelingCompat() {}

	public static boolean isLoaded() {
		if (loaded == null) loaded = ModList.get().isLoaded(MOD_ID);
		return loaded;
	}

	public static void copyLevel(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyLevel(from, to);
	}

	public static void copyLevelProgress(ItemStack from, ItemStack to) {
		if (!isLoaded()) return;
		Impl.copyLevelProgress(from, to);
	}

	private static final class Impl {
		static void copyLevel(ItemStack from, ItemStack to) {
			copy("updateLevel", "getLevel", from, to);
		}

		static void copyLevelProgress(ItemStack from, ItemStack to) {
			copy("updateLevelProgress", "getLevelProgress", from, to);
		}

		private static void copy(String setter, String getter, ItemStack from, ItemStack to) {
			try {
				Class<?> api = Class.forName("net.weaponleveling.api.LevelingAPI");
				Object value = invoke(api, getter, from);
				invoke(api, setter, to, value);
			} catch (ReflectiveOperationException | LinkageError exception) {
				LOGGER.warn("Unable to copy Weapon Leveling item data using {}", getter, exception);
			}
		}

		private static Object invoke(Class<?> type, String name, Object... arguments) throws ReflectiveOperationException {
			for (Method method : type.getMethods()) {
				if (!method.getName().equals(name) || method.getParameterCount() != arguments.length
						|| !Modifier.isStatic(method.getModifiers())) continue;
				Class<?>[] parameters = method.getParameterTypes();
				boolean compatible = true;
				for (int index = 0; index < parameters.length; index++) {
					if (!supports(parameters[index], arguments[index])) {
						compatible = false;
						break;
					}
				}
				if (compatible) return method.invoke(null, arguments);
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
