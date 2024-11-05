package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class ValidateAbsenceOfFederationEntityMetadata extends AbstractValidateMetadata {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		String metadataName = "federation_entity";
		JsonElement metadataElement = env.getElementFromObject("federation_response_jwt", "claims.metadata." + metadataName);

		if (metadataElement == null) {
			logSuccess(String.format("Subordinate statement does not contain the %s metadata claim", metadataName));
			return env;
		}

		JsonObject metadata = metadataElement.getAsJsonObject();

		Set<String> federationEntityUrlKeys = ImmutableSet.of(
			"federation_fetch_endpoint",
			"federation_list_endpoint",
			"federation_resolve_endpoint",
			"federation_trust_mark_status_endpoint",
			"federation_trust_mark_list_endpoint",
			"federation_trust_mark_endpoint",
			"federation_historical_keys_endpoint"
		);
		for (String key : federationEntityUrlKeys) {
			if (metadata.has(key) && metadata.get(key) != null && !metadata.get(key).isJsonNull()) {
				throw error("Subordinate statement must not contain federation_entity endpoint URLs", args("metadata", metadata));
			}
		}

		logSuccess("Subordinate statement does not contain federation_entity endpoint URLs");
		return env;
	}
}
