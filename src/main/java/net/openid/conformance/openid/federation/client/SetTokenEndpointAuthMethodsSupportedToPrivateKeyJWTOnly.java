package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetTokenEndpointAuthMethodsSupportedToPrivateKeyJWTOnly extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("private_key_jwt");

		JsonObject openIdProvider = env.getElementFromObject("server", "metadata.openid_provider").getAsJsonObject();
		openIdProvider.add("token_endpoint_auth_methods_supported", data);

		log("Set token_endpoint_auth_methods_supported to private_key_jwt only in openid_provider metadata configuration",
			args("openid_provider", openIdProvider));

		return env;
	}
}
