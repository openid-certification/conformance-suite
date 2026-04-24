package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIResolveCredentialProofTypeToUse extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "vci_credential_configuration"})
	public Environment evaluate(Environment env) {

		JsonObject credentialConfiguration = env.getObject("vci_credential_configuration");

		// Check if the credential configuration requires cryptographic binding
		if (!credentialConfiguration.has("cryptographic_binding_methods_supported") ||
			!credentialConfiguration.has("proof_types_supported")) {
			// No cryptographic binding required, no proof needed
			log("Credential configuration does not require cryptographic binding, skipping proof type resolution",
				args("credential_configuration", credentialConfiguration));
			env.putBoolean("vci_requires_cryptographic_binding", false);
			return env;
		}

		env.putBoolean("vci_requires_cryptographic_binding", true);

		JsonObject proofTypesSupported = credentialConfiguration.getAsJsonObject("proof_types_supported");

		if (proofTypesSupported.keySet().isEmpty()) {
			throw error("proof_types_supported is present but empty; no proof type can be selected",
				args("proof_types_supported", proofTypesSupported));
		}

		// Test modules can declare an ordered preference list of proof types they need via
		// AbstractVCIIssuerTestModule.getRequiredProofTypes(); it is encoded into env as a
		// comma-separated string. This takes precedence over the config hint and the
		// first-available fallback, but only when at least one of the requested types is
		// actually advertised — otherwise we fall through so the test's own start() can
		// fire testSkipped with a clear reason.
		String requiredProofTypesList = env.getString("vci_required_proof_types");
		String proofTypeHint = env.getString("config", "vci.credential_proof_type_hint");

		String proofTypeKey = null;
		if (requiredProofTypesList != null && !requiredProofTypesList.isBlank()) {
			for (String candidate : requiredProofTypesList.split(",")) {
				String trimmed = candidate.trim();
				if (!trimmed.isEmpty() && proofTypesSupported.has(trimmed)) {
					proofTypeKey = trimmed;
					log("Selected test-required proof type " + proofTypeKey,
						args("proof_type", proofTypesSupported.getAsJsonObject(proofTypeKey),
							"required_proof_types", requiredProofTypesList));
					break;
				}
			}
		}

		if (proofTypeKey == null) {
			if (proofTypeHint != null && !proofTypeHint.isBlank()) {
				if (!proofTypesSupported.has(proofTypeHint)) {
					throw error("The requested proof type '" + proofTypeHint + "' was not found in proof_types_supported",
						args("proof_type_hint", proofTypeHint, "proof_types_supported", proofTypesSupported));
				}
				proofTypeKey = proofTypeHint;
				log("Selected explicitly requested proof type to use " + proofTypeKey,
					args("proof_type", proofTypesSupported.getAsJsonObject(proofTypeKey)));
			} else {
				proofTypeKey = proofTypesSupported.keySet().iterator().next();
				log("Selected first available proof type to use " + proofTypeKey,
					args("proof_type", proofTypesSupported.getAsJsonObject(proofTypeKey)));
			}
		}

		env.putString("vci_proof_type_key", proofTypeKey);
		env.putObject("vci_proof_type", proofTypesSupported.getAsJsonObject(proofTypeKey));

		return env;
	}
}
