package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddFederationEntityMetadataToEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = {"base_url"})
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");
		JsonObject server = env.getObject("server");

		JsonObject metadata = new JsonObject();

		JsonObject federationEntity = new JsonObject();
		metadata.add("federation_entity", federationEntity);

		JsonElement immediateSubordinatesElement = env.getElementFromObject("config", "federation.immediate_subordinates");
		if (immediateSubordinatesElement != null) {
			if (!immediateSubordinatesElement.isJsonArray()) {
				throw error("immediate_subordinates must be an array of strings");
			}
			federationEntity.addProperty("federation_fetch_endpoint", baseUrl + "/fetch");
			federationEntity.addProperty("federation_list_endpoint", baseUrl + "/list");
		}

		server.add("metadata", metadata);

		logSuccess("Added federation_entity metadata to server configuration", args("server", server));

		return env;
	}

}
