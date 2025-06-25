package net.openid.conformance.vciid2issuer.condition.clientattestation;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateClientAttestationProofJwt extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String issuer = env.getString("server","issuer");
		String clientId = env.getString("client","client_id");

		String clientInstanceKey = env.getString("vci", "client_attestation_key");
		JWK clientInstanceKeyJwk;
		try {
			clientInstanceKeyJwk = JWK.parse(clientInstanceKey);
		} catch (ParseException e) {
			throw error("Client attestation key could not be parsed", e);
		}

		JWSHeader.Builder headerBuilder = new JWSHeader //
			.Builder(JWSAlgorithm.ES256) //
			.type(new JOSEObjectType("oauth-client-attestation-pop+jwt"));

		JWSHeader header = headerBuilder.build();

		Map<String, Object> claims = new HashMap<>();
		claims.put("iss", clientId);

		Instant iat = Instant.now();
		Instant exp = getExp(iat);

		claims.put("iat", iat.getEpochSecond());
		claims.put("nbf", iat.getEpochSecond());
		claims.put("exp", exp.getEpochSecond());
		claims.put("aud", issuer);
		claims.put("jti", UUID.randomUUID().toString());
		// TODO add support for nonce retrieval https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-05#section-8
		// claims.put("nonce", nonce);

		JWTClaimsSet claimsSet;
		try {
			claimsSet = JWTClaimsSet.parse(claims);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		SignedJWT jwt = new SignedJWT(header, claimsSet);

		JWSSigner signer;
		try {
			signer = new ECDSASigner((ECKey) clientInstanceKeyJwk); // FIXME need to cope with RSA too
			jwt.sign(signer);
		} catch (JOSEException e) {
			throw error("Failed to sign client attestation jwt", e);
		}

		String jwtString = jwt.serialize();

		env.putString("client_attestation_pop", jwtString);

		log("Generated client attestation proof jwt", args("client_attestation_pop", jwtString));

		return env;
	}

	protected Instant getExp(Instant iat) {
		return iat.plusSeconds(5 * 60);
	}
}
