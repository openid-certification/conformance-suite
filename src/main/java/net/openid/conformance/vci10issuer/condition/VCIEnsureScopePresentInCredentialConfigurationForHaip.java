package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Validates that the selected credential configuration includes a scope value.
 *
 * Per HAIP Section 4.1:
 *   "The Credential Issuer MUST include a scope for every Credential Configuration"
 *
 * Per HAIP Section 4.3:
 *   "The Wallet MUST use scope parameter to request issuance of Credentials"
 *
 * If the credential configuration does not include a scope, the test falls back to
 * authorization_details silently, which is not valid for HAIP.
 *
 * @see <a href="https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html#section-4.1">HAIP Section 4.1</a>
 * @see <a href="https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html#section-4.3">HAIP Section 4.3</a>
 */
public class VCIEnsureScopePresentInCredentialConfigurationForHaip extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"vci"})
	public Environment evaluate(Environment env) {

		String vciCredentialConfigurationId = env.getString("vci_credential_configuration_id");

		JsonObject credentialConfigurationsSupported = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_configurations_supported").getAsJsonObject();

		JsonObject credentialConfiguration = credentialConfigurationsSupported.getAsJsonObject(vciCredentialConfigurationId);
		if (credentialConfiguration == null) {
			throw error("Could not find credential configuration in issuer metadata",
				args("credential_configuration_id", vciCredentialConfigurationId));
		}

		JsonElement scopeEl = credentialConfiguration.get("scope");
		if (scopeEl == null) {
			throw error("Credential configuration does not include a 'scope' value. " +
				"HAIP Section 4.1 requires the Credential Issuer MUST include a scope for every Credential Configuration, " +
				"and HAIP Section 4.3 requires the Wallet MUST use the scope parameter to request issuance.",
				args("credential_configuration_id", vciCredentialConfigurationId,
					"credential_configuration", credentialConfiguration));
		}

		logSuccess("Credential configuration includes a 'scope' value as required by HAIP",
			args("credential_configuration_id", vciCredentialConfigurationId, "scope", scopeEl));

		return env;
	}
}
