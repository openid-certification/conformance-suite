package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddFAPIFinancialIdToResourceEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "resource" })
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		String institutionId = env.getString("resource", "institution_id");
		if (Strings.isNullOrEmpty(institutionId)) {
			throw error("institution_id not found under resource in test configuration");
		}

		// get the previous headers if they exist
		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		if (headers == null) {
			headers = new JsonObject();
		}

		headers.addProperty("x-fapi-financial-id", institutionId);

		env.putObject("resource_endpoint_request_headers", headers);

		return env;

	}

}
