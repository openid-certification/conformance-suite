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
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.client.ValidateSdJwtKbSdHash;
import net.openid.conformance.testmodule.Environment;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CreateSdJwtKbCredential extends AbstractCondition {

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


	@Override
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {

		// Create a private key for the credential key binding
		ECKey privateKey = null;
		try {
			privateKey = new ECKeyGenerator(Curve.P_256).generate();
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}

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

		// contents as per https://eu-digital-identity-wallet.github.io/eudi-doc-architecture-and-reference-framework/latest/annexes/annex-3/annex-3.01-pid-rulebook/#52-encoding-of-pid-attributes
		// There are more claims, possibly we should include more of them
		builder.putClaim("vct", "urn:eudi:pid:1");

		disclosures.add(builder.putSDClaim("given_name", "Jean"));
		disclosures.add(builder.putSDClaim("family_name", "Dupont"));
		disclosures.add(builder.putSDClaim("birthdate", "1980-05-23"));
		disclosures.add(builder.putSDClaim("age_in_years", "44"));

		SDObjectBuilder ageBuilder = new SDObjectBuilder();
		disclosures.add(ageBuilder.putSDClaim("21", true));
		disclosures.add(ageBuilder.putSDClaim("65", false));
		ageBuilder.putDecoyDigests(3);
		Map<String, Object> ageEqualOrOver = ageBuilder.build();

		Map<String, Object> cnf = new HashMap<>();
		cnf.put("jwk", privateKey.toPublicJWK().toJSONObject());

		builder.putClaim("cnf", cnf);
		builder.putClaim("iat", Instant.now().getEpochSecond());
		builder.putClaim("exp", Instant.now().plus(14, ChronoUnit.DAYS).getEpochSecond());
		builder.putClaim("age_equal_or_over", ageEqualOrOver);
		String baseUrl = env.getString("base_url");
		builder.putClaim("iss", baseUrl);

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

		String aud = env.getString("client", "client_id");
		String nonce = env.getString("nonce");
		String sd_hash = null;
		try {
			sd_hash = ValidateSdJwtKbSdHash.getCalculatedSdHash(new SDJWT(jwt.serialize(), disclosures).toString());
		} catch (NoSuchAlgorithmException e) {
			throw error("Failed to create hash", e);
		}
		String bindingJwt = keyBindingJwt(privateKey, aud, nonce, sd_hash);
		SDJWT sdJwt = new SDJWT(jwt.serialize(), disclosures, bindingJwt);

		String vpToken = sdJwt.toString();

		env.putString("credential", vpToken);

		log("Created an SD JWT credential", args("credential", vpToken));

		return env;

	}

}
