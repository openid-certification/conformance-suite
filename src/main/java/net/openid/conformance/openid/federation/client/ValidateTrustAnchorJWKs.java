package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractValidateJWKs;
import net.openid.conformance.testmodule.Environment;

public class ValidateTrustAnchorJWKs extends AbstractValidateJWKs {

	@Override
	@PreEnvironment(required = "trust_anchor_jwks")
	public Environment evaluate(Environment env) {
		JsonObject jwks = env.getObject("trust_anchor_jwks");

		checkJWKs(jwks, false);

		logSuccess("Valid trust anchor JWKs: keys are valid JSON, contain the required fields and are correctly encoded using unpadded base64url");

		return env;
	}
}
