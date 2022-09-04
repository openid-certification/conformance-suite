package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 *  Always PS256
 */
public class FAPIBrazilValidateRequestObjectSigningAlg extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getRequestObjectSigningAlg();
		if(alg==null) {
			logSuccess("request_object_signing_alg is not set");
			return env;
		}

		if("PS256".equals(alg)) {
			logSuccess("request_object_signing_alg is PS256");
			return env;
		}
		throw error("Unexpected request_object_signing_alg", args("alg", alg, "expected", "PS256"));
	}
}
