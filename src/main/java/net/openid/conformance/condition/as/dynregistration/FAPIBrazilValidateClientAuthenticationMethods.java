package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

		Set<String> allowed = Set.of("tls_client_auth", "self_signed_tls_client_auth", "private_key_jwt");

		if(allowed.contains(authMethod)) {
			logSuccess("token_endpoint_auth_method is valid", args("token_endpoint_auth_method", authMethod));
			return env;
		} else {
			throw error("Invalid token_endpoint_auth_method", args("allowed", allowed, "actual", authMethod));
		}


	}
}
