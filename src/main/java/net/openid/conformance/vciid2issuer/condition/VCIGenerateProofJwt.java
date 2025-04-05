package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWTUtil;

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

		String serverIssuer = env.getString("config", "server.discoveryIssuer");
		if (serverIssuer != null && !serverIssuer.endsWith("/")) {
			// FIXME: add trailing slash to issuer, as testsuite requires it
			serverIssuer += "/";
		}

		try {
			JWKSet jwkSet = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
			JWK jwk = jwkSet.getKeys().get(0);
			RSAKey rsaKey = RSAKey.parse(jwk.toJSONString());

			// TODO find a better way to generate the walletIssuerId
			String walletIssuerId = "did:key:" + rsaKey.getKeyID();

			String cNonce = getCNonce();
			int proofLifetimeSeconds = getProofLifetimeSeconds(); //
			String jwtProof = createJwtProof(rsaKey, jwk.getKeyID(), walletIssuerId, serverIssuer, cNonce, proofLifetimeSeconds);

			env.putString("vci","proof.jwt", jwtProof);

			JsonObject jwtProofObject = JWTUtil.jwtStringToJsonObjectForEnvironment(jwtProof);
			logSuccess("Create Proof JWT", args("jwt", jwtProof, "decoded_jwt_json", jwtProofObject.toString()));

		} catch (ParseException | JOSEException e) {
			throw error("Couldn't create Proof JWT", e);
		}

		return env;
	}

	protected String getCNonce() {
		return null;  //TODO use nonce if provided
	}

	protected int getProofLifetimeSeconds() {
		return DEFAULT_PROOF_LIFETIME_SECONDS;
	}

	/**
	 * Creates a signed JWT proof according to OID4VCI requirements.
	 *
	 * @param walletPrivateKey The Wallet's private key for signing.
	 * @param walletKeyId      The Key ID ('kid') corresponding to the Wallet's key.
	 * @param walletIssuerId   The identifier for the Wallet ('iss' claim).
	 * @param issuerAudience   The identifier for the target Issuer ('aud' claim).
	 * @param nonce           The nonce received from the Issuer's token endpoint ('nonce' claim).
	 * @param proofLifetimeSeconds Validity duration of the proof in seconds (e.g., 60).
	 * @return The compact serialized JWT proof string.
	 * @throws JOSEException If signing fails.
	 */
	public static String createJwtProof(
		RSAKey walletPrivateKey,
		String walletKeyId,
		String walletIssuerId,
		String issuerAudience,
		String nonce,
		long proofLifetimeSeconds) throws JOSEException {

		JWSSigner signer = new RSASSASigner(walletPrivateKey); // Assumes ECKey

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.PS256)
			.type(new JOSEObjectType("openid4vci-proof+jwt"))
			.keyID(walletKeyId)
			.build();

		Instant now = Instant.now();
		Date issueTime = Date.from(now);
		Date expirationTime = Date.from(now.plus(proofLifetimeSeconds, ChronoUnit.SECONDS));
		String jwtId = UUID.randomUUID().toString();  // Optional: Add a unique ID

		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.issuer(walletIssuerId)
			.audience(issuerAudience)
			.issueTime(issueTime)
			.expirationTime(expirationTime)
			.claim("nonce", nonce)
			.jwtID(jwtId)
			.build();

		SignedJWT signedJWT = new SignedJWT(header, claimsSet);
		signedJWT.sign(signer);

		return signedJWT.serialize();
	}
}
