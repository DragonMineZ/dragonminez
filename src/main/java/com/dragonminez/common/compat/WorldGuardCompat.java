package com.dragonminez.common.compat;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGuardCompat {
    private static boolean worldGuardAvailable = false;
    private static IWorldGuardHandler handler = null;

    public static void init() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");

            handler = new WorldGuardHandler();
            handler.registerFlag();
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

    public static boolean isWorldGuardAvailable() {
        return worldGuardAvailable;
    }

    private interface IWorldGuardHandler {
        void registerFlag();
        boolean canGrief(Level level, BlockPos pos, Entity source);
    }

    private static class WorldGuardHandler implements IWorldGuardHandler {
        private Object kiGriefingFlag = null;

        @Override
        public void registerFlag() {
            try {
                Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
                Object flagRegistry = worldGuardClass.getMethod("getFlagRegistry").invoke(worldGuardInstance);

                Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
                Object flag = stateFlagClass.getConstructor(String.class, boolean.class)
                        .newInstance("ki-griefing", true);

                Class<?> flagRegistryClass = Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
                flagRegistryClass.getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
                        .invoke(flagRegistry, flag);

                kiGriefingFlag = flag;
                LogUtil.info(Env.SERVER, "Successfully registered WorldGuard flag: ki-griefing (default: true)");
            } catch (ClassNotFoundException e) {
                LogUtil.warn(Env.SERVER, "WorldGuard classes not found during flag registration");
            } catch (Exception e) {
                try {
                    Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                    Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
                    Object flagRegistry = worldGuardClass.getMethod("getFlagRegistry").invoke(worldGuardInstance);

                    Class<?> flagRegistryClass = Class.forName("com.sk89q.worldguard.protection.flags.registry.FlagRegistry");
                    Object existingFlag = flagRegistryClass.getMethod("get", String.class)
                            .invoke(flagRegistry, "ki-griefing");

                    if (existingFlag != null) {
                        kiGriefingFlag = existingFlag;
                        LogUtil.info(Env.SERVER, "Using existing WorldGuard flag: ki-griefing");
                    } else {
                        LogUtil.error(Env.SERVER, "Failed to register or retrieve ki-griefing flag", e);
                    }
                } catch (Exception ex) {
                    LogUtil.error(Env.SERVER, "Failed to retrieve existing ki-griefing flag", ex);
                }
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
    }
}
