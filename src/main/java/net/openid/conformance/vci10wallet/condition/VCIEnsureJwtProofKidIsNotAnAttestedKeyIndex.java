package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIEnsureJwtProofKidIsNotAnAttestedKeyIndex extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String kid = env.getString(AbstractVCIValidateCredentialRequestProof.KID_ATTESTED_KEY_INDEX_ENV_KEY);
		if (kid != null) {
			throw error("The proof JWT identifies its signing key with a kid that is an index into the attested_keys of its key_attestation header. "
					+ "OpenID4VCI 1.0 does not define this use of kid; the suite accepted it, but other issuers may not. "
					+ "Identify the key with the jwk header instead.",
				args("kid", kid));
		}
		logSuccess("The proof JWT's kid was not used as an index into attested_keys");
		return env;
	}
}
