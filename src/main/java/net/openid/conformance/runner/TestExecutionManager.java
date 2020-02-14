package net.openid.conformance.runner;

import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestInterruptedException;
import net.openid.conformance.testmodule.TestModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestExecutionManager {

	private class BackgroundTask implements Callable<Object> {
		private String testId;
		private Callable<?> myCallable;
		private Authentication savedAuthentication;
		private TestRunnerSupport testRunnerSupport;

		public BackgroundTask(String testId, Callable<?> callable, TestRunnerSupport testRunnerSupport) {
			this.testId = testId;
			this.myCallable = callable;
			this.testRunnerSupport = testRunnerSupport;
			// save the authentication context for use when we run it later
			savedAuthentication = authenticationFacade.getContextAuthentication();
		}

		@Override
		public Object call() throws TestInterruptedException {
			// restore the authentication context that was in place when this was created
			authenticationFacade.setLocalAuthentication(savedAuthentication);
			Object returnObj = null;
			try {
				TestModule test = testRunnerSupport.getRunningTestById(testId);
				returnObj = myCallable.call();
				//
				if (test != null) {
					// ensure the callable did not leave the test module's lock held, potentially deadlocking other
					// threads
					test.checkLockReleased();
				} else {
					logger.error("Test '"+testId+"' does not seem to be running in BackgroundTask");
				}
			} catch (TestInterruptedException e) {
				if (e.getTestId() == null || !e.getTestId().equals(testId)) {
					throw new TestFailureException(testId, "A TestInterruptedException has been caught that does not contain the test id for the current test, this is a bug in the test module", e);
				}
				throw e;
			} catch (ConditionError e) {
				// we deliberately don't pass 'e' as the cause here, as doing so would make other parts of the
				// suite believe log messages had already been added for this failure.
				// see https://gitlab.com/openid/conformance-suite/issues/443
				throw new TestFailureException(testId, "A ConditionError has been incorrectly thrown by a TestModule, this is a bug in the test module: " + e.getMessage());
			} catch (Exception e) {
				// we /must/ throw a TestFailureException here, so that when TestRunner calls future.get() and
				// an exception is caught, it can map the exception back to the test
				throw new TestFailureException(testId, e);
			} finally {
				// release the lock, so other threads can still run
				TestModule test = testRunnerSupport.getRunningTestById(testId);
				test.forceReleaseLock();
			}
			return returnObj;
		}
	}

	private String testId;

	private List<Future<?>> futures = new ArrayList<>();

	private Future<?> finalisationFuture;

	private AtomicBoolean finalisationStarted = new AtomicBoolean(false);

	private ExecutorCompletionService<Object> executorCompletionService;

	private AuthenticationFacade authenticationFacade;

	private static final Logger logger = LoggerFactory.getLogger(TestExecutionManager.class);

	private TestRunnerSupport testRunnerSupport;

	public TestExecutionManager(String testId, ExecutorCompletionService<Object> executorCompletionService, AuthenticationFacade authenticationFacade, TestRunnerSupport testRunnerSupport) {
		this.testId = testId;
		this.executorCompletionService = executorCompletionService;
		this.authenticationFacade = authenticationFacade;
		this.testRunnerSupport = testRunnerSupport;
	}

	/**
	 * @return the testId
	 */
	public String getTestId() {
		return testId;
	}

	/**
	 * Clean up queued tasks for this test id
	 */
	public void cancelAllBackgroundTasks() {
		for (Future<?> f : futures) {
			if (!f.isDone()) {
				f.cancel(true); // True allows the task to be interrupted.
			}
		}
	}

	public void cancelAllBackgroundTasksExceptFinalisation() {
		for (Future<?> f : futures) {
			if (f.equals(finalisationFuture)) {
				continue;
			}
			if (!f.isDone()) {
				f.cancel(true); // True allows the task to be interrupted.
			}
		}
	}

	public void runInBackground(Callable<?> callable) {
		futures.add(executorCompletionService.submit(new BackgroundTask(testId, callable, testRunnerSupport)));
	}

	/**
	 * Run a finalisation task
	 *
	 * This is just like a normal task, except there can only ever be one of them.
	 */
	public void runFinalisationTaskInBackground(Callable<?> callable) {
		// use an AtomicBoolean.getAndSet to avoid any race conditions as it is possible for the main test thread
		// and the placeholder watcher to try and run the finalisation block at the same time
		boolean oldValue = finalisationStarted.getAndSet(true);
		if (!oldValue) {
			Future<?> f = executorCompletionService.submit(new BackgroundTask(testId, callable, testRunnerSupport));
			futures.add(f);
			finalisationFuture = f;
		}
	}

}
