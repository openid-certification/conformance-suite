package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIValidateNonceEndpointInIssuerMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();

		boolean found = findCredentialConfigurationWithCryptoBindingMethodsSupported(metadata);

		if (!found) {
			// we did not find a credential configuration with cryptographic_binding_methods_supported,
			// so it's not a problem if the nonce_endpoint is not present

			if (metadata.has("nonce_endpoint")) {
				log("Found nonce_endpoint in credential_issuer_metadata", args("metadata", metadata));
			} else {
				log("Did not find nonce_endpoint in credential_issuer_metadata", args("metadata", metadata));
			}

			return env;
		}

		if (!metadata.has("nonce_endpoint")) {
			throw error("Could not find expected nonce_endpoint in credential_issuer_metadata", args("metadata", metadata));
		}

		logSuccess("Found expected nonce_endpoint in credential_issuer_metadata", args("nonce_endpoint", metadata.get("nonce_endpoint")));

		return env;
	}

	protected boolean findCredentialConfigurationWithCryptoBindingMethodsSupported(JsonObject metadata) {

		JsonObject credentialConfigurationsSupported = metadata.getAsJsonObject("credential_configurations_supported");
		for (String credentialKey : credentialConfigurationsSupported.keySet()) {
			JsonObject credentialConfiguration = credentialConfigurationsSupported.getAsJsonObject(credentialKey);
			if (credentialConfiguration.has("cryptographic_binding_methods_supported")) {
				return true;
			}
		}
		return false;
	}
}
