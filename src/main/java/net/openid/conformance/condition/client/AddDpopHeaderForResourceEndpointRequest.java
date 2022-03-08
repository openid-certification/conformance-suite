package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddDpopHeaderForResourceEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_request_headers", strings = "dpop_proof")
	public Environment evaluate(Environment env) {

		String dpopProof = env.getString("dpop_proof");

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		requestHeaders.addProperty("DPoP", dpopProof);

		logSuccess("Set DPoP header", args("DPoP", dpopProof));

		return env;
	}

}
