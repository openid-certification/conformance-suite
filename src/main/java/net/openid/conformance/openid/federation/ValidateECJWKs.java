package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractValidateJWKs;
import net.openid.conformance.testmodule.Environment;

public class ValidateECJWKs extends AbstractValidateJWKs {

	@Override
	@PreEnvironment(required = "ec_jwks")
	public Environment evaluate(Environment env) {
		JsonObject jwks = env.getObject("ec_jwks");

		checkJWKs(jwks, false);

		logSuccess("Valid entity configuration JWKs: keys are valid JSON, contain the required fields and are correctly encoded using unpadded base64url");

		return env;
	}
}
