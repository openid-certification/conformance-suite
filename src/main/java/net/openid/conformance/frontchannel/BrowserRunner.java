package net.openid.conformance.frontchannel;

import com.google.gson.JsonObject;

import java.util.concurrent.Callable;

/**
 * A scripted browser session that visits one url and runs the tasks from the matching entry of the
 * test configuration's "browser" automation block.
 *
 * <p>Implementations wrap a browser engine ({@link SeleniumBrowserRunner} drives HtmlUnit through
 * Selenium, {@link PlaywrightBrowserRunner} drives a real Chromium/Firefox/WebKit). Each instance
 * owns its own browser, is submitted to the test's {@code TestExecutionManager} by
 * {@link BrowserControl#goToUrl} and runs on its own thread; it must call
 * {@link BrowserControl#removeRunner} when it finishes.
 */
public interface BrowserRunner extends Callable<String> {

	/**
	 * Snapshot of the runner's state as shown on the running-test page (see the "runners" entry
	 * of the {@code /api/runner/{id}} response).
	 *
	 * <p>This is called from the HTTP request thread while the runner is executing, so
	 * implementations must only expose values that are safe to read concurrently.
	 */
	JsonObject getStatus();
}
