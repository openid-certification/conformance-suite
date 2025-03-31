package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddFederationEntityMetadataToTrustAnchorEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "trust_anchor", strings = "trust_anchor_entity_identifier")
	public Environment evaluate(Environment env) {

		String trustAnchorEntityIdentifier = env.getString("trust_anchor_entity_identifier");
		JsonObject trustAnchor = env.getObject("trust_anchor");

		JsonObject metadata = new JsonObject();

		JsonObject federationEntity = new JsonObject();
		metadata.add("federation_entity", federationEntity);

		JsonElement immediateSubordinatesElement = env.getElementFromObject("config", "federation_trust_anchor.immediate_subordinates");
		if (immediateSubordinatesElement != null) {
			if (!immediateSubordinatesElement.isJsonArray()) {
				throw error("immediate_subordinates must be an array of strings");
			}
			federationEntity.addProperty("federation_fetch_endpoint", trustAnchorEntityIdentifier + "/fetch");
			federationEntity.addProperty("federation_list_endpoint", trustAnchorEntityIdentifier + "/list");
			federationEntity.addProperty("federation_resolve_endpoint", trustAnchorEntityIdentifier + "/resolve");
		}

		trustAnchor.add("metadata", metadata);

		logSuccess("Added federation_entity metadata to trust anchor entity configuration", args("trust_anchor", trustAnchor));

		return env;
	}

}
