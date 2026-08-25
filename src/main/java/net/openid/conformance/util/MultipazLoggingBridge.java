package net.openid.conformance.util;

import jakarta.annotation.PostConstruct;
import org.multipaz.util.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Routes the multipaz library's own {@link Logger} (which by default prints every level, debug
 * included, straight to stdout with its own timestamp format) through slf4j, so its output is
 * formatted like the rest of the server log and governed by the usual
 * {@code logging.level.org.multipaz[.<tag>]} settings. Messages are logged under
 * {@code org.multipaz.<tag>}.
 */
@Component
public class MultipazLoggingBridge {

	private static final String LOGGER_PREFIX = "org.multipaz.";

	@PostConstruct
	public void install() {
		Logger.INSTANCE.setLogPrinter(MultipazLoggingBridge::print);
		// multipaz formats debug payloads (CBOR/hex dumps) before calling the printer, so only
		// let it do that work when someone has actually enabled debug output for it
		Logger.INSTANCE.setDebugEnabled(LoggerFactory.getLogger("org.multipaz").isDebugEnabled());
	}

	private static void print(Logger.LogPrinter.Level level, String tag, String msg, Throwable throwable) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(LOGGER_PREFIX + tag);
		switch (level) {
			case DEBUG -> logger.debug(msg, throwable);
			case INFO -> logger.info(msg, throwable);
			case WARNING -> logger.warn(msg, throwable);
			case ERROR -> logger.error(msg, throwable);
		}
	}
}
