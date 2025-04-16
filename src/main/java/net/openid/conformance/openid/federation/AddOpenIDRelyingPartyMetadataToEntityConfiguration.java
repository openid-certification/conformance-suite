package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddOpenIDRelyingPartyMetadataToEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonObject metadata = server.getAsJsonObject("metadata");

		JsonObject openIdRelyingParty = new JsonObject();
		metadata.add("openid_relying_party", openIdRelyingParty);

		JsonArray responseTypes = new JsonArray();
		responseTypes.add("code");
		openIdRelyingParty.add("response_types", responseTypes);

		String entityIdentifier = env.getString("base_url");
		JsonArray redirectUris = new JsonArray();
		redirectUris.add(entityIdentifier + "/callback");
		openIdRelyingParty.add("redirect_uris", redirectUris);

		openIdRelyingParty.addProperty("jwks_uri", entityIdentifier + "/jwks");

		server.add("metadata", metadata);

		logSuccess("Added openid_relying_party metadata to server configuration", args("server", server));

		return env;
	}

}
