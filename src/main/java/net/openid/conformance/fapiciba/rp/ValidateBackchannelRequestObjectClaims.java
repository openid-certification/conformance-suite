package net.openid.conformance.fapiciba.rp;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.as.ValidateRequestObjectClaims;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

public class ValidateBackchannelRequestObjectClaims extends ValidateRequestObjectClaims {

	@Override
	protected void validateAud(Environment env) {
		JsonElement aud = env.getElementFromObject("authorization_request_object", "claims.aud");
		if (aud == null) {
			throw error("Missing audience");
		}

		String issuer = env.getString("server", "issuer");
		String tokenEndpoint = env.getString("server", "token_endpoint");
		String backchannelEndpoint = env.getString("server", "backchannel_authentication_endpoint");
		Set<String> allowedAuds = ImmutableSet.of(issuer, tokenEndpoint, backchannelEndpoint);

		if (aud.isJsonArray()) {
			JsonArray auds = aud.getAsJsonArray();
			if(allowedAuds.stream().noneMatch(a -> auds.contains(new JsonPrimitive(a)))) {
				throw error("Audience not found", args("expected", issuer, "actual", aud));
			}
		} else {
			if(allowedAuds.stream().noneMatch(a -> a.equals(OIDFJSON.getString(aud)))) {
				throw error("Audience mismatch", args("expected", issuer, "actual", aud));
			}
		}
	}

	@Override
	protected void validateIat(Environment env, Instant now) {
		Long iat = env.getLong("authorization_request_object", "claims.iat");
		if (iat == null) {
			throw error("Missing iat", args("iat", iat));
		} else {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				throw error("Token issued in the future", args("iat", new Date(iat * 1000L), "now", now));
			}
		}
	}

	@Override
	protected void validateJti(Environment env) {
		String jti = env.getString("authorization_request_object", "claims.jti");
		if (Strings.isNullOrEmpty(jti)) {
			throw error("Missing jti", args("jti", jti));
		}
		super.validateJti(env);
	}

}
