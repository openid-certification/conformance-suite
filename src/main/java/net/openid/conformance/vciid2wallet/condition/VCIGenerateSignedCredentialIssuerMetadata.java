package net.openid.conformance.vciid2wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class VCIGenerateSignedCredentialIssuerMetadata extends AbstractSignJWT {

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("signed_credential_issuer_metadata", jws);
		logSuccess("Generated the signed credential metadata", args("signed_credential_issuer_metadata", verifiableObj));
	}

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("openidvci-issuer-metadata+jwt");
	}

	@Override
	@PreEnvironment(required = {"config", "credential_issuer_metadata" })
	public Environment evaluate(Environment env) {

		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (credentialSigningJwkEl == null) {
			throw error("Credential signing JWK missing from configuration");
		}

		String issuer = env.getString("credential_issuer_metadata", "credential_issuer");
		if (issuer == null || issuer.isBlank()) {
			throw error("Credential issuer must not be null or empty");
		}

		JsonObject claims = new JsonObject();
		claims.addProperty("sub", issuer);
		Instant iat = Instant.now();
		Instant exp = iat.plus(1, ChronoUnit.DAYS);
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		JsonObject credentialIssuerMetadata = env.getObject("credential_issuer_metadata");
		for (String key : credentialIssuerMetadata.keySet()) {
			claims.add(key, credentialIssuerMetadata.get(key));
		}

		JsonObject jwks = JWKUtil.createJwksObjectFromJwkObjects(credentialSigningJwkEl.getAsJsonObject());

		signJWT(env, claims, jwks, true);

		return env;
	}
}
