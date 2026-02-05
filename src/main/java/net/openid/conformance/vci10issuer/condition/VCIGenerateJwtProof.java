package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VCIGenerateJwtProof extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_jwks")
	@PostEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		String proofTypeKey = env.getString("vci_proof_type_key");
		if (!"jwt".equals(proofTypeKey)) {
			log("Skip generating JWT for proof type " + proofTypeKey, args(proofTypeKey));
			return env;
		}

		String serverIssuer = getIssuer(env);

		try {
			JWKSet jwkSet = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
			String walletIssuerId = env.getString("client", "client_id");
			String cNonce = getCNonce(env);
			JsonObject proofType = env.getObject("vci_proof_type");

			// Get key attestation if required (same attestation used for all proofs)
			String keyAttestationJwt = null;
			if (proofType.has("key_attestations_required")) {
				keyAttestationJwt = env.getString("key_attestation_jwt");
			}

			// Collect all signing keys
			List<JWK> signingKeys = new ArrayList<>();
			for (JWK jwk : jwkSet.getKeys()) {
				var use = jwk.getKeyUse();
				if (use != null && !use.equals(KeyUse.SIGNATURE)) {
					// skip encryption keys
					continue;
				}
				signingKeys.add(jwk);
			}

			if (signingKeys.isEmpty()) {
				throw error("No signing keys found in client_jwks");
			}

			// Generate a JWT proof for each signing key
			List<String> jwtProofs = new ArrayList<>();
			JsonArray verifiableArray = new JsonArray();

			for (JWK jwk : signingKeys) {
				ECKey ecKey = ECKey.parse(jwk.toJSONString());

				// Ensure the key uses the correct curve
				if (!Curve.P_256.equals(ecKey.getCurve())) {
					throw error("Private key does not use the required P-256 curve for ES256",
						args("kid", jwk.getKeyID(), "curve", ecKey.getCurve()));
				}

				JWSSigner signer = new ECDSASigner(ecKey);

				JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256)
					.type(new JOSEObjectType("openid4vci-proof+jwt"));

				headerBuilder.jwk(jwk.toPublicJWK());

				// add key attestation if necessary (same for all proofs)
				if (keyAttestationJwt != null) {
					headerBuilder.customParam("key_attestation", keyAttestationJwt);
				}

				JWSHeader header = headerBuilder.build();

				Instant now = Instant.now();
				Date issueTime = Date.from(now);

				JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
					.issuer(walletIssuerId)
					.audience(serverIssuer)
					.issueTime(issueTime)
					.claim("nonce", cNonce)
					.build();

				SignedJWT signedJWT = new SignedJWT(header, claimsSet);
				signedJWT.sign(signer);

				String jwtProof = signedJWT.serialize();
				jwtProofs.add(jwtProof);

				JsonObject verifiableObj = new JsonObject();
				verifiableObj.addProperty("verifiable_jws", jwtProof);
				verifiableObj.addProperty("public_jwk", jwk.toPublicJWK().toString());
				verifiableObj.addProperty("kid", jwk.getKeyID());
				verifiableArray.add(verifiableObj);
			}

			// Store the first proof for backward compatibility
			env.putString("vci", "proof.jwt", jwtProofs.get(0));

			JsonObject proofsObject = createProofsObject(jwtProofs);
			env.putObject("credential_request_proofs", proofsObject);

			logSuccess("Generated " + jwtProofs.size() + " jwt proof(s)",
				args("proof_jwts", verifiableArray, "proofs", proofsObject, "key_count", jwtProofs.size()));

		} catch (ParseException | JOSEException e) {
			throw error("Couldn't create Proof JWT", e);
		}

		return env;
	}

	protected JsonObject createProofsObject(List<String> proofJwts) {
		JsonObject proofsObject = new JsonObject();
		proofsObject.add("jwt", OIDFJSON.convertListToJsonArray(proofJwts));
		return proofsObject;
	}

	protected String getIssuer(Environment env) {
		return env.getString("vci", "credential_issuer_metadata.credential_issuer");
	}

	protected String getCNonce(Environment env) {
		return env.getString("vci", "c_nonce");
	}
}
