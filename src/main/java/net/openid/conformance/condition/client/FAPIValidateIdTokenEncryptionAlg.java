package net.openid.conformance.condition.client;

import com.nimbusds.jose.JWEAlgorithm;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIValidateIdTokenEncryptionAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		String alg = env.getString("id_token", "jwe_header.alg");

		if (alg.equals("RSA1_5")) {
			throw error("id_token encrypted with RSA1_5, which is not permitted by FAPI specification", args("alg", alg));
		}
		JWEAlgorithm algorithm = JWEAlgorithm.parse(alg);
		if (JWEAlgorithm.Family.SYMMETRIC.contains(algorithm)) {
			throw error("id_token encrypted with a symmetric algorithm, whereas FAPI requires the use of asymmetric cryptography", args("alg", alg));
		}

		logSuccess("id_token was encrypted with a permitted algorithm", args("alg", alg));
		return env;

	}

}
