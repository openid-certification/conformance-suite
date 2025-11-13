package net.openid.conformance.frontchannel;

import java.util.concurrent.Callable;

/**
 * Interface for browser automation runners.
 * Implementations provide different browser engines (Selenium/HtmlUnit, Playwright, etc.)
 *
 * Browser runners are responsible for:
 * - Navigating to URLs
 * - Executing browser automation commands from JSON configuration
 * - Handling browser interactions (clicks, text entry, waits)
 * - Logging all actions to the event log
 *
 * Each runner instance is executed in a separate thread via ExecutorService
 * and should maintain its own browser instance for thread safety.
 */
public interface IBrowserRunner extends Callable<String> {

	/**
	 * Execute the browser automation workflow.
	 * This is called by the ExecutorService when the runner is submitted.
	 *
	 * @return The final URL after all navigation and commands complete
	 * @throws Exception if browser automation fails
	 */
	@Override
	String call() throws Exception;

	/**
	 * Get a description of the current task being executed.
	 * Used for debugging and error reporting.
	 *
	 * @return Current task name, or null if not in a task
	 */
	String getCurrentTask();

	/**
	 * Get a description of the current command being executed.
	 * Used for debugging and error reporting.
	 *
	 * @return Current command string, or null if not executing a command
	 */
	String getCurrentCommand();

	/**
	 * Get the last exception message encountered during execution.
	 * Used for error reporting when optional commands fail.
	 *
	 * @return Last exception message, or null if no exception
	 */
	String getLastException();
}
