package net.openid.conformance.fapiciba.rp;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class NonIssuerAsAudClaim extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {

		String issuer = env.getString("server", "issuer"); // to validate the audience
		JsonElement aud = env.getElementFromObject("authorization_request_object", "claims.aud");

		String tokenEndpoint = env.getString("server", "token_endpoint");
		String backchannelEndpoint = env.getString("server", "backchannel_authentication_endpoint");
		Set<String> allowedAuds = ImmutableSet.of(tokenEndpoint, backchannelEndpoint);

		if (aud == null || aud.isJsonNull()) {
			throw error("The issuer identifier of the OP should be used as the value of the audience, " +
				"but token_endpoint or backchannel_authentication_endpoint are accepted.", args("expected", issuer, "actual", aud));
		}

		if (aud.isJsonArray()) {
			JsonArray auds = aud.getAsJsonArray();
			if(allowedAuds.stream().anyMatch(a -> auds.contains(new JsonPrimitive(a)))) {
				throw error("The issuer identifier of the OP should be used as the value of the audience, " +
					"but token_endpoint or backchannel_authentication_endpoint are accepted.", args("expected", issuer, "actual", aud));
			}
		} else {
			if(allowedAuds.stream().anyMatch(a -> a.equals(OIDFJSON.getString(aud)))) {
				throw error("The issuer identifier of the OP should be used as the value of the audience, " +
					"but token_endpoint or backchannel_authentication_endpoint are accepted.", args("expected", issuer, "actual", aud));
			}
		}

		return env;
	}

}
