package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the device key in the MSO of the issued mdoc credential (extracted to
 * 'mdoc_device_key_jwk' in JWK form by ParseMdocCredentialFromVCIIssuance) is one of the
 * keys the proofs in the credential request demonstrated possession of.
 */
public class VCIEnsureMdocDeviceKeyMatchesProofKey extends AbstractVCIEnsureBindingKeyMatchesProofKey {

	@Override
	@PreEnvironment(required = { "mdoc_device_key_jwk", "credential_request_proofs" })
	public Environment evaluate(Environment env) {

		checkBindingKeyWasSent(env, env.getObject("mdoc_device_key_jwk"), "MSO device key");

		return env;
	}
}
