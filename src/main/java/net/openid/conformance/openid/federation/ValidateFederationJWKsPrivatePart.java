package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractValidateJWKs;
import net.openid.conformance.testmodule.Environment;

public class ValidateFederationJWKsPrivatePart extends AbstractValidateJWKs {

	@Override
	@PreEnvironment(required = "federation_jwks")
	public Environment evaluate(Environment env) {
		JsonElement jwks = env.getObject("federation_jwks");

		checkJWKs(jwks, true);

		logSuccess("Valid JWKs: keys are valid JSON, contain the required fields, the private/public exponents match and are correctly encoded using unpadded base64url");

		return env;
	}
}
