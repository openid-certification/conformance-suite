package net.openid.conformance.condition.as;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.authlete.sd.SDObjectBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.ValidateSdJwtKbSdHash;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddVpTokenToAuthorizationEndpointResponseParams extends AbstractCondition {

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
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

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

		builder.putClaim("vct", "https://example.bmi.bund.de/credential/pid/1.0");
		// fixme: vct#integrity ? but not sure what value to put

		disclosures.add(builder.putSDClaim("given_name", "Erika"));
		disclosures.add(builder.putSDClaim("family_name", "Mustermann"));
		disclosures.add(builder.putSDClaim("birthdate", "1963-08-12"));

		SDObjectBuilder ageBuilder = new SDObjectBuilder();
		disclosures.add(ageBuilder.putSDClaim("21", true));
		ageBuilder.putDecoyDigests(3);
		Map<String, Object> age = ageBuilder.build();

		Map<String, Object> cnf = new HashMap<>();
		cnf.put("jwk", privateKey.toPublicJWK().toJSONObject());

		builder.putClaim("cnf", cnf);
		builder.putClaim("iat", Instant.now().getEpochSecond());
		builder.putClaim("exp", Instant.now().plus(14, ChronoUnit.DAYS).getEpochSecond());
		builder.putClaim("age_equal_or_over", age);
		builder.putClaim("iss", "https://demo.pid-issuer.bundesdruckerei.de/c");

		builder.putDecoyDigests(3);

		Map<String, Object> claims = builder.build();
		JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("vc+sd-jwt"));
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

		params.addProperty("vp_token", vpToken);

		String id = env.getString("authorization_request_object", "claims.presentation_definition.id");
		JsonElement descArray = env.getElementFromObject("authorization_request_object", "claims.presentation_definition.input_descriptors");
		JsonObject foo = descArray.getAsJsonArray().get(0).getAsJsonObject();


		String descriptKey = OIDFJSON.getString(foo.get("id"));

		String ps = "{\n" +
			"      \"id\": \"vFB9qd4_0P-7fWRBBKHZx\",\n" +
			"      \"definition_id\": \""+id+"\",\n" +
			"      \"descriptor_map\": [\n" +
			"        {\n" +
			"          \"id\": \""+descriptKey+"\",\n" +
			"          \"format\": \"vc+sd-jwt\",\n" +
			"          \"path\": \"$\"\n" +
			"        }\n" +
			"      ]\n" +
			"    }";
		JsonElement jsonRoot = JsonParser.parseString(ps);
		params.add("presentation_submission", jsonRoot);

		logSuccess("Added vp_token to authorization endpoint response params", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
