package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.List;

public class FAPI2ValidateRequestObjectSigningAlg extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {

		String alg = env.getString("authorization_request_object", "header.alg");

		List<String> permitted = Arrays.asList(FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported.FAPI2_ALLOWED_ALGS);
		if (permitted.contains(alg)) {
			logSuccess("Request object was signed with a permitted algorithm", args("alg", alg));

			return env;
		}

		throw error("Request object must be signed with PS256, ES256, or EdDSA", args("alg", alg));
	}
}
