package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class ValidateDpopAccessToken extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	@Override
	@PreEnvironment(required =  "incoming_dpop_access_token", strings = "dpop_access_token")
	public Environment evaluate(Environment env) {

		String dpopAccessToken = env.getString("dpop_access_token");
		String incomingDpopToken = env.getString("incoming_dpop_access_token", "value");
		if(dpopAccessToken.equals(incomingDpopToken)) {
			// TODO Verify signature if possible, but since it's saved after generation, it should be OK just to compare values

			Instant now = Instant.now();
			Long iat = env.getLong("incoming_dpop_access_token", "claims.iat");
			if (iat == null) {
				throw error("'iat' claim in DPoP Access Token is missing");
			}
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("DPoP Access Token 'iat' in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(iat))) {
				// as per OIDCC, the client can reasonably assume servers send iat values that match the current time:
				// "The iat Claim can be used to reject tokens that were issued too far away from the current time, limiting
				// the amount of time that nonces need to be stored to prevent attacks. The acceptable range is Client specific."
				throw error("DPoP Access Token  'iat' more than 5 minutes in the past", args("issued-at", new Date(iat * 1000L), "now", now));
			}

			// nbf
			Long nbf = env.getLong("incoming_dpop_access_token", "claims.nbf");
			if (nbf != null) {
				if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
					// this is just something to log, it doesn't make the token invalid
					log("DPoP Access Token has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
				}
			}

			// exp
			Long exp = env.getLong("incoming_dpop_access_token", "claims.exp");
			if (exp != null) {
				if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(nbf))) {
					// this is just something to log, it doesn't make the token invalid
					log("DPoP Access Token is expired", args("expired", new Date(exp * 1000L), "now", now));
				}
			}
			logSuccess("DPoP Acess Token is valid", args("DPoP Access Token", dpopAccessToken));
			return env;
		} else {
			throw error("Invalid DPoP Access Token", args("expected", dpopAccessToken, "actual", incomingDpopToken));
		}
	}

}
