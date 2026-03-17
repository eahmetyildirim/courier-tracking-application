package com.migros.couriertracking.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Spring Boot compatible centralized logging utility class.
 * Works as a wrapper over SLF4J and provides request-scoped log tracking
 * with MDC (Mapped Diagnostic Context) support.
 *
 * <p>Usage:
 * <pre>
 *   private static final LoggerUtil logger = LoggerUtil.of(MyClass.class);
 *
 *   logger.info("Processing request for courier {}", courierId);
 *   logger.error("Unexpected error occurred", exception);
 *
 *   // Context-based logging with MDC
 *   try (var ctx = logger.withContext("courierId", "42")) {
 *       logger.info("This log carries courierId=42 MDC info");
 *   }
 * </pre>
 */
public final class LoggerUtil {

    private final Logger logger;

    private LoggerUtil(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    /**
     * Creates a LoggerUtil instance for the given class.
     *
     * @param clazz the class to create the logger for
     * @return new LoggerUtil instance
     */
    public static LoggerUtil of(Class<?> clazz) {
        return new LoggerUtil(clazz);
    }

    // ── INFO ─────────────────────────────────────────────────────────────

    public void info(String message) {
        logger.info(message);
    }

    public void info(String format, Object... args) {
        logger.info(format, args);
    }

    // ── WARN ─────────────────────────────────────────────────────────────

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String format, Object... args) {
        logger.warn(format, args);
    }

    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    // ── ERROR ────────────────────────────────────────────────────────────

    public void error(String message) {
        logger.error(message);
    }

    public void error(String format, Object... args) {
        logger.error(format, args);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    // ── DEBUG ────────────────────────────────────────────────────────────

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String format, Object... args) {
        logger.debug(format, args);
    }

    // ── TRACE ────────────────────────────────────────────────────────────

    public void trace(String message) {
        logger.trace(message);
    }

    public void trace(String format, Object... args) {
        logger.trace(format, args);
    }

    // ── Level checks ─────────────────────────────────────────────────────

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    // ── MDC Support ──────────────────────────────────────────────────────

    /**
     * Adds a key-value pair to MDC and returns an AutoCloseable.
     * When used with try-with-resources, the MDC entry is automatically cleaned up at block end.
     *
     * <pre>
     *   try (var ctx = logger.withContext("orderId", "12345")) {
     *       logger.info("This log carries orderId MDC info");
     *   }
     *   // orderId removed from MDC
     * </pre>
     *
     * @param key   MDC key
     * @param value MDC value
     * @return AutoCloseable that cleans up the MDC entry at block end
     */
    public MdcContext withContext(String key, String value) {
        return new MdcContext(key, value);
    }

    /**
     * AutoCloseable wrapper for MDC context management.
     * Guarantees MDC cleanup when used with try-with-resources.
     */
    public static class MdcContext implements AutoCloseable {

        private final String key;

        MdcContext(String key, String value) {
            this.key = key;
            MDC.put(key, value);
        }

        @Override
        public void close() {
            MDC.remove(key);
        }
    }
}
