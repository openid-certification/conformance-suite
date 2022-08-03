package net.openid.conformance.openbanking_brasil.testmodules.support;


import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CopyResourceEndpointResponse extends AbstractCondition {
	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	@PostEnvironment(required = "resource_endpoint_response_full_copy")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("resource_endpoint_response_full").deepCopy();
		env.putObject("resource_endpoint_response_full_copy", response);
		logSuccess("Copied resource_endpoint_response_full", args("copied response", response));
		return env;
	}
}
