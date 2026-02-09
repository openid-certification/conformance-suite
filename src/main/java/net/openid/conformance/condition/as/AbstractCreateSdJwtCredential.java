package net.openid.conformance.condition.as;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.authlete.sd.SDObjectBuilder;
import com.google.gson.JsonElement;
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
import net.openid.conformance.condition.client.ValidateSdJwtKbSdHash;
import net.openid.conformance.testmodule.Environment;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractCreateSdJwtCredential extends AbstractCondition {

	protected final Map<String, Object> additionalClaims;

	protected AbstractCreateSdJwtCredential() {
		this(null);
	}

	protected AbstractCreateSdJwtCredential(Map<String, Object> additionalClaims) {
		this.additionalClaims = additionalClaims;
	}

	public String keyBindingJwt(ECKey privateKey, String aud, String nonce, String sdHash) {
		// as per https://www.ietf.org/archive/id/draft-ietf-oauth-selective-disclosure-jwt-14.html#section-4.3
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("kb+jwt")).build();

		Map<String, Object> claims = new HashMap<>();
		Instant iat = Instant.now();
		claims.put("iat", iat.getEpochSecond());
		claims.put("aud", aud);
		claims.put("nonce", nonce);
		claims.put("sd_hash", sdHash);

		JWTClaimsSet claimsSet = null;
		try {
			claimsSet = JWTClaimsSet.parse(claims);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}


		SignedJWT jwt = new SignedJWT(header, claimsSet);

		JWSSigner signer = null;
		try {
			signer = new ECDSASigner(privateKey);
			jwt.sign(signer);
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}

		return jwt.serialize();
	}

	protected String createSdJwt(Environment env, JWK publicJWK, ECKey privateKey) {
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (credentialSigningJwkEl == null) {
			throw error("Credential Signing JWK missing from configuration");
		}
		JWK credentialSigningJwk = null;
		try {
			credentialSigningJwk = JWK.parse(credentialSigningJwkEl.toString());
		} catch (ParseException e) {
			throw error("Failed to create JWK from credential signing_jwk", e, args("signing_jwk", credentialSigningJwkEl));
		}

		// tries to generate a credential that's valid as per https://bmi.usercontent.opencode.de/eudi-wallet/eidas-2.0-architekturkonzept/functions/00-pid-issuance-and-presentation/#pid-contents

		SDObjectBuilder builder = new SDObjectBuilder();
		ArrayList<Disclosure> disclosures = new ArrayList<>();

		// as per https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#42-note-on-vct
		builder.putClaim("vct", "urn:eudi:pid:1");

		/*
		 * contents as per https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#41-encoding-of-pid-attributes-and-metadata
		 * mandatory elements are defined here:
		 * https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#22-mandatory-attributes-specified-in-cir-20242977
		 *
		 * We aim to include all mandatory elements
		 */

		disclosures.add(builder.putSDClaim("given_name", "Jean"));
		disclosures.add(builder.putSDClaim("family_name", "Dupont"));
		disclosures.add(builder.putSDClaim("birthdate", "1980-05-23"));

		Disclosure disclosure0 = new Disclosure("FR");
		disclosures.add(disclosure0);
		Map<String, Object> element = disclosure0.toArrayElement();
		disclosures.add(builder.putSDClaim("nationalities", List.of(element)));

		SDObjectBuilder pobBuilder = new SDObjectBuilder();
		disclosures.add(pobBuilder.putSDClaim("country", "DD"));
		Map<String, Object> placeOfBirth = pobBuilder.build();

		disclosures.add(builder.putSDClaim("place_of_birth", placeOfBirth));

		// Only add cnf claim if cryptographic binding is required (publicJWK is not null)
		if (publicJWK != null) {
			Map<String, Object> cnf = new HashMap<>();
			cnf.put("jwk", publicJWK.toJSONObject());
			builder.putClaim("cnf", cnf);
		}

		builder.putClaim("iat", Instant.now().getEpochSecond());
		builder.putClaim("exp", Instant.now().plus(14, ChronoUnit.DAYS).getEpochSecond());
		String baseUrl = env.getString("base_url");
		builder.putClaim("iss", baseUrl);

		// add support for adding additional claims from env if present
		if (additionalClaims != null) {
			for(var additionalClaim : additionalClaims.entrySet()) {
				builder.putClaim(additionalClaim.getKey(), additionalClaim.getValue());
			}
		}

		builder.putDecoyDigests(3);

		Map<String, Object> claims = builder.build();
		JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("dc+sd-jwt"));
		if (credentialSigningJwk.getX509CertChain() != null) {
			headerBuilder.x509CertChain(credentialSigningJwk.getX509CertChain());
		}
		JWSHeader header =
			headerBuilder.build();

		JWTClaimsSet claimsSet;
		try {
			claimsSet = JWTClaimsSet.parse(claims);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		SignedJWT jwt = new SignedJWT(header, claimsSet);

		JWSSigner signer = null;
		try {
			signer = new ECDSASigner((ECKey)credentialSigningJwk); // FIXME need to cope with RSA too
			jwt.sign(signer);
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}

		String bindingJwt = null;

		if (privateKey != null) {
			String aud = env.getString("client", "client_id");
			String sd_hash = null;
			try {
				sd_hash = ValidateSdJwtKbSdHash.getCalculatedSdHash(new SDJWT(jwt.serialize(), disclosures).toString());
			} catch (NoSuchAlgorithmException e) {
				throw error("Failed to create hash", e);
			}
			String nonce = env.getString("nonce");
			bindingJwt = keyBindingJwt(privateKey, aud, nonce, sd_hash);
		}
		SDJWT sdJwt = new SDJWT(jwt.serialize(), disclosures, bindingJwt);

		return sdJwt.toString();
	}
}
