package net.openid.conformance.info;

import java.util.List;
import java.util.Optional;

/**
 * Access to the Playwright trace archives recorded by the scripted browser
 * (see {@code PlaywrightBrowserRunner} and the {@code browser.playwright.traceEnabled} setting).
 *
 * <p>A test has one trace per scripted browser visit that kept one. Visits are numbered from 1 in
 * the order the test requested them; a visit without a trace leaves a gap.
 */
public interface TraceService {

	/**
	 * @param visit the number of the scripted browser visit the trace is of
	 * @param size  the size of the archive in bytes
	 */
	record TraceInfo(int visit, long size) { }

	/**
	 * @param testId the test instance id
	 * @return the traces recorded for the test, by visit; empty if there are none
	 */
	List<TraceInfo> listTraces(String testId);

	/**
	 * @param testId the test instance id
	 * @param visit  the number of the scripted browser visit
	 * @return the trace archive (a zip file openable with {@code npx playwright show-trace}),
	 *         or empty if no trace was recorded for that visit
	 */
	Optional<byte[]> getTrace(String testId, int visit);

	/**
	 * Removes every trace recorded for a test; called when the test is deleted so that traces
	 * don't outlive their tests. Never throws: a trace that can't be removed is logged.
	 *
	 * @param testId the test instance id
	 */
	void deleteTracesForTestId(String testId);
}
