/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.testmodule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.RedirectView;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;

/**
 * @author jricher
 *
 */
public abstract class AbstractTestModule implements TestModule {

	private static Logger logger = LoggerFactory.getLogger(AbstractTestModule.class);

	// Set up Thread executor
	//private ExecutorService executorService = Executors.newCachedThreadPool();

	private String id = null; // unique identifier for the test, set from the outside
	private Status status = Status.CREATED; // current status of the test
	private Result result = Result.UNKNOWN; // results of running the test

	private Map<String, String> owner; // Owner of the test (i.e. who created it. Should be subject and issuer from OIDC
	protected TestInstanceEventLog eventLog;
	protected BrowserControl browser;
	protected TestExecutionManager executionManager;
	protected Map<String, String> exposed = new HashMap<>(); // exposes runtime values to outside modules
	protected Environment env = new Environment(); // keeps track of values at runtime
	private Instant created; // time stamp of when this test created
	private Instant statusUpdated; // time stamp of when the status was last updated
	private TestFailureException finalError; // final error from running the test

	protected TestInfoService testInfo;

	private Supplier<String> testNameSupplier = Suppliers.memoize(() -> getClass().getDeclaredAnnotation(PublishTestModule.class).testName());

	/**
	 * @param name
	 */
	public AbstractTestModule(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		this.id = id;
		this.owner = owner;
		this.eventLog = eventLog;
		this.browser = browser;
		this.testInfo = testInfo;
		this.executionManager = executionManager;

		this.created = Instant.now();
		this.statusUpdated = created; // this will get changed in a moment but set it here for completeness

		setStatus(Status.CREATED);
	}

	@Override
	public Map<String, String> getOwner() {
		return owner;
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 */
	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass) {
		call(condition(conditionClass)
			.onFail(ConditionResult.FAILURE));
	}

	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.onFail(ConditionResult.FAILURE)
			.requirements(requirements));
	}

	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, ConditionResult onFail, String... requirements) {
		call(condition(conditionClass)
			.requirements(requirements)
			.onFail(onFail));
	}

	private void logException(Exception e) {
		Map<String, Object> event = ex(e);
		event.put("msg", "Caught exception from test framework");

		eventLog.log(getName(), event);
	}

	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 */

	protected void call(Class<? extends Condition> conditionClass) {
		call(condition(conditionClass)
			.onFail(ConditionResult.INFO)
			.dontStopOnFailure());
	}

	protected void call(Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.onFail(ConditionResult.WARNING)
			.requirements(requirements)
			.dontStopOnFailure());
	}

	protected void call(Class<? extends Condition> conditionClass, ConditionResult onFail, String... requirements) {
		call(condition(conditionClass)
			.requirements(requirements)
			.onFail(onFail)
			.dontStopOnFailure());
	}

	protected void skipIfMissing(String[] required, String[] strings, ConditionResult onSkip,
		Class<? extends Condition> conditionClass) {

		call(condition(conditionClass)
			.skipIfObjectsMissing(required)
			.skipIfStringsMissing(strings)
			.onSkip(onSkip)
			.onFail(ConditionResult.INFO)
			.dontStopOnFailure());
	}

	protected void skipIfMissing(String[] required, String[] strings, ConditionResult onSkip,
		Class<? extends Condition> conditionClass, String... requirements) {
		call(condition(conditionClass)
			.skipIfObjectsMissing(required)
			.skipIfStringsMissing(strings)
			.onSkip(onSkip)
			.requirements(requirements)
			.onFail(ConditionResult.WARNING)
			.dontStopOnFailure());
	}

	protected void skipIfMissing(String[] required, String[] strings, ConditionResult onSkip,
		Class<? extends Condition> conditionClass, ConditionResult onFail, String... requirements) {
		call(condition(conditionClass)
			.skipIfObjectsMissing(required)
			.skipIfStringsMissing(strings)
			.onSkip(onSkip)
			.requirements(requirements)
			.onFail(onFail)
			.dontStopOnFailure());
	}

	/**
	 * Call the condition as specified in the builder.
	 *
	 * @param builder
	 */
	protected void call(ConditionCallBuilder builder) {

		try {

			// create a new condition object from the class above
			Condition condition = builder.getConditionClass()
				.getDeclaredConstructor(String.class, TestInstanceEventLog.class, ConditionResult.class, String[].class)
				.newInstance(id, eventLog, builder.getOnFail(), builder.getRequirements());
			Method eval = builder.getConditionClass().getMethod("evaluate", Environment.class);

			logger.info((builder.isStopOnFailure() ? ">>" : "}}") + " Calling Condition " + builder.getConditionClass().getSimpleName());

			// check the environment to see if we need to skip this call
			for (String req : builder.getSkipIfObjectsMissing()) {
				if (!env.containsObject(req)) {
					logger.info("[skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find key in environment: " + req);
					eventLog.log(condition.getMessage(), args(
						"msg", "Skipped evaluation due to missing required object: " + req,
						"expected", req,
						"result", builder.getOnSkip(),
						"mapped", env.isKeyShadowed(req) ? env.getEffectiveKey(req) : null
					// TODO: log the environment here?
					));
					updateResultFromConditionFailure(builder.getOnSkip());
					return;
				}
			}
			for (String s : builder.getSkipIfStringsMissing()) {
				if (Strings.isNullOrEmpty(env.getString(s))) {
					logger.info("[skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find string in environment: " + s);
					eventLog.log(condition.getMessage(), args(
						"msg", "Skipped evaluation due to missing required string: " + s,
						"expected", s,
						"result", builder.getOnSkip()
					// TODO: log the environment here?
					));
					updateResultFromConditionFailure(builder.getOnSkip());
					return;
				}
			}
			for (Pair<String, String> idx : builder.getSkipIfElementsMissing()) {
				JsonElement el = env.getElementFromObject(idx.getLeft(), idx.getRight());
				if (el == null) {
					logger.info("[skip] Test condition " + builder.getConditionClass().getSimpleName() + " skipped, couldn't find element in environment: " + idx.getLeft() + " " + idx.getRight());
					eventLog.log(condition.getMessage(), args(
						"msg", "Skipped evaluation due to missing required element: " + idx.getLeft() + " " + idx.getRight(),
						"object", idx.getLeft(),
						"path", idx.getRight(),
						"mapped", env.isKeyShadowed(idx.getLeft()) ? env.getEffectiveKey(idx.getLeft()) : null,
						"result", builder.getOnSkip()
					// TODO: log the environment here?
					));
					updateResultFromConditionFailure(builder.getOnSkip());
					return;
				}
			}


			PreEnvironment pre = eval.getAnnotation(PreEnvironment.class);
			if (pre != null) {
				for (String req : pre.required()) {
					if (!env.containsObject(req)) {
						logger.info("[pre] Test condition " + builder.getConditionClass().getSimpleName() + " failure, couldn't find key in environment: " + req);
						eventLog.log(condition.getMessage(), args(
							"msg", "Condition failure, couldn't find required object in environment before evaluation: " + req,
							"expected", req,
							"result", builder.getOnFail(),
							"mapped", env.isKeyShadowed(req) ? env.getEffectiveKey(req) : null
						// TODO: log the environment here?
						));
						if (builder.isStopOnFailure()) {
							fireTestFailure();
							throw new TestFailureException(new ConditionError(getId(), "[pre] Couldn't find key in environment: " + req));
						} else {
							updateResultFromConditionFailure(builder.getOnFail());
							return;
						}

					}
				}
				for (String s : pre.strings()) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[pre] Test condition " + builder.getConditionClass().getSimpleName() + " failure, couldn't find string in environment: " + s);
						eventLog.log(condition.getMessage(), args(
							"msg", "Condition failure, couldn't find required string in environment before evaluation: " + s,
							"expected", s,
							"result", builder.getOnFail()
						// TODO: log the environment here?
						));
						if (builder.isStopOnFailure()) {
							fireTestFailure();
							throw new TestFailureException(new ConditionError(getId(), "[pre] Couldn't find string in environment: " + s));
						} else {
							updateResultFromConditionFailure(builder.getOnFail());
							return;
						}
					}
				}
			}

			// evaluate the condition and assign its results back to our environment
			env = condition.evaluate(env);

			// check the environment to make sure the condition did what it claimed to
			PostEnvironment post = eval.getAnnotation(PostEnvironment.class);
			if (post != null) {
				for (String req : post.required()) {
					if (!env.containsObject(req)) {
						logger.info("[post] Test condition " + builder.getConditionClass().getSimpleName() + " failure, couldn't find key in environment: " + req);
						eventLog.log(condition.getMessage(), args(
							"msg", "Condition failure, couldn't find required object in environment after evaluation: " + req,
							"expected", req,
							"result", builder.getOnFail(),
							"mapped", env.isKeyShadowed(req) ? env.getEffectiveKey(req) : null
						// TODO: log the environment here?
						));
						if (builder.isStopOnFailure()) {
							fireTestFailure();
							throw new TestFailureException(new ConditionError(getId(), "[post] Couldn't find key in environment: " + req));
						} else {
							updateResultFromConditionFailure(builder.getOnFail());
							return;
						}
					}
				}
				for (String s : post.strings()) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[post] Test condition " + builder.getConditionClass().getSimpleName() + " failure, couldn't find string in environment: " + s);
						eventLog.log(condition.getMessage(), args(
							"msg", "Condition failure, couldn't find required string in environment after evaluation: " + s,
							"expected", s,
							"result", builder.getOnFail()
						// TODO: log the environment here?
						));
						if (builder.isStopOnFailure()) {
							fireTestFailure();
							throw new TestFailureException(new ConditionError(getId(), "[post] Couldn't find string in environment: " + s));
						} else {
							updateResultFromConditionFailure(builder.getOnFail());
							return;
						}
					}
				}
			}

		} catch (ConditionError error) {
			if (builder.isStopOnFailure()) {
				logger.info("Test condition " + builder.getConditionClass().getSimpleName() + " failure: " + error.getMessage());
				fireTestFailure();
				throw new TestFailureException(error);
			} else {
				logger.info("Ignoring optional test condition " + builder.getConditionClass().getSimpleName() + " failure: " + error.getMessage());
				updateResultFromConditionFailure(builder.getOnFail());
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logException(e);
			if (builder.isStopOnFailure()) {
				logger.error("Couldn't create required condition object", e);
				fireTestFailure();
				throw new TestFailureException(getId(), "Couldn't create required condition: " + builder.getConditionClass().getSimpleName());
			} else {
				logger.error("Couldn't create optional condition object", e);
				updateResultFromConditionFailure(builder.getOnFail());
			}
		} catch (Exception e) {
			logException(e);
			if (builder.isStopOnFailure()) {
				logger.error("Generic error from underlying test framework", e);
				fireTestFailure();
				throw new TestFailureException(getId(), e.getMessage());
			} else {
				logger.error("Generic error from underlying test framework", e);
				updateResultFromConditionFailure(builder.getOnFail());
			}
		}

	}

	/**
	 * Create a new condition call builder, which can be passed to call()
	 */
	protected ConditionCallBuilder condition(Class<? extends Condition> conditionClass) {
		return new ConditionCallBuilder(conditionClass);
	}

	/**
	 * Create a new test execution builder, which can be passed to call()
	 */
	protected TestExecutionBuilder exec() {
		return new TestExecutionBuilder();
	}

	/**
	 * Execute a set of test execution commands.
	 *
	 * Commands are executed in this order:
	 *
	 * environment strings are exposed
	 * log blocks are started
	 * environment keys are mapped
	 * environment keys are unmapped
	 * log blocks are ended
	 *
	 */
	protected void call(TestExecutionBuilder builder) {

		for(String e : builder.getExposeStrings()) {
			exposeEnvString(e);
		}

		if (!Strings.isNullOrEmpty(builder.getStartBlock())) {
			eventLog.startBlock(builder.getStartBlock());
		}

		for (Map.Entry<String, String> e : builder.getMapKeys().entrySet()) {
			env.mapKey(e.getKey(), e.getValue());
		}

		for (String e : builder.getUnmapKeys()) {
			env.unmapKey(e);
		}

		if (builder.isEndBlock()) {
			eventLog.endBlock();
		}
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	protected void logFinalEnv() {
		logger.info("Final environment: " + env);
	}

	@Override
	public void fireSetupDone() {
		eventLog.log(getName(), "Setup Done");
	}

	@Override
	public void fireTestFinished() {

		// first we set our test to WAITING to release the lock (note that this happens in the calling thread) and prepare for finalization
		setStatus(Status.WAITING);

		// then this happens in the background so that we can check the state of the browser controller

		getTestExecutionManager().runInBackground(() -> {


			// wait for web runners to wrap up first

			Instant timeout = Instant.now().plusSeconds(60); // wait at most 60 seconds
			while (browser.getWebRunners().size() > 0
				&& Instant.now().isBefore(timeout)) {
				Thread.sleep(100); // sleep before we check again
			}

			// if we weren't interrupted already, then we're finished
			if (!getStatus().equals(Status.INTERRUPTED)) {
				setStatus(Status.FINISHED);

				// log the environment here in case "stop" doesn't get it it
				logFinalEnv();
			}

			if (getResult() == Result.UNKNOWN) {
				fireTestSuccess();
			}

			stop();

			eventLog.log(getName(), args(
				"msg", "Finished",
				"result", getResult()));

			return "done";
		});
	}

	@Override
	public void fireTestPlaceholderFilled() {
		// if we weren't interrupted already, then we're finished
		if (!getStatus().equals(Status.INTERRUPTED)) {
			setStatus(Status.FINISHED);
		}

		if (getResult() == Result.UNKNOWN || getResult() == Result.REVIEW) {
			fireTestSuccess();
		}

		stop();
	}

	@Override
	public void fireTestReviewNeeded() {
		setResult(Result.REVIEW);
	}

	@Override
	public void fireTestSuccess() {
		setResult(Result.PASSED);
	}

	@Override
	public void fireTestFailure() {
		setResult(Result.FAILED);
	}

	/**
	 * @return the result
	 */
	@Override
	public Result getResult() {
		return result;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	protected void setResult(Result result) {
		this.result = result;
		testInfo.updateTestResult(getId(), getResult());
	}

	protected void updateResultFromConditionFailure(ConditionResult onFail) {
		switch (onFail) {
			case FAILURE:
				setResult(Result.FAILED);
				break;
			case WARNING:
				if (getResult() != Result.FAILED) {
					setResult(Result.WARNING);
				}
				break;
			default:
				// No action
				break;
		}
	}

	/*
	 * Test status state machine:
	 *
	 *          /----------->--------------------------------\
	 *         /           /                                  \
	 *        /----------------->----------------\             \
	 *       /           /     /                  v             v
	 *   CREATED -> CONFIGURED -> RUNNING --> FINISHED      INTERRUPTED
	 *                         \     ^--v      ^              ^
	 *                          \-> WAITING --/--------------/
	 *
	 * Any state can go to "UNKNOWN"
	 */
	protected void setStatus(Status status) {
		logger.error("setStatus("+getStatus().toString()+"): current status = "+status.toString());

		if (status == getStatus()) {
			return;
		}

		switch (getStatus()) {
			case CREATED:
				switch (status) {
					case CONFIGURED:
					case INTERRUPTED:
					case FINISHED:
						break;
					default:
						clearLockIfHeld();
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case CONFIGURED:
				switch (status) {
					case RUNNING:
						acquireLock();
						break;
					case INTERRUPTED:
					case FINISHED:
					case WAITING:
						break;
					default:
						clearLock();
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case RUNNING:  // We should have the lock when we're running
				switch (status) {
					case INTERRUPTED:
						clearLockIfHeld();
						break;
					case FINISHED:
					case WAITING:
						clearLock();
						break;
					default:
						clearLockIfHeld();
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case WAITING:  // we shouldn't have the lock if we're waiting.
				switch (status) {
					case RUNNING:
						acquireLock();  // we want to grab the lock whenever we start running
						break;
					case INTERRUPTED:
					case FINISHED:
						break;
					default:
						clearLockIfHeld();
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case INTERRUPTED:
				clearLockIfHeld();
				throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
			case FINISHED:
				clearLockIfHeld();
				throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
			case UNKNOWN:
			default:
				clearLockIfHeld();
				throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
		}

		this.status = status;
		if (testInfo != null) {
			testInfo.updateTestStatus(getId(), getStatus());
		}
		this.statusUpdated = Instant.now();
	}

	private void clearLock(){
		env.getLock().unlock();
	}

	/**
	 * Helper to check if we have the lock, and if we do, unlock it.
	 */
	private void clearLockIfHeld(){
		if(env.getLock().isHeldByCurrentThread()) {
			env.getLock().unlock();
		}
	}

	/**
	 * Add a key/value pair to the exposed values
	 *
	 * @param key
	 * @param val
	 */
	protected void expose(String key, String val) {
		exposed.put(key, val);
	}

	/**
	 * Expose a value from the environment
	 *
	 * @param key
	 */
	protected void exposeEnvString(String key) {
		String val = env.getString(key);
		expose(key, val);
	}

	@Override
	public Map<String, String> getExposedValues() {
		return exposed;
	}

	@Override
	public BrowserControl getBrowser() {
		return this.browser;
	}

	/**
	 * @return the name
	 */
	@Override
	public String getName() {
		return testNameSupplier.get();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#stop()
	 */
	@Override
	public void stop() {

		if (getStatus().equals(Status.FINISHED) || getStatus().equals(Status.INTERRUPTED)) {
			// can't stop what's already stopped
			return;
		}

		if (!getStatus().equals(Status.FINISHED)) {
			setStatus(Status.INTERRUPTED);
			eventLog.log(getName(), args(
				"msg", "Test was interrupted before it could complete",
				"result", Status.INTERRUPTED.toString()));
		} else {
			eventLog.log(getName(), args(
				"msg", "Test was stopped",
				"result", getResult().toString()));
		}

		logFinalEnv();
	}

	/**
	 * @return the created
	 */
	@Override
	public Instant getCreated() {
		return created;
	}

	@Override
	public Instant getStatusUpdated() {
		return statusUpdated;
	}

	/**
	 * @return the finalError
	 */
	@Override
	public TestFailureException getFinalError() {
		return finalError;
	}

	/**
	 * @param finalError the finalError to set
	 */
	@Override
	public void setFinalError(TestFailureException finalError) {
		this.finalError = finalError;
	}

	protected RedirectView redirectToLogDetailPage() {
		return new RedirectView("/log-detail.html?log=" + getId());
	}

	/*
	 * Convenience pass-through methods
	 */
	protected Map<String, Object> args(Object... a) {
		return EventLog.args(a);
	}

	protected Map<String, Object> ex(Throwable cause) {
		return EventLog.ex(cause);
	}

	protected Map<String, Object> ex(Throwable cause, Map<String, Object> in) {
		return EventLog.ex(cause, in);
	}

	protected JsonObject ex(Throwable cause, JsonObject in) {
		return EventLog.ex(cause, in);
	}

	public void acquireLock() {
		env.getLock().lock();
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got an HTTP response we weren't expecting");
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got an HTTP response we weren't expecting");
	}

	@Override
	public TestExecutionManager getTestExecutionManager() {
		return executionManager;
	}

}
