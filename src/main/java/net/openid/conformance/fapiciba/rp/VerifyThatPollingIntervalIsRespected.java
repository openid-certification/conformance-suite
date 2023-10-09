package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class VerifyThatPollingIntervalIsRespected extends AbstractCondition {

	public static final int DEFAULT_INTERVAL = 0; // TODO CIBA 7.3 If no value is provided, clients MUST use 5 as the default value.

	@Override
	@PostEnvironment(strings = { "next_allowed_token_request" })
	public Environment evaluate(Environment env) {

		String nextAllowedTokenRequest = env.getString("next_allowed_token_request");
		Integer interval = env.getInteger("interval");
		int intervalOrDefault = interval != null ? interval : DEFAULT_INTERVAL;

		if(nextAllowedTokenRequest != null) {
			Instant limit = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(nextAllowedTokenRequest));
			Instant now = Instant.now();
			if(now.isBefore(limit)) {
				throw error("The request was made before the required interval had elapsed.",
					args("interval", intervalOrDefault,
						"limit", DateTimeFormatter.ISO_INSTANT.format(limit),
						"now", DateTimeFormatter.ISO_INSTANT.format(now)));
			}
		}

		nextAllowedTokenRequest = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(intervalOrDefault));
		env.putString("next_allowed_token_request", nextAllowedTokenRequest);
		logSuccess("Setting time for next allowed request", Map.of("next_allowed_token_request", nextAllowedTokenRequest));

		return env;
	}

}
