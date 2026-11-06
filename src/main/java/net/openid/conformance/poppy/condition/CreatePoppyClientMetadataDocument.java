package net.openid.conformance.poppy.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreatePoppyClientMetadataDocument extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client", strings = "base_url")
	@PostEnvironment(required = {"poppy_client_metadata_document", "client"}, strings = {"client_id", "redirect_uri"})
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");
		while (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}

		String clientId = baseUrl + "/agent.json";
		String jwksUri = baseUrl + "/jwks.json";
		String redirectUri = baseUrl + "/callback";

		JsonArray redirectUris = new JsonArray();
		redirectUris.add(redirectUri);

		JsonObject doc = new JsonObject();
		doc.addProperty("client_id", clientId);
		doc.addProperty("client_name", "OpenID Foundation conformance suite");
		doc.add("redirect_uris", redirectUris);
		doc.addProperty("token_endpoint_auth_method", "private_key_jwt");
		doc.addProperty("jwks_uri", jwksUri);

		env.putObject("poppy_client_metadata_document", doc);
		env.putString("client_id", clientId);
		env.putString("redirect_uri", redirectUri);

		JsonObject client = env.getObject("client");
		client.addProperty("client_id", clientId);
		client.add("redirect_uris", redirectUris.deepCopy());
		client.addProperty("jwks_uri", jwksUri);

		logSuccess("Created the client metadata document for the Personal Agent", args("poppy_client_metadata_document", doc));
		return env;
	}
}
