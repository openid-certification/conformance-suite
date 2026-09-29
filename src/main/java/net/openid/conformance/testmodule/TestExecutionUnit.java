package net.openid.conformance.testmodule;

/**
 * A step a test module or condition sequence can execute: a condition call, a command, a sequence, ...
 */
@FunctionalInterface
public interface TestExecutionUnit {

	/**
	 * Execute this unit within the given test module context.
	 */
	void run(ExecutionContext context);
}
