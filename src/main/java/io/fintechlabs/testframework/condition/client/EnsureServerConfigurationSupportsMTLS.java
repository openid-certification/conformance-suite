package io.fintechlabs.testframework.condition.client;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureServerConfigurationSupportsMTLS extends AbstractCondition {

	public static final List<String> MTLS_AUTH_METHODS = ImmutableList.of(
		"tls_client_auth",
		"pub_key_tls_client_auth");

	/**
	 * @param testId
	 * @param log
	 */
	public EnsureServerConfigurationSupportsMTLS(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
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
				if (MTLS_AUTH_METHODS.contains(method.getAsString())) {
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
