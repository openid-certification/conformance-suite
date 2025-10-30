package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class VCIAddOpenIdCredentialToAuthorizationDetailsSupportedIfScopeIsMissing extends AbstractCondition {

	@Override
	@PreEnvironment(required = "credential_issuer_metadata")
	public Environment evaluate(Environment env) {

		JsonObject supportedCredentialConfigurations = env.getElementFromObject("credential_issuer_metadata","credential_configurations_supported").getAsJsonObject();

		boolean scopeMissing = false;
		for (var configurationId : supportedCredentialConfigurations.keySet()) {
			JsonObject credentialConfiguration = supportedCredentialConfigurations.getAsJsonObject(configurationId);
			if (!credentialConfiguration.has("scope")) {
				scopeMissing = true;
			}
		}

		if (scopeMissing) {
			// https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.4-2.11.2.2
			// If scope is absent, the only way to request the Credential is using authorization_details [RFC9396] - in this case, the OAuth Authorization Server metadata for one of the Authorization Servers found from the Credential Issuer's Metadata must contain an authorization_details_types_supported that contains openid_credential.
			JsonObject oauthServerMetadata = env.getObject("server");
			JsonArray authorizationDetailsTypesSupportedArray = oauthServerMetadata.getAsJsonArray("authorization_details_types_supported");

			if (authorizationDetailsTypesSupportedArray == null) {
				oauthServerMetadata.add("authorization_details_types_supported", OIDFJSON.convertListToJsonArray(List.of("openid_credential")));
				log("Added new authorization_details_types_supported attribute with openid_credential in oauth authorization server metadata");
			} else if (!OIDFJSON.convertJsonArrayToList(authorizationDetailsTypesSupportedArray).contains("openid_credential")) {
				authorizationDetailsTypesSupportedArray.add("openid_credential");
				oauthServerMetadata.add("authorization_details_types_supported", authorizationDetailsTypesSupportedArray);
				log("Added openid_credential to authorization_details_types_supported in oauth authorization server metadata");
			}
		} else {
			log("No need to add openid_credential to authorization_details_types_supported in oauth authorization server metadata as all credential configurations use a scope");
		}

		return env;
	}
}
