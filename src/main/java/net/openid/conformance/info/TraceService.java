package net.openid.conformance.info;

import java.util.Optional;

/**
 * Access to the Playwright trace archives recorded by the scripted browser
 * (see {@code PlaywrightBrowserRunner} and the {@code browser.playwright.traceEnabled} setting).
 */
public interface TraceService {

	/**
	 * @param testId the test instance id
	 * @return the trace archive (a zip file openable with {@code npx playwright show-trace}),
	 *         or empty if no trace was recorded for the test
	 */
	Optional<byte[]> getTraceForTestId(String testId);

	/**
	 * Removes the trace recorded for a test, if any; called when the test is deleted so that
	 * traces don't outlive their tests. Never throws: a trace that can't be removed is logged.
	 *
	 * @param testId the test instance id
	 */
	void deleteTraceForTestId(String testId);
}
