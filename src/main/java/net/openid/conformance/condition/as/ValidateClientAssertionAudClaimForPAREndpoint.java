package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/*
 * [RFC9126]:  2. Pushed Authorization Request Endpoint
 *
 * the issuer identifier URL of the authorization server according to [RFC8414]
 * SHOULD be used as the value of the audience
 */

public class ValidateClientAssertionAudClaimForPAREndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "server", "client" })
	public Environment evaluate(Environment env) {
		String      issuer = env.getString("server", "issuer");
		JsonElement aud    = env.getElementFromObject("client_assertion", "claims.aud");

		if (aud == null) {
			throw error("Missing aud");
		}

		if (aud.isJsonArray()) {
			if (! (aud.getAsJsonArray().contains(new JsonPrimitive(issuer)) && aud.getAsJsonArray().size() == 1)) {
				throw error("aud claim does not match the authentication server issuer url",
					    args("expected", issuer, "actual", aud));
			}
		} else {
			if (!issuer.equals(OIDFJSON.getString(aud))) {
				throw error("aud claim does not match the authentication server issuer url",
					    args("expected", issuer, "actual", aud));
			}
		}

		logSuccess("Client Assertion 'aud' claim matches the authentication server issuer url");
		return env;
	}
}
