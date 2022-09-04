package net.openid.conformance.condition.as.dynregistration;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class FAPIBrazilValidateClientAuthenticationMethods extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	public Environment evaluate(Environment env) {
		//tls_client_auth, self_signed_tls_client_auth, private_key_jwt
		String authMethod = env.getString("dynamic_registration_request", "token_endpoint_auth_method");
		if(Strings.isNullOrEmpty(authMethod)) {
			throw error("token_endpoint_auth_method is not set in dynamic registration request. " +
				"The default value, client_secret_basic, is not allowed by Open Banking Brazil specifications " +
				"so token_endpoint_auth_method must be set in registration request.");
		}

		Set<String> allowed = Set.of("tls_client_auth", "self_signed_tls_client_auth", "private_key_jwt");

		if(allowed.contains(authMethod)) {
			logSuccess("token_endpoint_auth_method is valid", args("token_endpoint_auth_method", authMethod));
			return env;
		} else {
			throw error("Invalid token_endpoint_auth_method", args("allowed", allowed, "actual", authMethod));
		}


	}
}
