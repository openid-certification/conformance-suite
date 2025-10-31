package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;
import java.util.UUID;

public class VCIAddCredentialDataToAuthorizationDetailsForTokenEndpointResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		if (env.getObject("rich_authorization_request") == null) {
			// create rar if missing
			JsonObject richAuthorizationRequest = new JsonObject();
			richAuthorizationRequest.add("rar", new JsonArray());
			env.putObject("rich_authorization_request", richAuthorizationRequest);
		}

		JsonArray authDetails = getJsonArrayFromEnvironment(env, "rich_authorization_request", "rar", "authorization_details", false);
		if (authDetails == null) {
			log("No authorization details found");
			return env;
		}

		// TODO revise population of credentials in authorization_details
		if (!authDetails.isEmpty()) {
			for (var i = 0; i < authDetails.size(); i++) {
				JsonObject authDetail = authDetails.get(i).getAsJsonObject();

				// Process openid_credential authorization details
				boolean isOpenIdCredential = "openid_credential".equals(OIDFJSON.getString(authDetail.get("type")));
				if (!isOpenIdCredential) {
					continue;
				}
				String credentialConfigurationId = OIDFJSON.getString(authDetail.get("credential_configuration_id"));
				// Check for known credential configuration ID prefixes (SD-JWT PID, mdoc PID, mDL)
				if (credentialConfigurationId.startsWith("eu.europa.ec.eudi.pid.") ||
					credentialConfigurationId.startsWith("org.iso.18013.")) {

					JsonArray credentialIdentifiers = new JsonArray();
					String credentialIdentifier = credentialConfigurationId + ":" + UUID.randomUUID();
					credentialIdentifiers.add(credentialIdentifier);
					authDetail.add("credential_identifiers", credentialIdentifiers);

					log("Used credential_configuration from authorization_details",
							args("credential_configuration_id", credentialConfigurationId, "credential_identifiers", credentialIdentifiers));

					return env;
				}
			}
		} else {

			String scope = env.getString("effective_authorization_endpoint_request", "scope");
			if (scope == null || scope.isBlank()) {
				// throw error("Scope must not be empty if authorization_details are not present");
				// default a non-present scope to our credential
				scope = "eudi.pid.1";
				log("No scope was present in effective_authorization_endpoint_request, assuming default scope " + scope, args("scope", scope));
			}

			JsonObject credentialConfigurationIdScopeMap = env.getObject("credential_configuration_id_scope_map");
			if (credentialConfigurationIdScopeMap == null) {
				throw error("credential_configuration_id_scope_map object must not be null");
			}

			if (credentialConfigurationIdScopeMap.isEmpty()) {
				// assume default scope mappings for example credential
				// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-6.2-4.1.1
				credentialConfigurationIdScopeMap.addProperty("eudi.pid.1", "eu.europa.ec.eudi.pid.1");
			}

			for (String credentialConfigurationScopeCandidate : Set.of(scope.split(" "))) {
				JsonElement maybeCredentialConfigurationId = credentialConfigurationIdScopeMap.get(credentialConfigurationScopeCandidate);
				if (maybeCredentialConfigurationId == null) {
					continue;
				}

				String credentialConfigurationId = OIDFJSON.getString(maybeCredentialConfigurationId);

				JsonObject authDetail = new JsonObject();
				authDetail.addProperty("type", "openid_credential");
				authDetail.addProperty("credential_configuration_id", credentialConfigurationId);

				JsonArray credentialIdentifiers = new JsonArray();
				String credentialIdentifier = credentialConfigurationId + ":" + UUID.randomUUID();
				credentialIdentifiers.add(credentialIdentifier);
				authDetail.add("credential_identifiers", credentialIdentifiers);

				authDetails.add(authDetail);

				log("Used credential_configuration from scope",
						args("credential_configuration_id", credentialIdentifier, "credential_identifiers", credentialIdentifiers, "credential_scope", credentialConfigurationScopeCandidate));

			}
		}

		return env;
	}
}
