package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddSelfHostedTrustAnchorToConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "trust_anchor_entity_identifier")
	public Environment evaluate(Environment env) {

		JsonArray authorityHints;
		JsonElement authorityHintsElement = env.getElementFromObject("config", "federation.op_authority_hints");
		if (authorityHintsElement != null) {
			if (!authorityHintsElement.isJsonArray()) {
				throw error("authority_hints must be an array of strings");
			}
			authorityHints = authorityHintsElement.getAsJsonArray();
		} else {
			authorityHints = new JsonArray();
			env.putArray("config", "federation.op_authority_hints", authorityHints);
		}

		String trustAnchorEntityIdentifier = env.getString("trust_anchor_entity_identifier");
		if (!OIDFJSON.convertJsonArrayToList(authorityHints).contains(trustAnchorEntityIdentifier)) {
			authorityHints.add(trustAnchorEntityIdentifier);
		}

		logSuccess("Added self-hosted trust anchor to authority_hints", args("authority_hints", authorityHints));

		return env;
	}

}
