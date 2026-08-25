package net.openid.conformance.info;

import java.util.Optional;

/**
 * Access to the Playwright trace archives recorded by the scripted browser
 * (see {@code PlaywrightBrowserRunner} and the {@code browser.playwright.traceEnabled} setting).
 */
@FunctionalInterface
public interface TraceService {

	/**
	 * @param testId the test instance id
	 * @return the trace archive (a zip file openable with {@code npx playwright show-trace}),
	 *         or empty if no trace was recorded for the test
	 */
	Optional<byte[]> getTraceForTestId(String testId);
}
