package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the cnf.jwk of the issued SD-JWT credential (parsed into the 'sdjwt'
 * environment object) is one of the keys the proofs in the credential request demonstrated
 * possession of.
 */
public class VCIEnsureSdJwtCnfMatchesProofKey extends AbstractVCIEnsureBindingKeyMatchesProofKey {

	@Override
	@PreEnvironment(required = {"sdjwt", "credential_request_proofs"})
	public Environment evaluate(Environment env) {

		JsonElement jwkEl = env.getElementFromObject("sdjwt", "credential.claims.cnf.jwk");
		if (jwkEl == null || !jwkEl.isJsonObject()) {
			throw error("The issued credential does not contain a 'cnf' claim with a 'jwk' member binding it to "
					+ "one of the keys the proofs in the credential request demonstrated possession of",
				args("claims", env.getElementFromObject("sdjwt", "credential.claims")));
		}

		checkBindingKeyWasSent(env, jwkEl, "cnf key");

		return env;
	}
}
