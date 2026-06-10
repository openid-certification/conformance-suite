package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * OID4VCI 1.0 Final §8.3 permits the issuer to return fewer credentials than the number of
 * provided proofs ("unless the Issuer decides to issue fewer Credentials"), so this is not
 * a specification violation - call this condition with ConditionResult.WARNING to surface
 * it as it may indicate an interoperability issue or an unadvertised lower batch limit.
 */
public class VCIWarnIfFewerCredentialsThanRequestedProofs extends AbstractVCIBatchCredentialCountCheck {

	@Override
	@PreEnvironment(required = "extracted_credentials")
	public Environment evaluate(Environment env) {

		int credentialCount = getCredentialCount(env);
		int requestedCount = getRequestedProofCount(env);

		if (credentialCount < requestedCount) {
			throw error("The issuer returned fewer credentials than the number of key proofs sent in the credential request. "
					+ "This is permitted, but wallets expecting one credential per proof may not interoperate.",
				args("credentials_returned", credentialCount, "proofs_sent", requestedCount));
		}

		logSuccess("The issuer returned one credential per key proof sent",
			args("credentials_returned", credentialCount, "proofs_sent", requestedCount));

		return env;
	}
}
