package org.elysian.velocity.utils

import org.elysian.velocity.ElysianVelocity
import org.slf4j.Logger as SLF4JLogger

/**
 * Logging utility wrapper
 * Provides convenient logging methods with automatic debug check
 */
class Logger(private val plugin: ElysianVelocity) {

    private val logger: SLF4JLogger = plugin.logger

    /**
     * Log info message
     */
    fun info(message: String) {
        logger.info(message)
    }

    /**
     * Log warning message
     */
    fun warn(message: String) {
        logger.warn(message)
    }

    /**
     * Log error message
     */
    fun error(message: String) {
        logger.error(message)
    }

    /**
     * Log error with exception
     */
    fun error(message: String, throwable: Throwable) {
        logger.error(message, throwable)
    }

    /**
     * Log debug message (only if debug mode is enabled)
     */
    fun debug(message: String) {
        if (isDebugEnabled()) {
            logger.info("[DEBUG] $message")
        }
    }

    /**
     * Log trace message (very detailed debug)
     */
    fun trace(message: String) {
        if (isDebugEnabled()) {
            logger.info("[TRACE] $message")
        }
    }

    /**
     * Check if debug mode is enabled
     */
    fun isDebugEnabled(): Boolean {
        return plugin.configManager.getBoolean("general.debug", false)
    }

    /**
     * Log method entry (for tracing)
     */
    fun entering(className: String, methodName: String) {
        trace("-> $className.$methodName()")
    }

    /**
     * Log method exit (for tracing)
     */
    fun exiting(className: String, methodName: String) {
        trace("<- $className.$methodName()")
    }

    /**
     * Log method exit with return value
     */
    fun exiting(className: String, methodName: String, result: Any?) {
        trace("<- $className.$methodName() = $result")
    }

    /**
     * Log formatted message
     */
    fun format(message: String, vararg args: Any?) {
        info(message.format(*args))
    }

    /**
     * Log with prefix
     */
    fun logWithPrefix(prefix: String, message: String) {
        info("[$prefix] $message")
    }

    /**
     * Log performance metrics
     */
    fun performance(operation: String, durationMs: Long) {
        if (isDebugEnabled()) {
            debug("Performance: $operation took ${durationMs}ms")
        }
    }

    /**
     * Log with different levels based on condition
     */
    fun conditional(condition: Boolean, message: String) {
        if (condition) {
            info(message)
        } else {
            debug(message)
        }
    }

    /**
     * Log separator line
     */
    fun separator() {
        info("=" .repeat(50))
    }

    /**
     * Log banner
     */
    fun banner(text: String) {
        val line = "=" .repeat(50)
        info(line)
        info(text.padStart((50 + text.length) / 2).padEnd(50))
        info(line)
    }

    companion object {
        /**
         * Measure execution time of a block
         */
        inline fun <T> measureTime(logger: Logger, operation: String, block: () -> T): T {
            val start = System.currentTimeMillis()
            return try {
                block()
            } finally {
                val duration = System.currentTimeMillis() - start
                logger.performance(operation, duration)
            }
        }
    }
}