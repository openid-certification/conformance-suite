package net.openid.conformance.openid.federation;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class ExtractFederationEntityMetadataUrls extends AbstractValidateMetadata {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		String metadataName = "";
		JsonElement metadataElement = env.getElementFromObject("federation_response_jwt", "claims.metadata.federation_entity");

		if (metadataElement == null) {
			logSuccess(String.format("Entity statement does not contain the %s metadata claim", metadataName));
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
			if (metadata.has(key)) {
				env.putString(key, OIDFJSON.getString(metadata.get(key)));
			}
		}

		logSuccess("Extracted URLs from federation_entity metadata", args("metadata", metadata));
		return env;
	}
}
