package net.openid.conformance.vci10issuer.condition.clientattestation;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

/**
 * Invalidates the signature on the client attestation proof-of-possession JWT.
 * This is used for negative testing to ensure the authorization server properly rejects
 * client attestation pop JWTs with invalid signatures.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
public class VCIInvalidateClientAttestationPopSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "client_attestation_pop")
	@PostEnvironment(strings = "client_attestation_pop")
	public Environment evaluate(Environment env) {
		return invalidateSignature(env, "client_attestation_pop");
	}
}
