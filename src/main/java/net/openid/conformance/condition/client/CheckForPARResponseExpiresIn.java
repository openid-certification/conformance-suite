package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

//PAR-2.2.1 : Since the request URI can be replayed, its lifetime SHOULD be short and preferably limited to one-time use.
public class CheckForPARResponseExpiresIn extends AbstractCondition {
	private static final int SECONDS_IN_YEAR = 365 * 24 * 60 * 60;
	@Override
	@PreEnvironment(required = CallPAREndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {
		Long expiresIn = env.getLong(CallPAREndpoint.RESPONSE_KEY, "body_json.expires_in");
		if (expiresIn == null) {
			throw error("expires_in is missing or empty in pushed authorization response");
		}
		/*
		do some minimum sanity checks on expiresIn - i.e. any value less than 5 seconds is stupid as it's
		unlikely to give the client enough time to use the uri, and any value more than 1 year is equally bad.
		(These values are relatively arbitrary choices and there should be a comment in the code saying that,
		the standard just says 'its lifetime SHOULD be short')
		 */
		if (expiresIn < 5) {
			throw error("expires_in too short and is unlikely to give the client enough time to use the request_uri");
		}
		if (expiresIn >= SECONDS_IN_YEAR) {
			throw error("expires_in too long, the specification requires it to be short", args("expires_in", expiresIn));
		}
		logSuccess("Found expires_in ",  args("expires_in", expiresIn));
		return env;
	}
}
