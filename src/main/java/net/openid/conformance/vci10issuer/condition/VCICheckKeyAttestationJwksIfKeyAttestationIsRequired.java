package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCICheckKeyAttestationJwksIfKeyAttestationIsRequired extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "vci_proof_type"}, strings = "vci_proof_type_key")
	public Environment evaluate(Environment env) {

		String proofTypeKey = env.getString("vci_proof_type_key");
		JsonObject proofType = env.getObject("vci_proof_type");
		if (!proofType.has("key_attestations_required")) {
			log("Skip checking Key Attestation JWKS: Key attestation not required for selected proof type " + proofTypeKey,
				args("proof_type", proofType));
			return env;
		}

		log("Key attestation required for selected proof type " + proofTypeKey, args("proof_type", proofType));
		if (env.getElementFromObject("config", "vci.key_attestation_jwks") == null) {
			throw error("Required key_attestation_jwks is missing", args("proof_type", proofType));
		}

		JsonObject keyAttestationJwksObj = env.getElementFromObject("config", "vci.key_attestation_jwks").getAsJsonObject();
		env.putObject("vci_key_attestation_jwks", keyAttestationJwksObj);
		log("Successfully parsed key attestation jwks", args("key_attestation_jwks", keyAttestationJwksObj));

		return env;
	}
}
