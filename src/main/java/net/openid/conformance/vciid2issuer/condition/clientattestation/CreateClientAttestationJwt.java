package net.openid.conformance.vciid2issuer.condition.clientattestation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CreateClientAttestationJwt extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String issuer = env.getString("client_attestation","issuer");
		String clientId = env.getString("client", "client_id");

		String clientAttestationKey = env.getString("vci", "client_attestation_key");
		JsonObject cnf;
		try {
			JWK clientAttestationKeyJwk = JWK.parse(clientAttestationKey);
			cnf = JsonParser.parseString(clientAttestationKeyJwk.toPublicJWK().toJSONString()).getAsJsonObject();
		} catch (ParseException e) {
			throw error("Client attestation key could not be parsed", e);
		}

		String keyId = env.getString("vci","client_attestation_key_id");

		JWSHeader.Builder headerBuilder = new JWSHeader //
			.Builder(JWSAlgorithm.ES256) //
			.keyID(keyId) //
			.type(new JOSEObjectType("oauth-client-attestation+jwt"));

		JWSHeader header = headerBuilder.build();

		Map<String, Object> claims = new HashMap<>();
		claims.put("iss", issuer);
		claims.put("sub", clientId);

		Instant iat = Instant.now();
		Instant exp = getExp(iat);

		claims.put("iat", iat.getEpochSecond());
		claims.put("nbf", iat.getEpochSecond());
		claims.put("exp", exp.getEpochSecond());

		claims.put("cnf", cnf);

		JWTClaimsSet claimsSet;
		try {
			claimsSet = JWTClaimsSet.parse(claims);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		SignedJWT jwt = new SignedJWT(header, claimsSet);

		ECKey signingKey = null;
		try {
			signingKey = (ECKey)JWKUtil.getSigningKey(env.getElementFromObject("config", "vci.client_attester_keys_jwks").getAsJsonObject());
		} catch (ParseException e) {
			throw error("Failed to extract signing key for client attestation jwt", e);
		}

		JWSSigner signer;
		try {
			signer = new ECDSASigner(signingKey); // FIXME need to cope with RSA too
			jwt.sign(signer);
		} catch (JOSEException e) {
			throw error("Failed to sign client attestation jwt", e);
		}

		String jwtString = jwt.serialize();

		env.putString("client_attestation", jwtString);

		log("Generated client attestation jwt", args("client_attestation", jwtString));

		return env;
	}

	protected Instant getExp(Instant iat) {
		return iat.plusSeconds(5 * 60);
	}
}
