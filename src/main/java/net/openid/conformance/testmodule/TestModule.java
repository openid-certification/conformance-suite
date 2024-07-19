package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.runner.TestExecutionManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Map;

public interface TestModule {

	public static enum Status {
		NOT_YET_CREATED, // object just created, not yet setup
		CREATED, // test has been instantiated
		CONFIGURED, // configuration files have been sent and set up
		RUNNING, // test is executing
		WAITING, // test is waiting for external input
		INTERRUPTED, // test has been stopped before completion
		FINISHED, // test has completed
	}

	public static enum Result {
		PASSED, // test has passed successfully
		FAILED, // test has failed
		WARNING, // test has warnings
		REVIEW, // test requires manual review
		SKIPPED, // test can not be completed
		UNKNOWN // test results not yet known, probably still running (see status)
	}

	/**
	 * Method is called to pass configuration parameters
	 *
	 * @param config              A JSON object consisting of details that the testRunner
	 *                            doesn't need to know about
	 * @param baseUrl             The base of the URL that will need to be appended to any
	 *                            URL construction.
	 * @param externalUrlOverride The base of the URL if any external notifications are needed
	 * @param baseMtlsUrl
	 */
	void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl);

	/**
	 * *
	 *
	 * @return The name of the test.
	 */
	String getName();

	/**
	 * @return the id of this test
	 */
	String getId();

	/**
	 * @return The current status of the test
	 */
	Status getStatus();

	/**
	 * Called by the TestRunner to start the test
	 */
	void start();

	/**
	 * Called by the test runner to stop the test
	 *
	 * If called from a background execution unit, this may never return as background tasks may be aborted.
	 * Callers should hence try to perform any other required operations before calling this method.
	 *
	 * This will add an entry to the log, if the test is not already FINISHED/INTERRUPTED.
	 *
	 * @param reason Text describing why the test was stopped, to be used in any log entry.
	 */
	void stop(String reason);

	/**
	 * Called after the test has been stopped (for any reason) to allow
	 * cleanup of resources, such as dynamic client registrations.
	 */
	void cleanup();

	/**
	 * Called when a the test runner calls a URL
	 *
	 * @param path
	 *            The path that was called
	 * @param req
	 *            The request that passed to the server
	 * @param res
	 *            A response that will be sent from the server
	 * @param session
	 *            Session details
	 * @param requestParts
	 *            elements from the request parsed out into a json object for use in condition classes
	 * @return A response (could be a ModelAndview, ResponseEntity, or other item)
	 */
	Object handleHttp(String path,
		HttpServletRequest req, HttpServletResponse res,
		HttpSession session,
		JsonObject requestParts);

	/**
	 * @return get the test results
	 */
	Result getResult();

	/**
	 * @return a map of runtime values exposed by the test itself, potentially useful for configuration
	 *         of external entities.
	 */
	Map<String, String> getExposedValues();

	/**
	 * @return the associated browser control module
	 */
	BrowserControl getBrowser();

	/**
	 * @return the associated execution manager
	 */
	TestExecutionManager getTestExecutionManager();

	/**
	 * @param path
	 * @param req
	 * @param res
	 * @param session
	 * @param requestParts
	 * @return
	 */
	Object handleHttpMtls(String path,
		HttpServletRequest req, HttpServletResponse res,
		HttpSession session,
		JsonObject requestParts);

	/**
	 *
	 * @return get the {'iss':,'sub'} owner of the test
	 */
	Map<String, String> getOwner();

	/**
	 * @return get the Instant marking when the test was created
	 */
	Instant getCreated();

	/**
	 * @return true if test should be automatically started; false if the user should manually press the 'start' button
	 */
	boolean autoStart();

	/**
	 * @return get the Instant marking when the test's status was last updated
	 */
	Instant getStatusUpdated();

	/**
	 * @param error the final error from this test while running
	 */
	void setFinalError(TestInterruptedException error);

	/**
	 * @return the final error from this test while running, possibly null
	 */
	TestInterruptedException getFinalError();

	/**
	 * Mark the test as skipped (untestable). This method will throw a
	 * TestSkippedException to skip any further conditions from running.
	 * @param msg Reason for skipping the test
	 */
	void fireTestSkipped(String msg) throws TestSkippedException;

	/**
	 * Mark the test as finished, setting it to PASSED or REVIEW if appropriate.
	 *
	 * This should always be called with the test status set to 'RUNNING'.
	 */
	void fireTestFinished();

	/**
	 * Mark the test as having completed its setup.
	 */
	void fireSetupDone();

	/**
	 * Mark the test as requiring a manual review.
	 */
	void fireTestReviewNeeded();

	/**
	 * Pass along the appropriate runtime services and properties to allow the test to run. It cannot be used until this is completed.
	 */
	void setProperties(String id, Map<String, String> owner, TestInstanceEventLog wrappedEventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager, ImageService imageService);

	/**
	 * Pass along the current variant configuration
	 */
	void setVariant(Map<Class<? extends Enum<?>>, ? extends Enum<?>> variant);

	/** Handle a fatal exception */
	void handleException(TestInterruptedException error, String source);

	/**
	 * Force the lock to be released, if held.
	 *
	 * This should only be used in failure paths to cleanup. Must be called from the thread that might hold the lock.
	 */
	void forceReleaseLock();

	/**
	 * Check if the lock is held. Throws a TestFailureException if it is.
	 *
	 * This can be used to verify that a test module has correctly released it's lock when it has completed running
	 * on a thread, and hence make sure we avoid any situation where a test deadlocks because the next operation
	 * runs on a different thread and cannot obtain the lock.
	 */
	void checkLockReleased();
}
