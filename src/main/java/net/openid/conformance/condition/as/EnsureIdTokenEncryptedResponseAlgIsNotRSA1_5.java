package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIdTokenEncryptedResponseAlgIsNotRSA1_5 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		String alg = env.getString("client", "id_token_encrypted_response_alg");
		if("RSA1_5".equals(alg)) {
			throw error("RSA1_5 is not allowed");
		}
		logSuccess("Id token encryption algorithm is not RSA1_5", args("alg", alg));
		return env;
	}

}
