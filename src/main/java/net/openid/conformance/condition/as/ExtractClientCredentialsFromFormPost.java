package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractClientCredentialsFromFormPost extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = "client_authentication")
	public Environment evaluate(Environment env) {

		if (env.containsObject("client_authentication")) {
			throw error("Found existing client authentication");
		}

		String clientId = env.getString("token_endpoint_request", "body_form_params.client_id");
		String clientSecret = env.getString("token_endpoint_request", "body_form_params.client_secret");

		if (Strings.isNullOrEmpty(clientId) || Strings.isNullOrEmpty(clientSecret)) {
			throw error("Couldn't find client credentials in form post");
		}

		JsonObject clientAuthentication = new JsonObject();
		clientAuthentication.addProperty("client_id", clientId);
		clientAuthentication.addProperty("client_secret", clientSecret);
		clientAuthentication.addProperty("method", "client_secret_post");

		env.putObject("client_authentication", clientAuthentication);

		logSuccess("Extracted client authentication", clientAuthentication);

		return env;

	}

}
