package com.dragonminez.common.compat;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public class WorldGuardCompat {
	private static boolean worldGuardAvailable = false;
	private static IWorldGuardHandler handler = null;

	public static void init() {
		// TBH I'm just trying to do this in many different ways to see if I can get it to work at least once LMAO
		LogUtil.info(Env.SERVER, "Checking for WorldGuard compatibility...");
		try {
			Class<?> wgClass = null;
			try {
				wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
			} catch (ClassNotFoundException e) {
				try {
					Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
					Method getServerMethod = bukkitClass.getMethod("getServer");
					Object server = getServerMethod.invoke(null);
					
					Method getPluginManagerMethod = server.getClass().getMethod("getPluginManager");
					Object pluginManager = getPluginManagerMethod.invoke(server);
					
					Method getPluginMethod = pluginManager.getClass().getMethod("getPlugin", String.class);
					Object plugin = getPluginMethod.invoke(pluginManager, "WorldGuard");
					
					if (plugin != null) {
						ClassLoader pluginLoader = plugin.getClass().getClassLoader();
						wgClass = pluginLoader.loadClass("com.sk89q.worldguard.WorldGuard");
						LogUtil.info(Env.SERVER, "WorldGuard found via Bukkit PluginManager!");
					}
				} catch (Exception ex) {
					LogUtil.info(Env.SERVER, "WorldGuard not found via Bukkit PluginManager.");
				}
			}

			if (wgClass != null) {
				handler = new WorldGuardHandler(wgClass.getClassLoader());
				handler.registerFlags();
				worldGuardAvailable = true;
				LogUtil.info(Env.SERVER, "WorldGuard detected! Ki-griefing flag has been registered.");
			} else {
				throw new ClassNotFoundException("WorldGuard class not found");
			}

		} catch (ClassNotFoundException e) {
			LogUtil.info(Env.SERVER, "WorldGuard not found. Using only gamerules for ki-griefing control.");
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
		private final ClassLoader classLoader;
		private Object kiGriefingFlag;
		private Object gravityFlag;

		public WorldGuardHandler(ClassLoader classLoader) {
			this.classLoader = classLoader != null ? classLoader : this.getClass().getClassLoader();
		}

		private Class<?> getClass(String name) throws ClassNotFoundException {
			return Class.forName(name, true, classLoader);
		}

		@Override
		public void registerFlags() {
			try {
				Class<?> registryClass = getClass("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
				Class<?> worldGuardClass = getClass("com.sk89q.worldguard.WorldGuard");
				Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
				Object flagRegistry = worldGuardClass.getMethod("getFlagRegistry").invoke(worldGuardInstance);

				Class<?> stateFlagClass = getClass("com.sk89q.worldguard.protection.flags.StateFlag");
				Object kiFlagInstance = stateFlagClass.getConstructor(String.class, boolean.class).newInstance("ki-griefing", true);

				Class<?> doubleFlagClass = getClass("com.sk89q.worldguard.protection.flags.DoubleFlag");
				Object gravityFlagInstance = doubleFlagClass.getConstructor(String.class).newInstance("dmz-gravity");

				try {
					registryClass.getMethod("register", getClass("com.sk89q.worldguard.protection.flags.Flag")).invoke(flagRegistry, kiFlagInstance);
					this.kiGriefingFlag = kiFlagInstance;
				} catch (Exception e) {
					try {
						this.kiGriefingFlag = registryClass.getMethod("get", String.class).invoke(flagRegistry, "ki-griefing");
					} catch (Exception ex) {
						LogUtil.error(Env.SERVER, "Failed to retrieve existing ki-griefing flag", ex);
					}
				}

				try {
					registryClass.getMethod("register", getClass("com.sk89q.worldguard.protection.flags.Flag"))
							.invoke(flagRegistry, gravityFlagInstance);
					this.gravityFlag = gravityFlagInstance;
				} catch (Exception e) {
					try {
						this.gravityFlag = registryClass.getMethod("get", String.class).invoke(flagRegistry, "dmz-gravity");
					} catch (Exception ex) {
						LogUtil.error(Env.SERVER, "Failed to retrieve existing dmz-gravity flag", ex);
					}
				}

			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "Error registering WorldGuard flags", e);
			}
		}

		@Override
		public boolean canGrief(Level level, BlockPos pos, Entity source) {
			if (kiGriefingFlag == null) return true;

			try {
				Method getWorldMethod = level.getClass().getMethod("getWorld");
				Object bukkitWorld = getWorldMethod.invoke(level);

				Class<?> locationClass = Class.forName("org.bukkit.Location");
				Class<?> worldClass = Class.forName("org.bukkit.World");
				Object location = locationClass.getConstructor(worldClass, double.class, double.class, double.class)
						.newInstance(bukkitWorld, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());

				Class<?> bukkitAdapterClass = getClass("com.sk89q.worldguard.bukkit.BukkitAdapter");
				Object wgLocation = bukkitAdapterClass.getMethod("adapt", locationClass).invoke(null, location);

				Class<?> worldGuardClass = getClass("com.sk89q.worldguard.WorldGuard");
				Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
				Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuardInstance);
				Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

				Object regionQuery = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

				Class<?> stateFlagClass = getClass("com.sk89q.worldguard.protection.flags.StateFlag");
				Method queryStateMethod = regionQuery.getClass().getMethod("queryState",
						getClass("com.sk89q.worldedit.util.Location"),
						getClass("com.sk89q.worldguard.LocalPlayer"),
						getClass("com.sk89q.worldguard.protection.flags.Flag[]")
				);
				
				Object flagsArray = java.lang.reflect.Array.newInstance(getClass("com.sk89q.worldguard.protection.flags.Flag"), 1);
				java.lang.reflect.Array.set(flagsArray, 0, kiGriefingFlag);

				Object state = queryStateMethod.invoke(regionQuery, wgLocation, null, flagsArray);

				if (state == null) return true; 
				
				Class<?> stateFlagStateClass = getClass("com.sk89q.worldguard.protection.flags.StateFlag$State");
				Object denyState = stateFlagStateClass.getField("DENY").get(null);
				
				return !state.equals(denyState);

			} catch (NoSuchMethodException nsme) {
				return true;
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "Error querying WorldGuard flag state", e);
				return true;
			}
		}

		@Override
		public double getGravityValue(Level level, BlockPos pos, Entity source) {
			if (gravityFlag == null) return 0.0;
			try {
				Method getWorldMethod = level.getClass().getMethod("getWorld");
				Object bukkitWorld = getWorldMethod.invoke(level);

				Class<?> locationClass = Class.forName("org.bukkit.Location");
				Class<?> worldClass = Class.forName("org.bukkit.World");
				Object location = locationClass.getConstructor(worldClass, double.class, double.class, double.class)
						.newInstance(bukkitWorld, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());

				Class<?> bukkitAdapterClass = getClass("com.sk89q.worldguard.bukkit.BukkitAdapter");
				Object wgLocation = bukkitAdapterClass.getMethod("adapt", locationClass).invoke(null, location);

				Class<?> worldGuardClass = getClass("com.sk89q.worldguard.WorldGuard");
				Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
				Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuardInstance);
				Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
				Object regionQuery = regionContainer.getClass().getMethod("createQuery").invoke(regionContainer);

				Method queryValueMethod = regionQuery.getClass().getMethod("queryValue",
						getClass("com.sk89q.worldedit.util.Location"),
						getClass("com.sk89q.worldguard.LocalPlayer"),
						getClass("com.sk89q.worldguard.protection.flags.Flag")
				);

				Object result = queryValueMethod.invoke(regionQuery, wgLocation, null, gravityFlag);

				if (result != null && result instanceof Double) return (Double) result;
				return 0.0;
			} catch (NoSuchMethodException nsme) {
				return 0.0;
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "Error querying Gravity flag", e);
				return 0.0;
			}
		}
	}
}
