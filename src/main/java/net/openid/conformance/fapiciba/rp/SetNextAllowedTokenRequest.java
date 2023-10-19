package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SetNextAllowedTokenRequest extends AbstractCondition {

	@Override
	@PostEnvironment(strings = { "next_allowed_token_request" })
	public Environment evaluate(Environment env) {

		Integer interval = env.getInteger("interval");
		int intervalOrDefault = interval != null ? interval : VerifyThatPollingIntervalIsRespected.DEFAULT_INTERVAL;

		String nextAllowedTokenRequest = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(intervalOrDefault));
		env.putString("next_allowed_token_request", nextAllowedTokenRequest);
		logSuccess("Setting time for next allowed request", Map.of("next_allowed_token_request", nextAllowedTokenRequest));

		return env;
	}

}
