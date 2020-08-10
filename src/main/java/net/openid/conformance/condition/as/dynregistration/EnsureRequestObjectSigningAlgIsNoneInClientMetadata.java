package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureRequestObjectSigningAlgIsNoneInClientMetadata extends AbstractClientValidationCondition {

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getRequestObjectSigningAlg();

		if("none".equals(alg)) {
			logSuccess("request_object_signing_alg is none");
			return env;
		}
		throw error("Unexpected request_object_signing_alg. 'none' is required for this test.",
			args("actual", alg, "expected", "none"));
	}
}
