package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class VCIDetermineCredentialConfigurationTransferMethod extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "vci"})
	public Environment evaluate(Environment env) {

		// to emit credential_configuration_id
		// we ensure credential_configuration_id is present
		// by checking the issuer metadata

		String vciCredentialConfigurationId = env.getString("config", "vci.credential_configuration_id");
		if (vciCredentialConfigurationId == null || vciCredentialConfigurationId.isBlank()) {
			throw error("Missing credential_configuration_id not provided");
		}

		log("Found explicitly configured credential_configuration_id", args("credential_configuration_id", vciCredentialConfigurationId));

		Map<String, String> credentialConfigurationScopes = new HashMap<>();
		JsonObject credentialConfigurationsSupported = env.getElementFromObject("vci", "credential_issuer_metadata.credential_configurations_supported").getAsJsonObject();
		for (String credentialConfigurationId : credentialConfigurationsSupported.keySet()) {
			JsonObject credentialConfiguration = credentialConfigurationsSupported.getAsJsonObject(credentialConfigurationId);

			if (!vciCredentialConfigurationId.equals(credentialConfigurationId)) {
				// skip other credential configuration ids
				continue;
			}

			JsonElement scopeEl = credentialConfiguration.get("scope");
			if (scopeEl == null) {
				credentialConfigurationScopes.put(credentialConfigurationId, null);
			} else {
				String scopeValue = OIDFJSON.getString(scopeEl);
				credentialConfigurationScopes.put(credentialConfigurationId, scopeValue);
			}
		}

		if (!credentialConfigurationScopes.containsKey(vciCredentialConfigurationId)) {
			throw error("Couldn't find expected credential_configuration in credential issuer metadata",
				args("credential_configuration_id", vciCredentialConfigurationId,
					"credential_configurations_supported", credentialConfigurationsSupported));
		}

		String credentialConfigurationScope = credentialConfigurationScopes.get(vciCredentialConfigurationId);
		if (credentialConfigurationScope != null) {
			// if credential_configuration contains scope -> use scope to pass instead of the credential_configuration_id
			String scope = env.getString("config", "client.scope");
			if (scope == null) {
				scope = credentialConfigurationScope;
			} else {
				scope = scope + " " + credentialConfigurationScope;
			}

			// deduplicate scopes if necessary
			scope = String.join(" ", new LinkedHashSet<>(List.of(scope.split(" "))));

			env.putString("config", "client.scope", scope);
			logSuccess("Added scope value for credential_configuration_id to request scope", args("scope", scope, "credential_scope", credentialConfigurationScope, "credential_configuration_id", vciCredentialConfigurationId));
		} else {
			// if credential_configuration contains no scope -> use authorization_details to pass credential_configuration_id
			JsonObject resourceObject = env.getElementFromObject("config", "resource").getAsJsonObject();
			JsonObject credentialConfigurationObject = new JsonObject();
			credentialConfigurationObject.addProperty("type", "openid_credential");
			credentialConfigurationObject.addProperty("credential_configuration_id", vciCredentialConfigurationId);

			resourceObject.add("richAuthorizationRequest", OIDFJSON.convertJsonObjectListToJsonArray(List.of(credentialConfigurationObject)));

			logSuccess("Added element for credential_configuration_id to request authorization_details", args("rar_element", credentialConfigurationObject, "credential_configuration_id", vciCredentialConfigurationId));
		}

		return env;
	}
}
