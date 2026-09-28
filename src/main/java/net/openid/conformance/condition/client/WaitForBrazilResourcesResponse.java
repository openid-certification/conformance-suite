package net.openid.conformance.condition.client;

import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;

import java.util.concurrent.TimeUnit;

public class WaitForBrazilResourcesResponse extends AbstractWaitForSpecifiedSeconds {

	private final long deadline;
	private final int attempt;

	public WaitForBrazilResourcesResponse(long deadline, int attempt) {
		this.deadline = deadline;
		this.attempt = attempt;
	}

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		long remainingSeconds = TimeUnit.NANOSECONDS.toSeconds(deadline - System.nanoTime());
		if (remainingSeconds <= 0) {
			throw error("Resources API still returned HTTP 202 after the five-minute polling budget " +
				"measured from receipt of the CIBA access token. " +
				"A final resource response is required to complete this test.");
		}
		long delay = Math.min(30, 1L << Math.min(attempt, 5));
		return Math.min(delay, remainingSeconds);
	}
}
