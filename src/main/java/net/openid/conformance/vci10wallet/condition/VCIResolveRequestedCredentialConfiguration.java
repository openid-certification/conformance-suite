package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIResolveRequestedCredentialConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required="config")
	@PostEnvironment(required = "vci_credential_configuration", strings = "vci_credential_configuration_id")
	public Environment evaluate(Environment env) {

		JsonElement credentialConfigurationsSupportedEl = env.getElementFromObject("vci", "credential_issuer_metadata.credential_configurations_supported");
		if (credentialConfigurationsSupportedEl == null) {
			throw error("credential_issuer_metadata.credential_configurations_supported missing");
		}

		String requestedCredentialConfigurationId = env.getString("config", "vci.credential_configuration_id");
		JsonObject credentialConfigurationsSupported = credentialConfigurationsSupportedEl.getAsJsonObject();

		if (!credentialConfigurationsSupported.has(requestedCredentialConfigurationId)) {
			throw error("requested credential_configuration with id " + requestedCredentialConfigurationId + " not found in credential_configurations_supported",
				args("requested_credential_configuration_id", requestedCredentialConfigurationId, "credential_configurations_supported", credentialConfigurationsSupported));
		}

		JsonObject credentialConfiguration = credentialConfigurationsSupported.getAsJsonObject(requestedCredentialConfigurationId);

		env.putString("vci_credential_configuration_id", requestedCredentialConfigurationId);
		env.putObject("vci_credential_configuration", credentialConfiguration);
		log("Emitting credential_configuration for " + requestedCredentialConfigurationId, args("credential_configuration", credentialConfiguration));

		return env;
	}
}
