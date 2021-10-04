package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CloneConsentResponseToResourceResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "consent_endpoint_response")
	@PostEnvironment(required = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject consentEndpointResponse = env.getObject("consent_endpoint_response");
		env.putObject("resource_endpoint_response", consentEndpointResponse);
		env.putString("resource_endpoint_response", consentEndpointResponse.toString());
		logSuccess("Cloning value");
		return env;
	}
}
