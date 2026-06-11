package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base for conditions checking that the key an issued credential is bound to is one of the
 * keys whose possession the wallet demonstrated via the proofs parameter of the credential
 * request (OID4VCI 1.0 Final §8.3: the credentials match "the number of keys that the
 * Wallet has provided via the proofs parameter", each key binding at most one credential).
 *
 * The sent keys are recovered from the 'credential_request_proofs' environment object: for
 * 'jwt' proofs from the jwk in each proof's JOSE header, for 'attestation' proofs from the
 * attested_keys claim of the key attestation JWT. Keys are compared by RFC 7638 thumbprint
 * so extra members like kid or alg do not affect the comparison.
 */
public abstract class AbstractVCIEnsureBindingKeyMatchesProofKey extends AbstractVCIBatchBindingKeyCheck {

	protected Set<String> getSentProofKeyThumbprints(Environment env) {
		JsonObject proofs = env.getObject("credential_request_proofs");

		Set<String> thumbprints = new HashSet<>();

		JsonElement jwtProofsEl = proofs.get("jwt");
		if (jwtProofsEl != null && jwtProofsEl.isJsonArray()) {
			for (JsonElement proofEl : jwtProofsEl.getAsJsonArray()) {
				String proofJwt = OIDFJSON.getString(proofEl);
				try {
					JWK jwk = SignedJWT.parse(proofJwt).getHeader().getJWK();
					if (jwk == null) {
						throw error("JWT proof sent in the credential request does not contain a 'jwk' JOSE header",
							args("proof", proofJwt));
					}
					thumbprints.add(jwk.computeThumbprint().toString());
				} catch (ParseException | JOSEException e) {
					throw error("Failed to parse a JWT proof sent in the credential request", e,
						args("proof", proofJwt));
				}
			}
		}

		JsonElement attestationProofsEl = proofs.get("attestation");
		if (attestationProofsEl != null && attestationProofsEl.isJsonArray()) {
			for (JsonElement attestationEl : attestationProofsEl.getAsJsonArray()) {
				String attestationJwt = OIDFJSON.getString(attestationEl);
				try {
					List<?> attestedKeys = SignedJWT.parse(attestationJwt)
						.getJWTClaimsSet().getListClaim("attested_keys");
					if (attestedKeys == null) {
						throw error("Key attestation sent in the credential request does not contain 'attested_keys'",
							args("attestation", attestationJwt));
					}
					for (Object attestedKey : attestedKeys) {
						if (!(attestedKey instanceof Map)) {
							throw error("'attested_keys' entry in the sent key attestation is not a JWK object",
								args("attestation", attestationJwt));
						}
						@SuppressWarnings("unchecked")
						Map<String, Object> jwkMap = (Map<String, Object>) attestedKey;
						thumbprints.add(JWK.parse(jwkMap).computeThumbprint().toString());
					}
				} catch (ParseException | JOSEException e) {
					throw error("Failed to parse the key attestation sent in the credential request", e,
						args("attestation", attestationJwt));
				}
			}
		}

		if (thumbprints.isEmpty()) {
			throw error("No proof keys found in the proofs sent in the credential request",
				args("credential_request_proofs", proofs));
		}

		return thumbprints;
	}

	protected void checkBindingKeyWasSent(Environment env, JsonElement bindingJwk, String bindingKeyDescription) {
		String thumbprint = computeThumbprint(bindingJwk, bindingKeyDescription);

		if (!getSentProofKeyThumbprints(env).contains(thumbprint)) {
			throw error("The issued credential is bound to a key that is not one of the keys the proofs in the "
					+ "credential request demonstrated possession of",
				args("binding_key", bindingJwk,
					"credential_request_proofs", env.getObject("credential_request_proofs")));
		}

		logSuccess("The issued credential's " + bindingKeyDescription
				+ " is one of the proof keys sent in the credential request",
			args("binding_key", bindingJwk));
	}
}
