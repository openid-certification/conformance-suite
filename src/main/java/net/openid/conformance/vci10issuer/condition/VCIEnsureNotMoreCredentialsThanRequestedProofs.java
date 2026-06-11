package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * OID4VCI 1.0 Final §8.3: "The number of elements in the credentials array matches the
 * number of keys that the Wallet has provided via the proofs parameter of the Credential
 * Request, unless the Issuer decides to issue fewer Credentials. Each key provided by the
 * Wallet is used to bind to, at most, one Credential." - so the issuer must never return
 * more credentials than proofs were sent.
 */
public class VCIEnsureNotMoreCredentialsThanRequestedProofs extends AbstractVCIBatchCredentialCountCheck {

	@Override
	@PreEnvironment(required = "extracted_credentials")
	public Environment evaluate(Environment env) {

		int credentialCount = getCredentialCount(env);
		int requestedCount = getRequestedProofCount(env);

		if (credentialCount > requestedCount) {
			throw error("The issuer returned more credentials than the number of key proofs sent in the credential request",
				args("credentials_returned", credentialCount, "proofs_sent", requestedCount));
		}

		logSuccess("The issuer did not return more credentials than the number of key proofs sent",
			args("credentials_returned", credentialCount, "proofs_sent", requestedCount));

		return env;
	}
}
