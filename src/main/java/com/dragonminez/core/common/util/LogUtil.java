package com.dragonminez.core.common.util;

import com.dragonminez.mod.common.Reference;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for centralized logging in the mod.
 * <p>
 * Provides simplified logging methods for various levels (info, warn, error, debug), supports
 * parameterized messages with varargs, and integrates mod loading warnings. Also provides methods
 * for generating custom crash reports.
 * </p>
 * <p>
 * This class is not instantiable and only contains static methods.
 * </p>
 */
public final class LogUtil {

  private static final Logger LOGGER = LogManager.getLogger();

  /**
   * Logs an informational message with optional parameters.
   *
   * @param message The message string, possibly containing placeholders.
   * @param args    Optional arguments referenced by the format specifiers in the message.
   */
  public static void info(String message, Object... args) {
    LOGGER.info(message, args);
  }

  /**
   * Logs a warning message with optional parameters.
   *
   * @param message The warning message string, possibly containing placeholders.
   * @param args    Optional arguments referenced by the format specifiers in the message.
   */
  public static void warn(String message, Object... args) {
    LOGGER.warn(message, args);
  }

  /**
   * Logs a mod-specific warning message.
   * <p>
   * Logs the warning and additionally issues a mod loading warning to notify users during mod
   * initialization.
   * </p>
   *
   * @param message The warning message string, possibly containing placeholders.
   * @param args    Optional arguments referenced by the format specifiers in the message.
   */
  public static void warnMod(String message, Object... args) {
    LOGGER.warn(message, args);
    logModWarning(String.format(message, args));
  }

  /**
   * Logs an error message with optional parameters.
   *
   * @param message The error message string, possibly containing placeholders.
   * @param args    Optional arguments referenced by the format specifiers in the message.
   */
  public static void error(String message, Object... args) {
    LOGGER.error(message, args);
  }

  /**
   * Logs a debug message with optional parameters.
   * <p>
   * Will only appear if debug logging is enabled.
   * </p>
   *
   * @param message The debug message string, possibly containing placeholders.
   * @param args    Optional arguments referenced by the format specifiers in the message.
   */
  public static void debug(String message, Object... args) {
    LOGGER.debug(message, args);
  }

  /**
   * Issues a mod loading warning through Forge's mod loading system.
   *
   * @param message The warning message to display to users during mod loading.
   */
  private static void logModWarning(String message) {
    IModInfo modInfo = ModLoadingContext.get().getActiveContainer().getModInfo();
    ModLoadingWarning modLoadingWarning = new ModLoadingWarning(modInfo, ModLoadingStage.CONSTRUCT,
        message);
    ModLoader.get().addWarning(modLoadingWarning);
  }

  /**
   * Creates a detailed crash report and terminates the game.
   * <p>
   * The crash report contains mod-specific information and the provided exception cause.
   * </p>
   *
   * @param errorMessage The message to include in the crash report cause.
   * @param exception    The throwable that caused the crash.
   * @throws ReportedException Always thrown to terminate the game with the crash report.
   */
  public static void crash(String errorMessage, Throwable exception) {
    CrashReport crashReport = new CrashReport("Critical Mod Error", exception);
    CrashReportCategory category = crashReport.addCategory("Mod Information");
    category.setDetail("Mod ID", Reference.MOD_ID);
    category.setDetail("Cause", errorMessage
        + ". Please report this issue via ticket with the complete crash report file!");
    throw new ReportedException(crashReport);
  }

  /**
   * Creates a detailed crash report with a generated Throwable and terminates the game.
   *
   * @param errorMessage The message to include in the crash report cause.
   * @throws ReportedException Always thrown to terminate the game with the crash report.
   */
  public static void crash(String errorMessage) {
    crash(errorMessage, new Throwable(errorMessage));
  }

  // Prevent instantiation
  private LogUtil() {
  }
}
