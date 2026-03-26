package com.dragonminez;

import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.OnStartupTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LogUtil {
	private static final String LOGGER_NAME = Reference.MOD_ID;
	private static final String APPENDER_NAME = "DragonMineZRollingFile";
	private static final Logger LOGGER = LogManager.getLogger(LOGGER_NAME);
	private static volatile boolean configured;

	private LogUtil() {}

	public static void info(Env env, String message, Object... args) {
		log(Level.INFO, env, message, args);
	}

	public static void warn(Env env, String message, Object... args) {
		log(Level.WARN, env, message, args);
	}

	public static void error(Env env, String message, Object... args) {
		log(Level.ERROR, env, message, args);
	}

	public static void debug(Env env, String message, Object... args) {
		log(Level.DEBUG, env, message, args);
	}

	private static void log(Level level, Env env, String message, Object... args) {
		ensureLoggerConfigured();
		LOGGER.log(level, prefixedMessage(env, message), args);
	}

	private static void ensureLoggerConfigured() {
		if (configured) return;

		synchronized (LogUtil.class) {
			if (configured) return;

			try {
				LoggerContext context = (LoggerContext) LogManager.getContext(false);
				Configuration configuration = context.getConfiguration();

				if (configuration.getAppender(APPENDER_NAME) == null) {
					Path logsDir = FMLPaths.GAMEDIR.get().resolve("logs");
					Files.createDirectories(logsDir);

					String fileName = logsDir.resolve("dragonminez.log").toString();
					String filePattern = logsDir.resolve("dmz-%d{yyyy-MM-dd}-%i.log.gz").toString();

					PatternLayout layout = PatternLayout.newBuilder()
							.withConfiguration(configuration)
							.withPattern("[%d{HH:mm:ss}] [%t/%level] [%logger]: %msg%n")
							.build();

					CompositeTriggeringPolicy policy = CompositeTriggeringPolicy.createPolicy(
							OnStartupTriggeringPolicy.createPolicy(1),
							SizeBasedTriggeringPolicy.createPolicy("10MB")
					);

					DefaultRolloverStrategy rolloverStrategy = DefaultRolloverStrategy.newBuilder()
							.withMax("10")
							.withConfig(configuration)
							.build();

					RollingFileAppender appender = RollingFileAppender.newBuilder()
							.setConfiguration(configuration)
							.setName(APPENDER_NAME)
							.withFileName(fileName)
							.withFilePattern(filePattern)
							.withPolicy(policy)
							.withStrategy(rolloverStrategy)
							.setLayout(layout)
							.setIgnoreExceptions(false)
							.build();

					appender.start();
					configuration.addAppender(appender);

					LoggerConfig loggerConfig = configuration.getLoggerConfig(LOGGER_NAME);
					if (!LOGGER_NAME.equals(loggerConfig.getName())) {
						loggerConfig = new LoggerConfig(LOGGER_NAME, Level.ALL, true);
						configuration.addLogger(LOGGER_NAME, loggerConfig);
					}

					loggerConfig.addAppender(appender, Level.ALL, null);
					context.updateLoggers();
				}
			} catch (Exception exception) {
				LogManager.getLogger(LOGGER_NAME).warn("Failed to configure DMZ dedicated logger: {}", exception.getMessage());
			}

			configured = true;
		}
	}

	private static String prefixedMessage(Env env, String message) {
		return "[DMZ-" + env.name() + "] " + message;
	}
}
