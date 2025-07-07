package net.openid.conformance.vciid2issuer.condition.clientattestation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateClientAttestationJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"vci", "config", "client"})
	public Environment evaluate(Environment env) {

		String issuer = env.getString("client_attestation","issuer");
		String clientId = env.getString("client", "client_id");

		String clientInstanceKey = env.getString("vci", "client_instance_key");
		JsonObject cnf = new JsonObject();
		try {
			JWK clientInstanceKeyJwk = JWK.parse(clientInstanceKey);
			JWK clientInstanceKeyPublicJwk = clientInstanceKeyJwk.toPublicJWK();
			JsonObject clientInstancePublicKeyJwk = JsonParser.parseString(clientInstanceKeyPublicJwk.toJSONString()).getAsJsonObject();
			cnf.add("jwk", clientInstancePublicKeyJwk);
		} catch (ParseException e) {
			throw error("Client attestation key could not be parsed", e);
		}

		JsonElement clientAttesterKeysJwksEl = env.getElementFromObject("config", "vci.client_attester_keys_jwks");
		if (clientAttesterKeysJwksEl == null) {
			throw error("client_attester_keys_jwks could not be found");
		}

		JWK signingKey;
		try {
			JsonObject clientAttesterKeysJwks = clientAttesterKeysJwksEl.getAsJsonObject();
			signingKey = JWKUtil.getSigningKey(clientAttesterKeysJwks);
		} catch (ParseException e) {
			throw error("Failed to extract signing key for client attestation jwt", e, args("client_attester_keys_jwks", clientAttesterKeysJwksEl));
		}

		JWSSigner signer;
		JWSAlgorithm jwsAlgorithm;
		try {
			if (signingKey instanceof ECKey ecKey) {
				signer = new ECDSASigner(ecKey);
				jwsAlgorithm = JWSAlgorithm.ES256;
			} else  if (signingKey instanceof RSAKey rsaKey) {
				signer = new RSASSASigner(rsaKey);
				jwsAlgorithm = JWSAlgorithm.RS256;
			} else {
				throw error("Unsupported client attester signing key", args("signingJwk", signingKey));
			}
		} catch (JOSEException e) {
			throw error("Failed to create jws signer for client attestation jwt", e);
		}

		// see: https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0-03.html#section-4.3.1-2
		String clientAttestationCertificate = env.getString("config", "vci.client_attester_certificate");
		if (clientAttestationCertificate == null) {
			throw error("Client attestation certificate could not be found");
		}
		List<Base64> clientCertList = Arrays.stream(clientAttestationCertificate.split(",")).map(Base64::new).toList();

		JWSHeader.Builder headerBuilder = new JWSHeader //
			.Builder(jwsAlgorithm) //
			.x509CertChain(clientCertList) //
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

		try {
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
