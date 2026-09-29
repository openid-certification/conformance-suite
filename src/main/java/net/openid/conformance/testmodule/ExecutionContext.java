package net.openid.conformance.testmodule;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;

/**
 * The services a running test module offers to the {@link TestExecutionUnit}s it executes.
 */
public interface ExecutionContext {

	String getTestId();

	Environment getEnv();

	TestInstanceEventLog getEventLog();

	/**
	 * Expose a string from the environment to the user (i.e. display it in the front end)
	 */
	void exposeEnvString(String key);

	/**
	 * Evaluate a condition: check its skip prerequisites, execute it, and record the result.
	 *
	 * @param name the name used for logging, normally the condition class simple name
	 * @param stopOnFailure whether a failure of the condition stops the test
	 * @param onFail the result recorded when the condition fails and the test continues
	 * @param preRequisiteValidator decides whether the call is skipped
	 * @param call executes the condition
	 */
	void runCondition(String name, boolean stopOnFailure, ConditionResult onFail,
		ConditionalPreRequisiteValidator preRequisiteValidator, ConditionalCall call);

	/**
	 * Execute a nested unit through the owning test module.
	 */
	void run(TestExecutionUnit unit);

	/**
	 * Log an unexpected framework error to the test log and return the exception that stops the test.
	 */
	TestFailureException fatalError(String message, Throwable cause);
}
