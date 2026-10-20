package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

public class VCICheckKeyAttestationJwksIfKeyAttestationIsRequired extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		Boolean requiresCryptographicBinding = env.getBoolean("vci_requires_cryptographic_binding");
		if (requiresCryptographicBinding == null || !requiresCryptographicBinding) {
			log("Skip checking Key Attestation JWKS: Cryptographic binding not required");
			return env;
		}

		String proofTypeKey = env.getString("vci_proof_type_key");
		JsonObject proofType = env.getObject("vci_proof_type");
		if (proofType == null || !(proofType.has("key_attestations_required") || "attestation".equals(proofTypeKey))) {
			log("Skip checking Key Attestation JWKS: Key attestation not required for selected proof type " + proofTypeKey,
				args("proof_type", proofType));
			return env;
		}

		log("Key attestation required for selected proof type " + proofTypeKey, args("proof_type", proofType));
		// Read the new key first; fall back to the legacy vci.* key so existing stored
		// test configs keep working through a transition window.
		JsonObject keyAttestationJwksObj = env.getElementFromObject("config", "client_attestation.key_attestation_jwks") != null
			? env.getElementFromObject("config", "client_attestation.key_attestation_jwks").getAsJsonObject()
			: (env.getElementFromObject("config", "vci.key_attestation_jwks") != null
				? env.getElementFromObject("config", "vci.key_attestation_jwks").getAsJsonObject()
				: null);
		if (keyAttestationJwksObj == null) {
			// the built-in wallet provider certificate; its CA is published at /trust-anchors
			keyAttestationJwksObj = TestKeysAndCerts.getWalletAttestationSigner().jwksJson();
			env.putObject("vci_key_attestation_jwks", keyAttestationJwksObj);
			log("No 'Key Attestation JWKS' in the 'Client Attestation' section of the test configuration; signing key"
					+ " attestations with the suite's built-in wallet provider certificate",
				args("attester_certificate_subject", TestKeysAndCerts.getWalletAttestationSigner().getCert().getSubject().getName(),
					"wallet_provider_ca_url", TestKeysAndCerts.SuiteCa.WALLET_PROVIDER.getTrustAnchorUrl()));
			return env;
		}

		env.putObject("vci_key_attestation_jwks", keyAttestationJwksObj);
		log("Successfully parsed key attestation jwks", args("key_attestation_jwks", keyAttestationJwksObj));

		return env;
	}
}
