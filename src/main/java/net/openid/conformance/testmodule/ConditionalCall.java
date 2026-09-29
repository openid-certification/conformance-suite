package net.openid.conformance.testmodule;

import net.openid.conformance.logging.TestInstanceEventLog;

/**
 * Executes a condition once its skip prerequisites have been checked.
 */
@FunctionalInterface
public interface ConditionalCall {
	void execute(String id, TestInstanceEventLog eventLog, TestLockManager testLockManager, Environment env);
}
