package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddFederationEntityToTrustAnchorImmediateSubordinates extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "trust_anchor_entity_identifier")
	public Environment evaluate(Environment env) {

		JsonArray immediateSubordinates;
		JsonElement immediateSubordinatesElement = env.getElementFromObject("config", "federation_trust_anchor.immediate_subordinates");
		if (immediateSubordinatesElement != null) {
			immediateSubordinates = immediateSubordinatesElement.getAsJsonArray();
		} else {
			immediateSubordinates = new JsonArray();
		}

		String entityIdentifier = env.getString("config", "federation.entity_identifier");
		if (!OIDFJSON.convertJsonArrayToList(immediateSubordinates).contains(entityIdentifier)) {
			immediateSubordinates.add(entityIdentifier);
		}

		env.putArray("config", "federation_trust_anchor.immediate_subordinates", immediateSubordinates);

		logSuccess("Added entity to self-hosted trust anchor's list of immediate subordinates", args("immediate_subordinates", immediateSubordinates));

		return env;
	}

}
