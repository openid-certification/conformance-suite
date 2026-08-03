package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.PreGeneratedJwks;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates one ephemeral P-256 signing key per credential to be requested in a batch
 * credential request: min(batch_size, MAX_REQUESTED_PROOFS) keys.
 *
 * The JWKS is stored separately from client_jwks (which is also used for client
 * authentication); the test module maps client_jwks to it around proof generation so
 * VCIGenerateJwtProof / VCIGenerateKeyAttestationIfNecessary pick up all keys.
 */
public class VCIPrepareBatchProofKeys extends AbstractCondition {

	public static final int MAX_REQUESTED_PROOFS = 20;

	@Override
	@PostEnvironment(required = {"vci_batch_proof_jwks", "vci_batch_proof_public_jwks"})
	public Environment evaluate(Environment env) {

		Integer batchSize = env.getInteger("vci_batch_size");
		if (batchSize == null || batchSize < 2) {
			throw error("'vci_batch_size' is missing or invalid - VCIValidateBatchCredentialIssuanceMetadata must run first",
				args("vci_batch_size", batchSize));
		}

		int requested = Math.min(batchSize, MAX_REQUESTED_PROOFS);

		List<JWK> keys = new ArrayList<>();
		for (int i = 0; i < requested; i++) {
			try {
				// Each iteration advances the per-test EC-P-256 counter, so the N
				// proof keys are distinct (required by the batch issuance flow:
				// keyIDFromThumbprint means distinct keys → distinct kids, which
				// the credential issuer uses to match proofs 1:1 to responses).
				keys.add(new ECKey.Builder(PreGeneratedJwks.nextEcKey(env, Curve.P_256))
					.algorithm(JWSAlgorithm.ES256)
					.keyUse(KeyUse.SIGNATURE)
					.keyIDFromThumbprint()
					.build());
			} catch (JOSEException e) {
				throw error("Failed to build EC key", e);
			}
		}

		JWKSet jwkSet = new JWKSet(keys);
		JsonObject jwks = JsonParser.parseString(jwkSet.toString(false)).getAsJsonObject();
		JsonObject publicJwks = JsonParser.parseString(jwkSet.toString(true)).getAsJsonObject();

		env.putInteger("vci_batch_requested_proof_count", requested);
		env.putObject("vci_batch_proof_jwks", jwks);
		env.putObject("vci_batch_proof_public_jwks", publicJwks);

		logSuccess("Generated " + requested + " proof signing keys for the batch credential request",
			args("requested_proof_count", requested,
				"issuer_batch_size", batchSize,
				"public_jwks", publicJwks));

		return env;
	}
}
