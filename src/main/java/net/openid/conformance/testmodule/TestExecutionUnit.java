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

	/**
	 * The identity used by {@code ConditionSequence.replace/skip/insertBefore/insertAfter} to address this unit:
	 * the condition class for class-based condition calls, or the name for functional conditions.
	 *
	 * @return the key, or null if this unit can't be addressed
	 */
	default Object key() {
		return null;
	}
}
