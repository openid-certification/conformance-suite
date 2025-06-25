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

		var credentialConfigScopeMapping = new HashMap<String, String>();
		JsonObject credentialConfigurationsSupported = env.getElementFromObject("vci", "credential_issuer_metadata.credential_configurations_supported").getAsJsonObject();
		for (String credentialConfigId : credentialConfigurationsSupported.keySet()) {
			JsonObject credentialConfiguration = credentialConfigurationsSupported.getAsJsonObject(credentialConfigId);

			if (!vciCredentialConfigurationId.equals(credentialConfigId)) {
				// skip other credential configuration ids
				continue;
			}

			JsonElement scopeEl = credentialConfiguration.get("scope");
			if (scopeEl == null) {
				credentialConfigScopeMapping.put(credentialConfigId, null);
			} else {
				String scopeValue = OIDFJSON.getString(scopeEl);
				credentialConfigScopeMapping.put(credentialConfigId, scopeValue);
			}
		}

		if (!credentialConfigScopeMapping.containsKey(vciCredentialConfigurationId)) {
			throw error("Couldn't find expected credential_configuration in credential issuer metadata",
				args("credential_configuration_id", vciCredentialConfigurationId,
					"credential_configurations_supported", credentialConfigurationsSupported));
		}

		String credentialConfigurationScope = credentialConfigScopeMapping.get(vciCredentialConfigurationId);
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
			logSuccess("Using credential scope value to reference credential_configuration_id", args("scope", scope, "credential_scope", credentialConfigurationScope, "credential_configuration_id", vciCredentialConfigurationId));
		} else {
			// if credential_configuration contains no scope -> use authorization_details to pass credential_configuration_id
			JsonObject credentialConfig = new JsonObject();
			credentialConfig.addProperty("type", "openid_credential");
			credentialConfig.addProperty("credential_configuration_id", vciCredentialConfigurationId);

			JsonObject rar = new JsonObject();
			rar.add("payload", OIDFJSON.convertJsonObjectListToJsonArray(List.of(credentialConfig)));
			env.putObject("rar", rar);

			logSuccess("Using authorization_details to refer to credential_configuration_id",
				args("rar_element", credentialConfig, "credential_configuration_id", vciCredentialConfigurationId));
		}

		return env;
	}
}
