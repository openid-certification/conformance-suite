package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDCCGenerateServerConfiguration extends GenerateServerConfiguration {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "server", strings = { "issuer", "discoveryUrl" })
	public Environment evaluate(Environment env) {
		//TODO add other options and missing values
		super.evaluate(env);
		JsonObject server = env.getObject("server");
		String baseUrl = env.getString("base_url");
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		server.addProperty("userinfo_endpoint", baseUrl + "userinfo");
		server.addProperty("registration_endpoint", baseUrl + "register");
		JsonArray scopes = new JsonArray();
		scopes.add("openid");
		server.add("scopes_supported", scopes);
		JsonArray responseTypes = new JsonArray();
		//response types are intentionally in unusual order
		responseTypes.add("code");
		responseTypes.add("id_token code");
		responseTypes.add("token code id_token");
		responseTypes.add("id_token");
		responseTypes.add("token id_token");
		responseTypes.add("token code");
		responseTypes.add("token");
		server.add("response_types_supported", responseTypes);

		JsonArray responseModes = new JsonArray();
		responseModes.add("query");
		responseModes.add("fragment");
		responseModes.add("form_post");
		server.add("response_modes_supported", responseModes);

		JsonArray clientAuthTypes = new JsonArray();
		clientAuthTypes.add("client_secret_basic");
		clientAuthTypes.add("client_secret_post");
		clientAuthTypes.add("client_secret_jwt");
		clientAuthTypes.add("private_key_jwt");
		server.add("token_endpoint_auth_methods_supported", clientAuthTypes);

		JsonArray grantTypes = new JsonArray();
		grantTypes.add("authorization_code");
		grantTypes.add("implicit");
		server.add("grant_types_supported", grantTypes);


		server.addProperty("request_parameter_supported", true);
		server.addProperty("request_uri_parameter_supported", true);


		// add this as the server configuration
		env.putObject("server", server);

		return env;
	}

}
