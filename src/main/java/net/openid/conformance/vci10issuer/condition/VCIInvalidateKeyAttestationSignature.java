package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

/**
 * Invalidates the signature on the key attestation JWT.
 * This is used for negative testing to ensure the issuer properly rejects
 * key attestations with invalid signatures.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-D.1">OID4VCI Appendix D.1 - Key Attestation in JWT format</a>
 */
public class VCIInvalidateKeyAttestationSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "key_attestation_jwt")
	@PostEnvironment(strings = "key_attestation_jwt")
	public Environment evaluate(Environment env) {
		return invalidateSignature(env, "key_attestation_jwt");
	}
}
