package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddDpopHeaderForParEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "dpop_proof")
	@PostEnvironment(required = "pushed_authorization_request_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		String dpopProof = env.getString("dpop_proof");

		JsonObject headers = env.getObject("pushed_authorization_request_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("pushed_authorization_request_endpoint_request_headers", headers);
		}

		headers.addProperty("DPoP", dpopProof);

		logSuccess("Set DPoP header for PAR endpoint", args("DPoP", dpopProof));

		return env;
	}

}
