package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPICIBAEnsureRegistrationRequestSigningAlgIsPS256 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement signingAlgorithm = env.getElementFromObject(
			"dynamic_registration_request", "backchannel_authentication_request_signing_alg");
		if (signingAlgorithm == null
			|| !signingAlgorithm.isJsonPrimitive()
			|| !signingAlgorithm.getAsJsonPrimitive().isString()) {
			throw error("backchannel_authentication_request_signing_alg must be the string PS256",
				args("backchannel_authentication_request_signing_alg", signingAlgorithm));
		}

		String signingAlgorithmValue = OIDFJSON.getString(signingAlgorithm);
		if (!"PS256".equals(signingAlgorithmValue)) {
			throw error("Open Finance Brazil CIBA registration must use PS256 request signing",
				args("backchannel_authentication_request_signing_alg", signingAlgorithmValue,
					"required", "PS256"));
		}

		logSuccess("Registration request uses PS256 for CIBA request signing");
		return env;
	}
}
