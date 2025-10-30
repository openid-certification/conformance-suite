package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Map;

public class VCIEnsureAuthorizationDetailsTypesSupportedContainOpenIdCredentialIfScopeIsMissing extends VCIAuthorizationServerMetadataValidation {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject credentialConfigurationsSupportedObj = env.getElementFromObject("vci", "credential_issuer_metadata.credential_configurations_supported").getAsJsonObject();

		boolean scopeMissing = false;
		for (Map.Entry<String, JsonElement> entry : credentialConfigurationsSupportedObj.asMap().entrySet()) {
			JsonElement value = entry.getValue();
			if (!value.getAsJsonObject().has("scope")) {
				scopeMissing = true;
				break;
			}
		}

		if (!scopeMissing) {
			log("scope is defined for all supported credential configurations, therefore there is no need for authorization_details_types_supported in oauth authorization server metadata");
			return env;
		}

		JsonObject oauthAuthorizationMetadata = getAuthorizationServerMetadata(env);
		if (!oauthAuthorizationMetadata.has("authorization_details_types_supported")) {
			throw error("authorization_details_types_supported attribute not found in oauth authorization server metadata");
		}

		List<String> authorizationDetailsTypesSupported = OIDFJSON.convertJsonArrayToList(oauthAuthorizationMetadata.getAsJsonArray("authorization_details_types_supported"));
		if (!authorizationDetailsTypesSupported.contains("openid_credential")) {
			throw error("Required 'openid_credential' value not found in authorization_details_types_supported attribute in oauth authorization server metadata", args("authorization_details_types_supported", authorizationDetailsTypesSupported));
		}

		logSuccess("Valid 'openid_credential' value found in authorization_details_types_supported attribute in oauth authorization server metadata", args("authorization_details_types_supported", authorizationDetailsTypesSupported));

		return env;
	}
}
