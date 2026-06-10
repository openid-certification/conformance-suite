package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks that the keys proven in the proofs of a (batch) credential request are pairwise
 * distinct, compared by RFC 7638 thumbprint.
 *
 * For SD-JWT credentials this is a MUST for the wallet (SD-JWT / RFC 9901 §10.1: "New Key
 * Binding keys and salts MUST be used for each credential in the batch to ensure that the
 * Verifiers cannot link the credentials using these values.") - call with FAILURE. For mdoc
 * only OID4VCI 1.0 Final §3.3.2's SHOULD on different Cryptographic Data applies - call
 * with WARNING.
 */
public class VCIEnsureProofKeysAreDistinct extends AbstractCondition {

	@Override
	@PreEnvironment(required = "proof_jwts")
	public Environment evaluate(Environment env) {

		JsonArray items = env.getObject("proof_jwts").getAsJsonArray("items");

		// thumbprint -> index of the proof it was first seen in
		Map<String, Integer> seen = new HashMap<>();
		for (int i = 0; i < items.size(); i++) {
			JsonElement jwkEl = items.get(i).getAsJsonObject().get("jwk");
			if (jwkEl == null || !jwkEl.isJsonObject()) {
				throw error("No resolved public key found for a proof - proof validation must run first",
					args("proof_index", i));
			}
			String thumbprint;
			try {
				thumbprint = JWK.parse(jwkEl.getAsJsonObject().toString()).computeThumbprint().toString();
			} catch (ParseException | JOSEException e) {
				throw error("Failed to parse the proof's public key as a JWK", e,
					args("proof_index", i, "jwk", jwkEl));
			}
			Integer firstIndex = seen.putIfAbsent(thumbprint, i);
			if (firstIndex != null) {
				throw error("Two proofs in the credential request prove possession of the same key; each "
						+ "credential in a batch must be bound to a fresh key so verifiers cannot link them",
					args("first_proof_index", firstIndex, "second_proof_index", i, "jwk", jwkEl));
			}
		}

		logSuccess("All " + items.size() + " proofs in the credential request use distinct keys");

		return env;
	}
}
