package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.ValidateCnfJwkFields;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates the cnf.jwk in a client attestation JWT: checks it is a public key,
 * warns about unknown JWK fields and unexpected cnf fields.
 */
public class ValidateClientAttestationCnfJwkFields extends ValidateCnfJwkFields {

	@Override
	@PreEnvironment(required = {"client_attestation_object"})
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected JsonElement getCnfFromEnvironment(Environment env) {
		return env.getElementFromObject("client_attestation_object", "claims.cnf");
	}

	@Override
	protected String getContext() {
		return "client attestation JWT";
	}
}
