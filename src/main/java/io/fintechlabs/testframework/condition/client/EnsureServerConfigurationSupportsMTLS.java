package io.fintechlabs.testframework.condition.client;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class EnsureServerConfigurationSupportsMTLS extends AbstractCondition {

	public static final List<String> MTLS_AUTH_METHODS = ImmutableList.of(
		"tls_client_auth",
		"self_signed_tls_client_auth");

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement supportedAuthMethods = env.getElementFromObject("server", "token_endpoint_auth_methods_supported");

		if (supportedAuthMethods == null) {
			// Null implies default (only client_secret_basic)
			throw error("Only default auth method supported");
		}

		boolean supportsMtls = false;

		try {
			for (JsonElement method : supportedAuthMethods.getAsJsonArray()) {
				if (MTLS_AUTH_METHODS.contains(OIDFJSON.getString(method))) {
					logSuccess("Found supported MTLS method", args("method", method));
					supportsMtls = true;
				}
			}
		} catch (ClassCastException e) {
			throw error("Invalid supported auth methods metadata", e, args("token_endpoint_auth_methods_supported", supportedAuthMethods));
		}

		if (supportsMtls) {
			return env;
		} else {
			throw error("No MTLS auth methods supported", args("token_endpoint_auth_methods_supported", supportedAuthMethods));
		}
	}

}
