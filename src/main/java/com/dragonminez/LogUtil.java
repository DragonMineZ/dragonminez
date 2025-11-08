package com.dragonminez;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class LogUtil {

	private static final Logger LOGGER = LogManager.getLogger();

	private LogUtil() {
	}

	public static void info(Env env, String message, Object... args) {
		LOGGER.info(prefixedMessage(env, message), args);
	}

	public static void warn(Env env, String message, Object... args) {
		LOGGER.warn(prefixedMessage(env, message), args);
	}

	public static void error(Env env, String message, Object... args) {
		LOGGER.error(prefixedMessage(env, message), args);
	}

	public static void debug(Env env, String message, Object... args) {
		LOGGER.debug(prefixedMessage(env, message), args);
	}

	private static String prefixedMessage(Env env, String message) {
		return "[" + env.name() + "] " + message;
	}
}
