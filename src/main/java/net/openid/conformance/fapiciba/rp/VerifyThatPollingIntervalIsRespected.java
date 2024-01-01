package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class VerifyThatPollingIntervalIsRespected extends AbstractCondition {

	public static final int DEFAULT_INTERVAL = 5; // CIBA 7.3 If no value is provided, clients MUST use 5 as the default value.

	@Override
	public Environment evaluate(Environment env) {

		String nextAllowedTokenRequest = env.getString("next_allowed_token_request");
		Integer interval = env.getInteger("interval");
		int intervalOrDefault = interval != null ? interval : DEFAULT_INTERVAL;

		Instant now = Instant.now();

		if(nextAllowedTokenRequest != null) {
			Instant limit = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(nextAllowedTokenRequest));
			if(now.isBefore(limit)) {
				throw error("The request was made before the required interval had elapsed.",
					args("interval", intervalOrDefault,
						"limit", DateTimeFormatter.ISO_INSTANT.format(limit),
						"now", DateTimeFormatter.ISO_INSTANT.format(now)));
			}
		}

		logSuccess("The polling interval was respected", args(
			"interval", intervalOrDefault,
			"now", DateTimeFormatter.ISO_INSTANT.format(now),
			"next_allowed_token_request", nextAllowedTokenRequest)
		);

		return env;
	}

}
