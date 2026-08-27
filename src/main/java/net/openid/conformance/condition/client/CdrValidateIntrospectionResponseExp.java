package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class CdrValidateIntrospectionResponseExp extends AbstractCondition {

	// generous allowance for clock skew and the time between authorisation and this check
	private static final long PERMITTED_SKEW_SECONDS = 3600;

	@Override
	@PreEnvironment(required = CallTokenIntrospectionEndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {

		Long exp = env.getLong(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json.exp");

		if (exp == null) {
			throw error("exp is missing from the introspection response; CDR requires it so the Data Recipient can determine the sharing arrangement expiry",
				args("introspection_response", env.getElementFromObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json")));
		}

		long expected = Instant.now().getEpochSecond()
			+ AddCdrSharingDurationClaimToAuthorizationEndpointRequest.SHARING_DURATION_SECONDS;

		if (exp < expected - PERMITTED_SKEW_SECONDS || exp > expected + PERMITTED_SKEW_SECONDS) {
			throw error("The introspection response exp does not match the sharing_duration that was requested and authorised",
				args("exp", exp, "expected_approximately", expected, "permitted_skew_seconds", PERMITTED_SKEW_SECONDS));
		}

		logSuccess("The introspection response exp matches the requested sharing_duration",
			args("exp", exp, "expected_approximately", expected, "permitted_skew_seconds", PERMITTED_SKEW_SECONDS));
		return env;
	}

}
