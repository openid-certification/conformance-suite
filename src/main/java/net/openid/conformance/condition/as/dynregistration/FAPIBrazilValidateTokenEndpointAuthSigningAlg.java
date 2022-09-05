package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 *  Always PS256
 *
 */
public class FAPIBrazilValidateTokenEndpointAuthSigningAlg extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");

		String alg = getTokenEndpointAuthSigningAlg();
		if(alg == null) {
			logSuccess("token_endpoint_auth_signing_alg is not set");
			return env;
		}

		String clientAuthType = getTokenEndpointAuthMethod();

		if("private_key_jwt".equals(clientAuthType)) {
			if("PS256".equals(alg)) {
				logSuccess("token_endpoint_auth_signing_alg is valid",
					args("token_endpoint_auth_signing_alg", alg));
				return env;
			} else {
				throw error("Invalid algorithm for private_key_jwt", args("token_endpoint_auth_signing_alg", alg, "expected", "PS256"));
			}
		}
		else {
			log("token_endpoint_auth_signing_alg is set but it is not applicable to client authentication method",
				args("token_endpoint_auth_signing_alg", alg, "token_endpoint_auth_method", clientAuthType));
			return env;
		}
	}
}
