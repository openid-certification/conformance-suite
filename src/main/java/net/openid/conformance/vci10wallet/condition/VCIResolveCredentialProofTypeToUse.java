package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCIResolveCredentialProofTypeToUse extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "vci_credential_configuration"})
	@PostEnvironment(strings = "vci_proof_type_key", required = "vci_proof_type")
	public Environment evaluate(Environment env) {

		JsonObject credentialConfiguration = env.getObject("vci_credential_configuration");
		JsonObject proofTypesSupported = credentialConfiguration.getAsJsonObject("proof_types_supported");

		String proofTypeHint = env.getString("config", "vci.credential_proof_type_hint");

		String proofTypeKey;
		JsonObject proofType;
		if (proofTypeHint == null || proofTypeHint.isBlank()) {
			if (proofTypesSupported.keySet().size() == 1) {
				// only one proof type use the one that is present
				proofTypeKey = proofTypesSupported.keySet().iterator().next();
				proofType = proofTypesSupported.getAsJsonObject(proofTypeKey);
				log("Selected proof type to use " + proofTypeKey, args("proof_type", proofType));
			} else {
				// multiple proof types present, select the first one
				proofTypeKey = proofTypesSupported.keySet().iterator().next();
				proofType = proofTypesSupported.getAsJsonObject(proofTypeKey);
				log("Selected first proof type to use " + proofTypeKey, args("proof_type", proofType));
			}
		} else {

			if (proofTypesSupported.has(proofTypeHint)) {
				proofTypeKey = proofTypesSupported.get(proofTypeHint).toString();
				proofType = proofTypesSupported.getAsJsonObject(proofTypeKey);
				log("Selected explicitly requested proof type to use " + proofTypeKey, args("proof_type", proofType));
			} else {
				throw error("The requested proof type '" + proofTypeHint + "' was not found in proof_types_supported",
					args("proof_type_hint", proofTypeHint, "proof_types_supported", proofTypesSupported));
			}
		}

		if (proofTypeKey == null) {
			throw error("Could not determine the proof_type to use");
		}

		env.putString("vci_proof_type_key", proofTypeKey);
		env.putObject("vci_proof_type", proofType);

		return env;
	}
}
