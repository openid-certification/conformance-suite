package net.openid.conformance.authzen.condition;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.CheckDiscEndpointAllEndpointsAreHttps;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Checks the endpoints in the PDP's discovered metadata: Section 9.1.1 makes
 * policy_decision_point and access_evaluation_endpoint REQUIRED, and every endpoint present must
 * be an https URL.
 */
public class CheckPDPServerConfiguration extends CheckDiscEndpointAllEndpointsAreHttps {

	protected List<String> getRequiredEndpoints() {
		return ImmutableList.of("policy_decision_point",
			"access_evaluation_endpoint");
	}

	protected List<String> getOptionalEndpoints() {
		return ImmutableList.of("access_evaluations_endpoint",
			"search_subject_endpoint",
			"search_resource_endpoint",
			"search_action_endpoint");
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		for (String key : getRequiredEndpoints()) {
			env = validate(env, key);
		}

		var discoveryDoc = env.getObject("server");
		for (String endpoint : getOptionalEndpoints()) {
			if (isProvided(discoveryDoc, endpoint)) {
				env = validate(env, endpoint);
			}
		}
		return env;
	}

	/**
	 * Whether an optional endpoint is there to check. In PDP metadata any member present is
	 * checked, so an empty value fails as the invalid URL it is.
	 */
	protected boolean isProvided(JsonObject server, String endpoint) {
		return server.has(endpoint);
	}
}
