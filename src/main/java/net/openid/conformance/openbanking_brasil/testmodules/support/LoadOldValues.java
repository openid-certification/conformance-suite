package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class LoadOldValues extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"old_protected_resource_url", "old_resource_endpoint_response"}, required = {"old_resource_endpoint_response_headers", "old_resource_endpoint_response_full"})
	public Environment evaluate(Environment env) {
		env.putString("protected_resource_url", env.getString("old_protected_resource_url"));
		env.putString("resource_endpoint_response", env.getString("old_resource_endpoint_response"));
		env.putObject("resource_endpoint_response_headers", env.getObject("old_resource_endpoint_response_headers"));
		env.putObject("resource_endpoint_response_full", env.getObject("old_resource_endpoint_response_full"));

		log("Loading old environment values");
		return env;
	}
}
