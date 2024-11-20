package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateClientAssertionAudClaimIsIssuerAsString extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "server", "client_assertion" })
	public Environment evaluate(Environment env) {
		String      issuer = env.getString("server", "issuer");
		JsonElement aud    = env.getElementFromObject("client_assertion", "claims.aud");

		if (aud == null) {
			throw error("Missing aud not present in private_key_jwt client assertion");
		}

		if (aud.isJsonArray()) {
			throw error("private_key_jwt aud claim is an array but should be a simple string", args("expected", issuer, "actual", aud));
		}
		if (!issuer.equals(OIDFJSON.getString(aud))) {
			throw error("private_key_jwt aud claim does not match the authentication server issuer url", args("expected", issuer, "actual", aud));
		}

		logSuccess("private_key_jwt client Assertion 'aud' claim matches the authentication server issuer url");
		return env;
	}
}
