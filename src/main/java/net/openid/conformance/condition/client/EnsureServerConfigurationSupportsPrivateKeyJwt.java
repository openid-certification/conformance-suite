package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureServerConfigurationSupportsPrivateKeyJwt extends AbstractCondition {

	public static final String PRIVATEKEY_JWT_AUTH_METHOD = "private_key_jwt";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement supportedAuthMethods = env.getElementFromObject("server", "token_endpoint_auth_methods_supported");

		if (supportedAuthMethods == null) {
			// Null implies default (only client_secret_basic)
			throw error("Only default auth method supported");
		}

		boolean supportsPrivateKeyJwt = false;

		try {
			for (JsonElement method : supportedAuthMethods.getAsJsonArray()) {
				if (PRIVATEKEY_JWT_AUTH_METHOD.equals(OIDFJSON.getString(method))) {
					logSuccess("Found supported private_key_jwt method", args("method", method));
					supportsPrivateKeyJwt = true;
				}
			}
		} catch (ClassCastException e) {
			throw error("Invalid supported auth methods metadata", e, args("token_endpoint_auth_methods_supported", supportedAuthMethods));
		}

		if (supportsPrivateKeyJwt) {
			return env;
		} else {
			throw error("private_key_jwt is not listed as a supported client authentication method");
		}
	}
}
