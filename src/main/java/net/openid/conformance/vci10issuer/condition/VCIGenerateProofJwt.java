package net.openid.conformance.vci10issuer.condition;

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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public class VCIGenerateProofJwt extends AbstractCondition {

	public static final int DEFAULT_PROOF_LIFETIME_SECONDS = 60;

	@Override
	@PreEnvironment(required = "client_jwks")
	@PostEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		String serverIssuer = getIssuer(env);

		try {
			JWKSet jwkSet = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
			JWK jwk = jwkSet.getKeys().get(0);
			ECKey ecKey = ECKey.parse(jwk.toJSONString());

			String walletIssuerId = env.getString("client", "client_id");

			String cNonce = getCNonce(env);
			int proofLifetimeSeconds = getProofLifetimeSeconds(); //

			// Ensure the key uses the correct curve
			if (!Curve.P_256.equals(ecKey.getCurve())) {
				throw error("Private key does not use the required P-256 curve for ES256");
			}

			JWSSigner signer = new ECDSASigner(ecKey);

			String publicKeySetString = (jwk.toPublicJWK() != null ? jwk.toPublicJWK().toString() : null);

			JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256)
				.type(new JOSEObjectType("openid4vci-proof+jwt"));

			String proofTypeKey = env.getString("vci_proof_type_key");
			JsonObject proofType = env.getObject("vci_proof_type");

			// TODO how to select the proof type? Do we need to introduce a new proofType variant for this?
			if ("jwt".equals(proofTypeKey)) {
				headerBuilder.jwk(jwk.toPublicJWK());
			} else if ("attestation".equals(proofTypeKey)) {
				// TODO handle attestation proof type key
			}

			// add key attestation if necessary
			if (proofType.has("key_attestations_required")) {
				String keyAttestationJwt = env.getString("key_attestation_jwt");
				headerBuilder.customParam("key_attestation", keyAttestationJwt);
			}

			JWSHeader header = headerBuilder.build();

			Instant now = Instant.now();
			Date issueTime = Date.from(now);
			Date expirationTime = Date.from(now.plus(proofLifetimeSeconds, ChronoUnit.SECONDS));
			String jwtId = UUID.randomUUID().toString();  // Optional: Add a unique ID

			JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.issuer(walletIssuerId)
				.audience(serverIssuer)
				.issueTime(issueTime)
				.expirationTime(expirationTime)
				.claim("nonce", cNonce)
				.jwtID(jwtId)
				.build();

			SignedJWT signedJWT = new SignedJWT(header, claimsSet);
			signedJWT.sign(signer);

			String jwtProof = signedJWT.serialize();

			env.putString("vci", "proof.jwt", jwtProof);

			JsonObject verifiableObj = new JsonObject();
			verifiableObj.addProperty("verifiable_jws", jwtProof);
			verifiableObj.addProperty("public_jwk", publicKeySetString);

			logSuccess("Generated Proof JWT", args("proof_jwt", verifiableObj));

		} catch (ParseException | JOSEException e) {
			throw error("Couldn't create Proof JWT", e);
		}

		return env;
	}

	protected String getIssuer(Environment env) {
		return env.getString("vci", "credential_issuer_metadata.credential_issuer");
	}

	protected String getCNonce(Environment env) {
		String cnonce = env.getString("vci", "c_nonce");
		return cnonce;
	}

	protected int getProofLifetimeSeconds() {
		return DEFAULT_PROOF_LIFETIME_SECONDS;
	}

}
