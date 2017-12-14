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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;

/**
 * @author jricher
 *
 */
public abstract class AbstractTestModule implements TestModule {
	
	private static Logger logger = LoggerFactory.getLogger(AbstractTestModule.class);

	private String name;
	private String id = null; // unique identifier for the test, set from the outside
	private Status status = Status.UNKNOWN; // current status of the test
	private Result result = Result.UNKNOWN; // results of running the test

	private Map<String,String> owner; // Owner of the test (i.e. who created it. Should be subject and issuer from OIDC
	protected TestInstanceEventLog eventLog;
	protected BrowserControl browser;
	protected Map<String, String> exposed = new HashMap<>(); // exposes runtime values to outside modules
	protected Environment env = new Environment(); // keeps track of values at runtime

	protected TestInfoService testInfo;

	/**
	 * @param name
	 */
	public AbstractTestModule(String name, String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		this.name = name;
		this.id = id;
		this.owner = owner;
		this.eventLog = eventLog;
		this.browser = browser;
		this.testInfo = testInfo;

		setStatus(Status.CREATED);
	}

	public Map<String, String> getOwner() {
		return owner;
	}

	protected void setOwner(ImmutableMap<String, String> owner) {
		this.owner = owner;
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 */
	protected void require(Class<? extends Condition> conditionClass) {
		require(conditionClass, ConditionResult.FAILURE);
	}
	
	protected void require(Class<? extends Condition> conditionClass, String... requirements) {
		require(conditionClass, ConditionResult.FAILURE, requirements);
	}
	
	protected void require(Class<? extends Condition> conditionClass, ConditionResult onFail, String... requirements) {
		try {
			
			// create a new condition object from the class above
			Condition condition = conditionClass
				.getDeclaredConstructor(String.class, TestInstanceEventLog.class, ConditionResult.class, String[].class)
				.newInstance(id, eventLog, onFail, requirements);
			Method eval = conditionClass.getMethod("evaluate", Environment.class);
	
			// evaluate the condition and assign its results back to our environment
			logger.info(">> Calling Condition " + conditionClass.getSimpleName());
			

			PreEnvironment pre = eval.getAnnotation(PreEnvironment.class);
			if (pre != null) {
				for (String req : pre.required()) {
					if (!env.containsObj(req)) {
						logger.info("[pre] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find key in environment: " + req);
						fireTestFailure();
						throw new TestFailureException(new ConditionError(getId(), "[pre] Couldn't find key in environment: " + req));
					}
				}
				for (String s : pre.strings()) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[pre] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find string in environment: " + s);
						fireTestFailure();
						throw new TestFailureException(new ConditionError(getId(), "[pre] Couldn't find string in environment: " + s));
					}
				}
			}
			
			env = condition.evaluate(env);
			
			PreEnvironment post = eval.getAnnotation(PreEnvironment.class);
			if (post != null) {
				for (String req : post.required()) {
					if (!env.containsObj(req)) {
						logger.info("[post] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find key in environment: " + req);
						fireTestFailure();
						throw new TestFailureException(new ConditionError(getId(), "[post] Couldn't find key in environment: " + req));
					}
				}
				for (String s : post.strings()) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[post] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find string in environment: " + s);
						fireTestFailure();
						throw new TestFailureException(new ConditionError(getId(), "[post] Couldn't find string in environment: " + s));
					}
				}
			}
		} catch (ConditionError error) {
			logger.info("Test condition " + conditionClass.getSimpleName() + " failure: " + error.getMessage());
			fireTestFailure();
			logException(error);
			throw new TestFailureException(error);
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.error("Couldn't create required condition object", e);
			logException(e);
			fireTestFailure();
			throw new TestFailureException(getId(), "Couldn't create required condition: " + conditionClass.getSimpleName());
		} catch (Exception e) {
			logger.error("Generic error from underlying test framework", e);
			logException(e);
			fireTestFailure();
			throw new TestFailureException(getId(), e.getMessage());
		}
	}

	private void logException(Exception e) {
		Map<String, Object> event = new HashMap<>();
		event.put("msg", "Error from test framework");
		event.put("error", e.getMessage());
		event.put("error_class", e.getClass().getName());
		
		List<String> stack = Arrays.stream(e.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(Collectors.toList());

		event.put("stacktrace", stack);
		
		eventLog.log(getName(), event);
	}

	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 */
	
	protected void optional(Class<? extends Condition> conditionClass) {
		optional(conditionClass, ConditionResult.INFO);
	}
	
	protected void optional(Class<? extends Condition> conditionClass, String... requirements) {
		optional(conditionClass, ConditionResult.WARNING, requirements);
	}
	
	protected void optional(Class<? extends Condition> conditionClass, ConditionResult onFail, String... requirements) {
		try {
			
			// create a new condition object from the class above
			Condition condition = conditionClass
				.getDeclaredConstructor(String.class, TestInstanceEventLog.class, ConditionResult.class, String[].class)
				.newInstance(id, eventLog, onFail, requirements);
			Method eval = conditionClass.getMethod("evaluate", Environment.class);
	
			// evaluate the condition and assign its results back to our environment
			logger.info("}} Calling Condition " + conditionClass.getSimpleName());
			
			PreEnvironment pre = eval.getAnnotation(PreEnvironment.class);
			if (pre != null) {
				for (String req : pre.required()) {
					if (!env.containsObj(req)) {
						logger.info("[pre] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find key in environment: " + req);
						return;
					}
				}
				for (String s : pre.strings()) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[pre] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find string in environment: " + s);
						return;
					}
				}
			}
			
			env = condition.evaluate(env);
			
			PreEnvironment post = eval.getAnnotation(PreEnvironment.class);
			if (post != null) {
				for (String req : post.required()) {
					if (!env.containsObj(req)) {
						logger.info("[post] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find key in environment: " + req);
						return;
					}
				}
				for (String s : post.strings()) {
					if (Strings.isNullOrEmpty(env.getString(s))) {
						logger.info("[post] Test condition " + conditionClass.getSimpleName() + " failure, couldn't find string in environment: " + s);
						return;
					}
				}
			}
			
			
		} catch (ConditionError error) {
			logger.info("Ignoring optional test condition " + conditionClass.getSimpleName() + " failure: " + error.getMessage());
			logException(error);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.error("Couldn't create optional condition object", e);
			logException(e);
		} catch (Exception e) {
			logger.error("Generic error from underlying test framework", e);
			logException(e);
		}
	}

	public String getId() {
		return id;
	}

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

	protected void fireTestSuccess() {
		eventLog.log(getName(), ImmutableMap.of("result", "SUCCESS"));
		
		setResult(Result.PASSED);
	
		logFinalEnv();
	}

	protected void fireTestFailure() {
		eventLog.log(getName(), ImmutableMap.of("result", "FAILURE"));
	
		setResult(Result.FAILED);

		logFinalEnv();
	}

	protected void fireInterrupted() {
		eventLog.log(getName(), ImmutableMap.of("result", "INTERRUPTED"));
	
		setResult(Result.UNKNOWN);
		
		logFinalEnv();
	}

	/**
	 * @return the result
	 */
	public Result getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	protected void setResult(Result result) {
		this.result = result;
		if (testInfo != null) {
			testInfo.updateTestResult(getId(), getResult());
		}
	}
	
	/*
	 * Test status state machine:
	 * 
	 *        /----------------->----------------\
	 *       /                 /                  v
	 *   CREATED -> CONFIGURED -> RUNNING --> FINISHED
	 *                         \     ^--v      ^
	 *                          \-> WAITING --/
	 * 
	 * Any state can go to "UNKNOWN"
	 */
	protected void setStatus(Status status) {
		
		switch (getStatus()) {
			case CREATED:
				switch (status) {
					case CONFIGURED:
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
					case FINISHED:
					case UNKNOWN:
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
	}

	/**
	 * Add a key/value pair to the exposed values
	 * @param key
	 * @param val
	 */
	protected void expose(String key, String val) {
		exposed.put(key, val);
	}

	/**
	 * Expose a value from the environment
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
		return name;
	}

}
