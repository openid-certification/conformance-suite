package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractOpenIDProviderMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "federation_response_jwt")
	public Environment evaluate(Environment env) {

		JsonElement openidRelyingPartyMetadataElement = env.getElementFromObject("federation_response_jwt", "claims.metadata.openid_provider");
		if (openidRelyingPartyMetadataElement == null) {
			logSuccess("Entity statement does not contain the openid_provider metadata claim");
		} else {
			JsonObject openidRelyingPartyMetadata = openidRelyingPartyMetadataElement.getAsJsonObject();
			env.putObject("openid_provider_metadata", openidRelyingPartyMetadata);
			logSuccess("Extracted openid_provider metadata", args("openid_provider", openidRelyingPartyMetadata));
		}

		return env;
	}

}
