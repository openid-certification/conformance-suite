package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckIdTokenSignatureAlgorithm extends AbstractCheckIdTokenSignatureAlgorithm {

	@Override
	@PreEnvironment(required = {"dynamic_registration_request", "id_token"})
	public Environment evaluate(Environment env) {

		String requestedAlg = env.getString("dynamic_registration_request", "id_token_signed_response_alg");
		if (Strings.isNullOrEmpty(requestedAlg)) {
			throw error("id_token_signed_response_alg not found in dynamic registration request",
					args("dynamic_registration_request", env.getObject("dynamic_registration_request")));
		}

		return checkIdTokenSignatureAlgorithm(env, requestedAlg);
	}

}
