package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.stream.Stream;

public class ValidateClientAssertionClaimsForBackchannelEndpoint extends ValidateClientAssertionClaims {

	@Override
	protected void validateAud(Environment env) {
		String backchannelEndpoint = env.getString("server", "backchannel_authentication_endpoint");
		if (Strings.isNullOrEmpty(backchannelEndpoint)) {
			throw error("Couldn't find the backchannel authentication endpoint in the server configuration to test the assertion",
				args("server", env.getObject("server")));
		}

		JsonElement aud = env.getElementFromObject("client_assertion", "claims.aud");
		if (aud == null || aud.isJsonNull()) {
			throw error("Missing aud", args("aud", aud));
		}

		List<String> allowedAudiences = Stream.of(
			env.getString("server", "issuer"),
			env.getString("server", "token_endpoint"),
			backchannelEndpoint,
			env.getString("server", "mtls_endpoint_aliases.token_endpoint"),
			env.getString("server", "mtls_endpoint_aliases.backchannel_authentication_endpoint")
		).filter(value -> !Strings.isNullOrEmpty(value)).toList();

		if (aud.isJsonArray()) {
			if (allowedAudiences.stream().noneMatch(value -> aud.getAsJsonArray().contains(new JsonPrimitive(value)))) {
				throw error("aud not found", args("expected", allowedAudiences, "actual", aud));
			}
		} else if (!aud.isJsonPrimitive() || !aud.getAsJsonPrimitive().isString()) {
			throw error("aud is neither a string nor an array of strings", args("expected", allowedAudiences, "actual", aud));
		} else if (!allowedAudiences.contains(OIDFJSON.getString(aud))) {
			throw error("aud mismatch", args("expected", allowedAudiences, "actual", aud));
		}
	}
}
