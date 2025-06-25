package net.openid.conformance.vciid2wallet.condition;

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

				// use first openid_credential with configuration_id eu.europa.ec.eudi.pid.1
				if ("openid_credential".equals(OIDFJSON.getString(authDetail.get("type")))
						&& "eu.europa.ec.eudi.pid.1".equals(OIDFJSON.getString(authDetail.get("credential_configuration_id")))) {

					JsonArray credentialIdentifiers = new JsonArray();
					String credentialIdentifier = "eu.europa.ec.eudi.pid.1:" + UUID.randomUUID();
					credentialIdentifiers.add(credentialIdentifier);
					authDetail.add("credential_identifiers", credentialIdentifiers);

					log("Used credential_configuration from authorization_details",
							args("credential_configuration_id", credentialIdentifier, "credential_identifiers", credentialIdentifiers));

					return env;
				}
			}
		} else {

			String scope = env.getString("effective_authorization_endpoint_request", "scope");
			if (scope == null || scope.isBlank()) {
				throw error("Scope must not be empty if authorization_details are not present");
			}

			JsonObject credentialConfigurationIdScopeMap = env.getObject("credential_configuration_id_scope_map");
			if (credentialConfigurationIdScopeMap == null) {
				throw error("credential_configuration_id_scope_map object must not be null");
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
				String credentialIdentifier = "eu.europa.ec.eudi.pid.1:" + UUID.randomUUID();
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
