package net.openid.conformance.authzen.condition;

import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.CheckDiscEndpointAllEndpointsAreHttps;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

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
			if (discoveryDoc.has(endpoint)) {
				env = validate(env, endpoint);
			}
		}
		return env;
	}
}
