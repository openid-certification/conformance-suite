package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilValidateRequestObjectEncryption extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		String alg = env.getString("dynamic_registration_request", "request_object_encryption_alg");
		String enc = env.getString("dynamic_registration_request", "request_object_encryption_enc");
		if(alg==null && enc == null) {
			log("request_object_encryption_alg and request_object_encryption_enc were not included in registration request");
			return env;
		}
		if("RSA-OAEP".equals(alg) && "A256GCM".equals(enc)) {
			logSuccess("Request object encryption options are valid", args("request_object_encryption_alg", alg,
				"request_object_encryption_enc", enc));
			return env;
		} else {
			throw error("Invalid request object encryption options", args("request_object_encryption_alg", alg, "request_object_encryption_enc", enc));
		}


	}
}
