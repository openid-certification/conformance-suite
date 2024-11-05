package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractOpenIDConnectRelyingPartyMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "federation_response_jwt")
	public Environment evaluate(Environment env) {

		JsonElement openidRelyingPartyMetadataElement = env.getElementFromObject("federation_response_jwt", "claims.metadata.openid_relying_party");
		if (openidRelyingPartyMetadataElement == null) {
			logSuccess("Entity statement does not contain the openid_relying_party metadata claim");
		} else {
			JsonObject openidRelyingPartyMetadata = openidRelyingPartyMetadataElement.getAsJsonObject();
			env.putObject("openid_relying_party_metadata", openidRelyingPartyMetadata);
			logSuccess("Extracted openid_relying_party metadata", args("openid_relying_party", openidRelyingPartyMetadata));
		}

		return env;
	}

}
