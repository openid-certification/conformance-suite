package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestObjectSigningAlgIsRS256InClientMetadata extends AbstractClientValidationCondition {

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getRequestObjectSigningAlg();

		if("RS256".equals(alg)) {
			logSuccess("request_object_signing_alg is RS256");
			return env;
		}
		throw error("Unexpected request_object_signing_alg. RS256 is required for this test.",
			args("actual", alg, "expected", "RS256"));
	}
}
