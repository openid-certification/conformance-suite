package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureDiscoveryEndpointResponseStatusCodeIs200 extends AbstractCondition {


	@Override
	@PreEnvironment(required = "discovery_endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("discovery_endpoint_response", "status");

		if(statusCode != 200) {
			throw error("discovery_endpoint_response returned an unexpected status code", args("http_status", statusCode));
		}

		logSuccess("discovery_endpoint_response returned http 200 as expected", args("http_status", statusCode));

		return env;

	}

}
