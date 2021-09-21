package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SaveOldValues extends AbstractCondition {

	@Override
	@PostEnvironment(strings = {"old_protected_resource_url", "old_resource_endpoint_response"}, required = {"old_resource_endpoint_response_headers", "old_resource_endpoint_response_full"})
	public Environment evaluate(Environment env) {
		if (Strings.isNullOrEmpty(env.getString("protected_resource_url"))) {
			env.putString("old_protected_resource_url", "");
		} else {
			env.putString("old_protected_resource_url", env.getString("protected_resource_url"));
		}
		env.putString("old_resource_endpoint_response", env.getString("resource_endpoint_response"));
		env.putObject("old_resource_endpoint_response_headers", env.getObject("resource_endpoint_response_headers"));
		env.putObject("old_resource_endpoint_response_full", env.getObject("resource_endpoint_response_full"));

		log("Saving old environment values");
		return env;
	}
}
