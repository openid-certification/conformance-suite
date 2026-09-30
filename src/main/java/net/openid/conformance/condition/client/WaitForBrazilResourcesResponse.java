package net.openid.conformance.condition.client;

import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;

import java.util.concurrent.TimeUnit;

// The Resources guidance defines five minutes and recommends exponential retry ("Recomendação uso de polling"):
// https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/219512943/Orienta+es+-+DC+Recursos
// The suite counts from token receipt to allow time for user approval; the exact delays and cap are suite choices.
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
			throw error("Resources API still returned HTTP 202 after the suite's five-minute polling budget " +
				"measured from receipt of the CIBA access token. " +
				"A final resource response is required to complete this test.");
		}
		long delay = Math.min(30, 1L << Math.min(attempt, 5));
		return Math.min(delay, remainingSeconds);
	}
}
