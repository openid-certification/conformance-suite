package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateOAuthAuthorizationServerMetadata extends AbstractValidateMetadata {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		String metadataName = "oauth_authorization_server";
		JsonElement metadataElement = env.getElementFromObject("federation_response_jwt", "claims.metadata." + metadataName);

		if (metadataElement == null) {
			logSuccess(String.format("Entity statement does not contain the %s metadata claim", metadataName));
			return env;
		}

		if (!metadataElement.isJsonObject()) {
			throw error(String.format("%s metadata must be a JSON object", metadataName), args(metadataName, metadataElement));
		}

		logSuccess(String.format("Entity statement contains valid %s metadata claim", metadataName), args("metadata", metadataElement.getAsJsonObject()));
		return env;
	}
}
