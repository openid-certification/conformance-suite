package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddAuthorityHintsForRP extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonArray authorityHints;
		JsonElement authorityHintsElement = env.getElementFromObject("config", "federation.authority_hints");
		if (authorityHintsElement != null) {
			if (!authorityHintsElement.isJsonArray()) {
				throw error("authority_hints must be an array of strings");
			}
			authorityHints = authorityHintsElement.getAsJsonArray();
		} else {
			authorityHints = new JsonArray();
			env.putArray("config", "federation.authority_hints", authorityHints);
		}

		String rpAuthorityHint = env.getString("config", "federation.rp_authority_hint");
		if (rpAuthorityHint != null && !OIDFJSON.convertJsonArrayToList(authorityHints).contains(rpAuthorityHint)) {
			authorityHints.add(rpAuthorityHint);
		}

		logSuccess("Added authority hint", args("authority_hints", authorityHints));

		return env;
	}

}
