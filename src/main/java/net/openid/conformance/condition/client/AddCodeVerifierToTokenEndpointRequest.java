package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCodeVerifierToTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request_form_parameters", strings = "code_verifier")
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		String code_verifier = env.getString("code_verifier");
		if (Strings.isNullOrEmpty(code_verifier)) {
			throw error("Couldn't find code_verifier value");
		}

		JsonObject o = env.getObject("token_endpoint_request_form_parameters");

		o.addProperty("code_verifier", code_verifier);

		env.putObject("token_endpoint_request_form_parameters", o);

		log(o);

		return env;

	}

}
