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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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

/**
 * @author jricher
 *
 */
public abstract class AbstractTestModule implements TestModule {

	private static Logger logger = LoggerFactory.getLogger(AbstractTestModule.class);

	private String id = null; // unique identifier for the test, set from the outside
	private Status status = Status.UNKNOWN; // current status of the test
	private Result result = Result.UNKNOWN; // results of running the test

	private Map<String, String> owner; // Owner of the test (i.e. who created it. Should be subject and issuer from OIDC
	protected TestInstanceEventLog eventLog;
	protected BrowserControl browser;
	protected Map<String, String> exposed = new HashMap<>(); // exposes runtime values to outside modules
	protected Environment env = new Environment(); // keeps track of values at runtime
	private Instant created; // time stamp of when this test created
	private Instant statusUpdated; // time stamp of when the status was last updated

	protected TestInfoService testInfo;

	private Supplier<String> testNameSupplier = Suppliers.memoize(() -> getClass().getDeclaredAnnotation(PublishTestModule.class).testName());

	/**
	 * @param name
	 */
	public AbstractTestModule(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		this.id = id;
		this.owner = owner;
		this.eventLog = eventLog;
		this.browser = browser;
		this.testInfo = testInfo;

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
		callAndStopOnFailure(conditionClass, ConditionResult.FAILURE);
	}

	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
		callAndStopOnFailure(conditionClass, ConditionResult.FAILURE, requirements);
	}

	protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, ConditionResult onFail, String... requirements) {
		callConditionInternal(conditionClass, requirements, onFail, null, true, null, null);
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
		call(conditionClass, ConditionResult.INFO);
	}

	protected void call(Class<? extends Condition> conditionClass, String... requirements) {
		call(conditionClass, ConditionResult.WARNING, requirements);
	}

	protected void call(Class<? extends Condition> conditionClass, ConditionResult onFail, String... requirements) {
		callConditionInternal(conditionClass, requirements, onFail, null, false, null, null);
	}

	protected void skipIfMissing(String[] required, String[] strings, ConditionResult onSkip,
		Class<? extends Condition> conditionClass) {
		skipIfMissing(required, strings, onSkip, conditionClass, ConditionResult.INFO);
	}

	protected void skipIfMissing(String[] required, String[] strings, ConditionResult onSkip,
		Class<? extends Condition> conditionClass, String... requirements) {
		skipIfMissing(required, strings, onSkip, conditionClass, ConditionResult.WARNING, requirements);
	}

	protected void skipIfMissing(String[] required, String[] strings, ConditionResult onSkip,
		Class<? extends Condition> conditionClass, ConditionResult onFail, String... requirements) {
		callConditionInternal(conditionClass, requirements, onFail, onSkip, false, required, strings);
	}

	/**
	 * Internal uber-method for calling a condition
	 * 
	 * @param conditionClass
	 *            The condition to call and evaluate
	 * @param onFail
	 *            What result to log if the condition fails
	 * @param onSkip
	 *            What result to log if the condition is skipped
	 * @param stopOnFailure
	 *            Whether to stop the test if the condition fails or keep going
	 * @param skipIfRequired
	 *            List of objects to check the environment for and skip the condition evaluation if they're not found
	 * @param skipIfStringsRequired
	 *            List of strings to check the environment for and skip the condition evaluation if they're not found
	 * @param requirements
	 *            The list of requirements that are tied to this condition within this test module
	 */
	private void callConditionInternal(Class<? extends Condition> conditionClass,
		String[] requirements,
		ConditionResult onFail,
		ConditionResult onSkip,
		boolean stopOnFailure,
		String[] skipIfRequired, String[] skipIfStringsRequired) {

		try {

			// create a new condition object from the class above
			Condition condition = conditionClass
				.getDeclaredConstructor(String.class, TestInstanceEventLog.class, ConditionResult.class, String[].class)
				.newInstance(id, eventLog, onFail, requirements);
			Method eval = conditionClass.getMethod("evaluate", Environment.class);

			logger.info((stopOnFailure ? ">>" : "}}") + " Calling Condition " + conditionClass.getSimpleName());

			// check the environment to see if we need to skip anything
			if (skipIfRequired != null) {
				for (String req : skipIfRequired) {
					if (!env.containsObj(req)) {
						logger.info("[skip] Test condition " + conditionClass.getSimpleName() + " skipped, couldn't find key in environment: " + req);
						eventLog.log(condition.getMessage(), args(
							"msg", "Skipped evaluation due to missing required object: " + req,
							"expected", req,
							"result", onSkip
						// TODO: log the environment here?
						));
						return;
					}
				}
			}
			if (skipIfStringsRequired != null) {
				for (String s : skipIfStringsRequired) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[skip] Test condition " + conditionClass.getSimpleName() + " skipped, couldn't find string in environment: " + s);
						eventLog.log(condition.getMessage(), args(
							"msg", "Skipped evaluation due to missing required string: " + s,
							"expected", s,
							"result", onSkip
						// TODO: log the environment here?
						));
						return;
					}
				}
			}

			PreEnvironment pre = eval.getAnnotation(PreEnvironment.class);
			if (pre != null) {
				for (String req : pre.required()) {
					if (!env.containsObj(req)) {
						logger.info("[pre] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find key in environment: " + req);
						eventLog.log(condition.getMessage(), args(
							"msg", "Condition failure, couldn't find required object in environment before evaluation: " + req,
							"expected", req,
							"result", onFail
						// TODO: log the environment here?
						));
						if (stopOnFailure) {
							fireTestFailure();
							throw new TestFailureException(new ConditionError(getId(), "[pre] Couldn't find key in environment: " + req));
						} else {
							updateResultFromConditionFailure(onFail);
							return;
						}

					}
				}
				for (String s : pre.strings()) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[pre] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find string in environment: " + s);
						eventLog.log(condition.getMessage(), args(
							"msg", "Condition failure, couldn't find required string in environment before evaluation: " + s,
							"expected", s,
							"result", onFail
						// TODO: log the environment here?
						));
						if (stopOnFailure) {
							fireTestFailure();
							throw new TestFailureException(new ConditionError(getId(), "[pre] Couldn't find string in environment: " + s));
						} else {
							updateResultFromConditionFailure(onFail);
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
					if (!env.containsObj(req)) {
						logger.info("[post] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find key in environment: " + req);
						eventLog.log(condition.getMessage(), args(
							"msg", "Condition failure, couldn't find required object in environment after evaluation: " + req,
							"expected", req,
							"result", onFail
						// TODO: log the environment here?
						));
						if (stopOnFailure) {
							fireTestFailure();
							throw new TestFailureException(new ConditionError(getId(), "[post] Couldn't find key in environment: " + req));
						} else {
							updateResultFromConditionFailure(onFail);
							return;
						}
					}
				}
				for (String s : post.strings()) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[post] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find string in environment: " + s);
						eventLog.log(condition.getMessage(), args(
							"msg", "Condition failure, couldn't find required string in environment after evaluation: " + s,
							"expected", s,
							"result", onFail
						// TODO: log the environment here?
						));
						if (stopOnFailure) {
							fireTestFailure();
							throw new TestFailureException(new ConditionError(getId(), "[post] Couldn't find string in environment: " + s));
						} else {
							updateResultFromConditionFailure(onFail);
							return;
						}
					}
				}
			}

		} catch (ConditionError error) {
			if (stopOnFailure) {
				logger.info("Test condition " + conditionClass.getSimpleName() + " failure: " + error.getMessage());
				fireTestFailure();
				throw new TestFailureException(error);
			} else {
				logger.info("Ignoring optional test condition " + conditionClass.getSimpleName() + " failure: " + error.getMessage());
				updateResultFromConditionFailure(onFail);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logException(e);
			if (stopOnFailure) {
				logger.error("Couldn't create required condition object", e);
				fireTestFailure();
				throw new TestFailureException(getId(), "Couldn't create required condition: " + conditionClass.getSimpleName());
			} else {
				logger.error("Couldn't create optional condition object", e);
				updateResultFromConditionFailure(onFail);
			}
		} catch (Exception e) {
			logException(e);
			if (stopOnFailure) {
				logger.error("Generic error from underlying test framework", e);
				fireTestFailure();
				throw new TestFailureException(getId(), e.getMessage());
			} else {
				logger.error("Generic error from underlying test framework", e);
				updateResultFromConditionFailure(onFail);
			}
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
		//		Map<String, Object> finalEnv = new HashMap<>();
		//		for (String key : env.allObjectIds()) {
		//			finalEnv.put(key, env.get(key));
		//		}
		//		
		//		eventLog.log(getId(), "final_env", finalEnv);
		//		
		logger.info("Final environment: " + env);
	}

	protected void fireSetupDone() {
		eventLog.log(getName(), "Setup Done");
	}

	protected void fireTestFinished() {
		setStatus(Status.FINISHED);

		if (getResult() == Result.UNKNOWN) {
			fireTestSuccess();
		}

		eventLog.log(getName(), args(
			"msg", "Finished",
			"result", getResult()));
	}

	protected void fireTestSuccess() {
		setResult(Result.PASSED);
	}

	protected void fireTestFailure() {
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
		if (testInfo != null) {
			testInfo.updateTestResult(getId(), getResult());
		}
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

		switch (getStatus()) {
			case CREATED:
				switch (status) {
					case CONFIGURED:
					case INTERRUPTED:
					case FINISHED:
					case UNKNOWN:
						break;
					default:
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case CONFIGURED:
				switch (status) {
					case RUNNING:
					case INTERRUPTED:
					case FINISHED:
					case WAITING:
					case UNKNOWN:
						break;
					default:
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case RUNNING:
				switch (status) {
					case INTERRUPTED:
					case FINISHED:
					case WAITING:
					case UNKNOWN:
						break;
					default:
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case WAITING:
				switch (status) {
					case RUNNING:
					case INTERRUPTED:
					case FINISHED:
					case UNKNOWN:
						break;
					default:
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case INTERRUPTED:
				switch (status) {
					case INTERRUPTED:
						break;
					default:
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
				break;
			case FINISHED:
				switch (status) {
					case FINISHED:
						break;
					default:
						throw new TestFailureException(getId(), "Illegal test state change: " + getStatus() + " -> " + status);
				}
			case UNKNOWN:
			default:
				break;
		}

		this.status = status;
		if (testInfo != null) {
			testInfo.updateTestStatus(getId(), getStatus());
		}
		this.statusUpdated = Instant.now();
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

		String logResult;

		if (!getStatus().equals(Status.FINISHED)) {
			setStatus(Status.INTERRUPTED);
			logResult = "INTERRUPTED";
			eventLog.log(getName(), args(
				"msg", "Test was interrupted before it could complete",
				"result", logResult));
		} else {
			logResult = getResult().toString();
			eventLog.log(getName(), args(
				"msg", "Test was stopped",
				"result", logResult));
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

	protected void logIncomingHttpRequest(String path, JsonObject requestParts) {
		eventLog.log(getName(), args(
			"msg", "Incoming HTTP request to test instance " + getId(),
			"http", "incoming",
			"path", path,
			"request", requestParts));
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

}
