package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public class ValidateFederationEntityMetadata extends AbstractValidateMetadata {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		String metadataName = "federation_entity";
		JsonElement metadataElement = env.getElementFromObject("federation_response_jwt", "claims.metadata." + metadataName);

		if (metadataElement == null) {
			logSuccess(String.format("Entity statement does not contain the %s metadata claim", metadataName));
			return env;
		}

		JsonObject metadata = metadataElement.getAsJsonObject();

		Set<String> federationEntityUrlKeys = EntityUtils.STANDARD_FEDERATION_ENTITY_URL_KEYS;
		for (String key : federationEntityUrlKeys) {
			boolean isOptional = true;
			if(!validateUrl(metadata, key, isOptional)) {
				throw error("This URL MUST use the https scheme and MAY contain port, path, " +
					"and query parameter components encoded in application/x-www-form-urlencoded format; " +
					"it MUST NOT contain a fragment component.", args("metadata", metadata));
			}
		}

		logSuccess(String.format("Entity statement contains valid %s metadata claim", metadataName), args("metadata", metadata));
		return env;
	}
}
