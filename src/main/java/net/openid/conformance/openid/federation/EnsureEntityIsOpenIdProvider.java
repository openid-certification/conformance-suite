package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;

public class EnsureEntityIsOpenIdProvider extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "primary_entity_statement_jwt")
	public Environment evaluate(Environment env) {

		JsonElement metadataElement = env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata");
		if (metadataElement == null) {
			throw error("Entity statement does not contain a metadata claim");
		}

		JsonObject metadata = metadataElement.getAsJsonObject();
		if (!metadata.has("openid_provider")) {
			throw error("Entity statement does not contain openid_provider metadata");
		}

		JsonObject openidProvider = metadata.getAsJsonObject("openid_provider");
		if (!openidProvider.has("authorization_endpoint")) {
			throw error("Entity statement does not contain openid_provider.authorization_endpoint");
		}

		logSuccess("Entity statement contains openid_provider metadata", args("openid_provider", openidProvider));
		return env;
	}

}
