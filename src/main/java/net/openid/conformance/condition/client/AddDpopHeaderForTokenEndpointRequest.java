package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddDpopHeaderForTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "dpop_proof")
	@PostEnvironment(required = "token_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		String dpopProof = env.getString("dpop_proof");

		JsonObject headers = env.getObject("token_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("token_endpoint_request_headers", headers);
		}

		headers.addProperty("DPoP", dpopProof);

		logSuccess("Set DPoP header", args("DPoP", dpopProof));

		return env;
	}

}
