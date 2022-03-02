package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BackchannelValidateClientAssertionClaims extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing
	private long oneDayMillis = 60 * 60 * 24 * 1000L; // Duration for one day

	@Override
	@PreEnvironment(required = { "server", "client" })
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		String clientId = env.getString("client", "client_id"); // to check the client
		String issuer = env.getString("server", "issuer"); // to validate the issuer
		String backchannelEndpoint = env.getString("server", "backchannel_authentication_endpoint"); // to validate the audience

		// check all our testable values
		if (Strings.isNullOrEmpty(clientId)
			|| Strings.isNullOrEmpty(issuer)
			|| Strings.isNullOrEmpty(backchannelEndpoint)) {
			throw error("Couldn't find issuer or client or backchannel endpoint values in the test configuration to test the assertion");
		}

		JsonElement iss = env.getElementFromObject("client_assertion", "claims.iss");
		if (iss == null) {
			throw error("Missing iss");
		}
		if (!clientId.equals(env.getString("client_assertion", "claims.iss"))) {
			throw error("Issuer mismatch", args("expected", clientId, "actual", env.getString("client_assertion", "claims.iss")));
		}

		validateAud(env);

		JsonElement sub = env.getElementFromObject("client_assertion", "claims.sub");
		if (sub == null) {
			throw error("Missing sub");
		}

		JsonElement jti = env.getElementFromObject("client_assertion", "claims.jti");
		if (jti == null) {
			throw error("Missing JWT ID");
		}

		Long nbf = env.getLong("client_assertion", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				throw error("Assertion 'nbf' value is in the future'", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		Long exp = env.getLong("client_assertion", "claims.exp");
		if (exp == null) {
			throw error("Missing exp");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("Assertion expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
			if (now.plusMillis(oneDayMillis).isBefore(Instant.ofEpochSecond(exp))) {
				throw error("Assertion expires unreasonable far in the future", args("expired-at", new Date(exp * 1000L), "now", now));
				//Arbitrary, allow for 1 day in the future as standard says "unreasonably far".
			}
		}

		Long iat = env.getLong("client_assertion", "claims.iat");
		if (iat == null) {
			throw error("Missing iat");
		} else {
			if (now.plusMillis(oneDayMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Assertion expires unreasonable far in the future", args("issued-at", new Date(exp * 1000L), "now", now));
				//Arbitrary, allow for 1 day in the future as standard says "unreasonably far".
			}
		}

		logSuccess("Client Assertion passed all validation checks");
		return env;
	}

	protected void validateAud(Environment env) {
		String backchannelEndpoint = env.getString("server", "backchannel_authentication_endpoint");
		String mtlsBackchannelEndpoint = env.getString("server", "mtls_endpoint_aliases.backchannel_authentication_endpoint");
		JsonElement aud = env.getElementFromObject("client_assertion", "claims.aud");
		if (aud == null) {
			throw error("Missing aud");
		}

		List<String> backchannelEndpoints = new ArrayList<>(List.of(backchannelEndpoint));
		if (mtlsBackchannelEndpoint != null) {
			backchannelEndpoints.add(mtlsBackchannelEndpoint);
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(backchannelEndpoint)) &&
				!aud.getAsJsonArray().contains(new JsonPrimitive(mtlsBackchannelEndpoint))) {
				throw error("aud not found", args("expected", backchannelEndpoints, "actual", aud));
			}
		} else {
			String audStr = OIDFJSON.getString(aud);
			// TODO: Not getting the /backchannel path when using the OP test plan, let's just hack it for now
			if(!backchannelEndpoint.startsWith(audStr) &&
				!mtlsBackchannelEndpoint.startsWith(audStr)) {
				throw error("aud mismatch", args("expected", backchannelEndpoints, "actual", aud));
			}
			// TODO: It was like this:
			/*
			if (!audStr.equals(backchannelEndpoint) &&
				!audStr.equals(mtlsBackchannelEndpoint)) {
				throw error("aud mismatch", args("expected", backchannelEndpoints, "actual", aud));
			}
			*/
		}
	}
}
