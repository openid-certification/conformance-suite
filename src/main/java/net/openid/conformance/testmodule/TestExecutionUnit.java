package net.openid.conformance.testmodule;

import java.util.function.UnaryOperator;

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

	/**
	 * Rebuild this unit with {@code leafMapper} applied to every leaf unit below it, used to apply sequence
	 * modifications (replace/skip/insert). Leaves return {@code leafMapper.apply(this)}; containers return a copy of
	 * themselves holding their transformed children. Called while the test runs, so containers that build their
	 * children lazily (such as condition sequences) can create them from the context.
	 */
	default TestExecutionUnit transform(ExecutionContext context, UnaryOperator<TestExecutionUnit> leafMapper) {
		return leafMapper.apply(this);
	}
}
