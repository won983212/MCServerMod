package com.won983212.servermod;

import org.apache.logging.log4j.LogManager;

public class Logger {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();

    public static void info(Object obj) {
        LOGGER.info("[" + ServerMod.MODID + "] " + obj);
    }

    public static void warn(Object obj) {
        LOGGER.warn("[" + ServerMod.MODID + "] " + obj);
    }

    public static void error(String message) {
        LOGGER.error("[" + ServerMod.MODID + "] " + message);
    }

    public static void error(Throwable obj) {
        LOGGER.error("[" + ServerMod.MODID + "] ", obj);
    }

    public static void debug(Object obj) {
        LOGGER.debug("[" + ServerMod.MODID + "] " + obj);
    }
}