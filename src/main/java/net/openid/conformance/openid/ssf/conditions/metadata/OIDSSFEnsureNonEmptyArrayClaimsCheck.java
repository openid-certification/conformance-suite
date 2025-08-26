package net.openid.conformance.openid.ssf.conditions.metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashSet;
import java.util.Set;

public class OIDSSFEnsureNonEmptyArrayClaimsCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonObject transmitterMetadata = env.getElementFromObject("ssf", "transmitter_metadata").getAsJsonObject();

		Set<String> emptyArrayClaims = new HashSet<>();
		for (var claim : transmitterMetadata.keySet()) {
			JsonElement claimValueEl = transmitterMetadata.get(claim);
			if (claimValueEl.isJsonArray() && claimValueEl.getAsJsonArray().isEmpty()) {
				emptyArrayClaims.add(claim);
			}
		}

		if (!emptyArrayClaims.isEmpty()) {
			throw error("Found empty array claim values in transmitter metadata", args("claims_with_empty_arrays", emptyArrayClaims));
		}

		logSuccess("Found no empty array claim values in transmitter metadata", args("claim", transmitterMetadata));

		return env;
	}
}
