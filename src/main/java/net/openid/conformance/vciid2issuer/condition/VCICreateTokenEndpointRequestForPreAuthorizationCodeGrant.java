package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCICreateTokenEndpointRequestForPreAuthorizationCodeGrant extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "vci" })
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		String preAuthorizedCode = env.getString("vci", "pre-authorized_code");
		String txCodeValue = env.getString("vci", "pre-authorized_code_tx_code_value");

		JsonObject o = new JsonObject();
		o.addProperty("grant_type", "urn:ietf:params:oauth:grant-type:pre-authorized_code");
		o.addProperty("pre-authorized_code", preAuthorizedCode);
		o.addProperty("tx_code", txCodeValue);

		env.putObject("token_endpoint_request_form_parameters", o);

		// Remove headers as well, so that we're truly starting a 'new' request
		env.removeObject("token_endpoint_request_headers");

		logSuccess("Created token endpoint request with pre-authorized_code", o);

		return env;
	}

}
