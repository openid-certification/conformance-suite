package net.openid.conformance.vci10wallet.condition;

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
import net.openid.conformance.util.TestKeysAndCerts;

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

		// ETSI TS 119 472-3 ISS-MDATA-4.2.1-02: the issuer metadata is signed with the issuer's access
		// certificate, so the built-in WRPAC signs it unless the tester supplied a 'Signing JWK', which
		// then signs both the credentials and the metadata
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		JsonObject jwks;
		if (credentialSigningJwkEl == null) {
			jwks = TestKeysAndCerts.getWrpac().jwksJson();
			log("No 'Signing JWK' in the 'Credential Issuer' section of the test configuration; signing the issuer metadata"
					+ " with the suite's built-in wallet-relying-party access certificate",
				args("signing_certificate_subject", TestKeysAndCerts.getWrpac().getCert().getSubject().getName(),
					"access_ca_url", TestKeysAndCerts.SuiteCa.ACCESS.getTrustAnchorUrl()));
		} else {
			jwks = JWKUtil.createJwksObjectFromJwkObjects(credentialSigningJwkEl.getAsJsonObject());
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

		signJWT(env, claims, jwks, true, false, true, true);

		return env;
	}
}
