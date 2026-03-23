package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class InvalidateSdJwtCredentialSignature extends AbstractInvalidateJwsSignature {

	@Override
	@PreEnvironment(strings = "credential")
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {
		String credential = env.getString("credential");
		int firstTilde = credential.indexOf('~');
		if (firstTilde < 0) {
			throw error("SD-JWT credential does not contain any disclosures",
				args("credential", credential));
		}
		String issuerJwt = credential.substring(0, firstTilde);
		String suffix = credential.substring(firstTilde);

		String invalidIssuerJwt = invalidateSignatureString("credential", issuerJwt);

		env.putString("credential", invalidIssuerJwt + suffix);

		log("Invalidated issuer signature in SD-JWT credential");

		return env;
	}
}
