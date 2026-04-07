package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates the cnf.jwk in an SD-JWT VC credential: checks it is a public key,
 * warns about unknown JWK fields and unexpected cnf fields.
 */
public class ValidateCredentialCnfJwkIsPublicKey extends ValidateCnfJwkFields {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected JsonElement getCnfFromEnvironment(Environment env) {
		return env.getElementFromObject("sdjwt", "credential.claims.cnf");
	}

	@Override
	protected String getContext() {
		return "SD-JWT credential";
	}
}
