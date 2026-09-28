package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.TestLockManager;

import java.util.concurrent.TimeUnit;

public abstract class AbstractWaitForSpecifiedSeconds extends AbstractCondition {

	protected abstract long getExpectedWaitSeconds(Environment env);

	protected void sleepForSeconds(long seconds) throws InterruptedException {
		TimeUnit.SECONDS.sleep(seconds);
	}

	@Override
	public Environment evaluate(Environment env) {
		try {
			long expectedWaitSeconds = getExpectedWaitSeconds(env);

			logSuccess("Pausing for " + expectedWaitSeconds + " seconds");

			TestLockManager lockManager = getLockManager();
			if (lockManager != null) {
				lockManager.releaseLock();
			}
			try {
				sleepForSeconds(expectedWaitSeconds);
			} finally {
				if (lockManager != null) {
					lockManager.reacquireLock();
				}
			}

			logSuccess("Woke up after " + expectedWaitSeconds + " seconds sleep");
		} catch (InterruptedException e) {
			throw error("Interrupted while sleeping", e);
		}
		return env;
	}


}
