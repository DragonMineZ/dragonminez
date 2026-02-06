package com.dragonminez.common.compat;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

public class WorldGuardCompat {
	private static boolean worldGuardAvailable = false;
	private static IWorldGuardHandler handler = null;

	public static void init() {
		try {
			Class.forName("com.sk89q.worldguard.WorldGuard");

			handler = new WorldGuardHandler();
			handler.registerFlags();
			worldGuardAvailable = true;

			LogUtil.info(Env.SERVER, "WorldGuard detected! Ki-griefing flag has been registered.");
		} catch (ClassNotFoundException e) {
			LogUtil.debug(Env.SERVER, "WorldGuard not found. Using only gamerules for ki-griefing control.");
			worldGuardAvailable = false;
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Failed to initialize WorldGuard compatibility", e);
			worldGuardAvailable = false;
		}
	}


	public static boolean canGrief(Level level, BlockPos pos, Entity source) {
		if (!worldGuardAvailable || handler == null) {
			return true;
		}

		try {
			return handler.canGrief(level, pos, source);
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "Error checking WorldGuard flag at " + pos, e);
			return true;
		}
	}

	public static double getGravity(Level level, BlockPos pos, Entity source) {
		if (!worldGuardAvailable || handler == null) return 0.0;
		return handler.getGravityValue(level, pos, source);
	}

	private interface IWorldGuardHandler {
		void registerFlags();

		boolean canGrief(Level level, BlockPos pos, Entity source);

		double getGravityValue(Level level, BlockPos pos, Entity source);
	}

	private static class WorldGuardHandler implements IWorldGuardHandler {
		private Object kiGriefingFlag;
		private Object gravityFlag;

		@Override
		public void registerFlags() {
			try {
				Class<?> registryClass = Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
				Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
				Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
				Object flagRegistry = worldGuardClass.getMethod("getFlagRegistry").invoke(worldGuardInstance);

				Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
				Object kiFlagInstance = stateFlagClass.getConstructor(String.class, boolean.class).newInstance("ki-griefing", true);

				Class<?> doubleFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.DoubleFlag");
				Object gravityFlagInstance = doubleFlagClass.getConstructor(String.class).newInstance("dmz-gravity");

				try {
					registryClass.getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag")).invoke(flagRegistry, kiFlagInstance);
					this.kiGriefingFlag = kiFlagInstance;
				} catch (Exception e) {
					this.kiGriefingFlag = registryClass.getMethod("get", String.class).invoke(flagRegistry, "ki-griefing");
				}

				try {
					registryClass.getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
							.invoke(flagRegistry, gravityFlagInstance);
					this.gravityFlag = gravityFlagInstance;
				} catch (Exception e) {
					this.gravityFlag = registryClass.getMethod("get", String.class).invoke(flagRegistry, "dmz-gravity");
				}

			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "Error registering WorldGuard flags", e);
			}
		}

		@Override
		public boolean canGrief(Level level, BlockPos pos, Entity source) {
			if (kiGriefingFlag == null) return true;

			try {
				Object bukkitWorld = level.getClass().getMethod("getWorld").invoke(level);

				Class<?> locationClass = Class.forName("org.bukkit.Location");
				Object location = locationClass.getConstructor(
						Class.forName("org.bukkit.World"),
						double.class, double.class, double.class
				).newInstance(bukkitWorld, pos.getX(), pos.getY(), pos.getZ());

				Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
				Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
				Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuardInstance);
				Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
				Object regionQuery = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

				Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
				Object wgLocation = bukkitAdapterClass.getMethod("adapt", locationClass).invoke(null, location);

				Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
				Class<?> stateFlagStateClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag$State");

				Object state = regionQuery.getClass().getMethod("queryState",
						Class.forName("com.sk89q.worldedit.util.Location"),
						Class.forName("com.sk89q.worldguard.LocalPlayer"),
						stateFlagClass
				).invoke(regionQuery, wgLocation, null, kiGriefingFlag);

				Object denyState = stateFlagStateClass.getField("DENY").get(null);
				return state != denyState;

			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "Error querying WorldGuard flag state", e);
				return true;
			}
		}

		@Override
		public double getGravityValue(Level level, BlockPos pos, Entity source) {
			if (gravityFlag == null) return 0.0;
			try {
				Class<?> vectorClass = Class.forName("com.sk89q.worldedit.util.Location");
				Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
				Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
				Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuardInstance);
				Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
				Object regionQuery = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

				Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
				Class<?> locationClass = Class.forName("org.bukkit.Location");
				Object bukkitLoc = locationClass.getConstructor(Class.forName("org.bukkit.World"), double.class, double.class, double.class).newInstance(level.getClass().getMethod("getWorld").invoke(level), pos.getX(), pos.getY(), pos.getZ());
				Object wgLocation = bukkitAdapterClass.getMethod("adapt", locationClass).invoke(null, bukkitLoc);

				Method queryMethod = regionQuery.getClass().getMethod("queryValue",
						Class.forName("com.sk89q.worldedit.util.Location"),
						Class.forName("com.sk89q.worldguard.LocalPlayer"),
						Class.forName("com.sk89q.worldguard.protection.flags.Flag")
				);

				Object result = queryMethod.invoke(regionQuery, wgLocation, null, gravityFlag);

				if (result != null && result instanceof Double) return (Double) result;
				return 0.0;
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "Error querying Gravity flag", e);
				return 0.0;
			}
		}
	}
}
