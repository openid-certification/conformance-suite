package net.openid.conformance.vci10issuer.condition;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class VCIDecodeSignedCredentialIssuerMetadata extends AbstractVerifyJwsSignature {

	private static final JOSEObjectType OPENID_VCI_ISSUER_METADATA_JWT = new JOSEObjectType("openidvci-issuer-metadata+jwt");

	@Override
	@PreEnvironment(required = "credential_issuer_metadata_endpoint_response")
	@PostEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		String signedCredentialMetadataJwt = env.getString("credential_issuer_metadata_endpoint_response", "body");

		if (signedCredentialMetadataJwt == null || signedCredentialMetadataJwt.isBlank()) {
			throw error("Signed credential issuer metadata is missing or empty");
		}

		try {
			// Parse the signed JWT
			SignedJWT jwt = SignedJWT.parse(signedCredentialMetadataJwt);
			JWSHeader header = jwt.getHeader();

			JOSEObjectType type = header.getType();
			if (!OPENID_VCI_ISSUER_METADATA_JWT.equals(type)) {
				throw error("Signed credential issuer metadata JWT header type is not openidvci-issuer-metadata+jwt",
					args("header", header.toJSONObject()));
			}

			JWK jwk = header.getJWK();
			List<Base64> x5c = header.getX509CertChain();

			if (jwk != null) {
				// Verify signature using embedded JWK
				verifySignatureWithJwk(signedCredentialMetadataJwt, jwt, jwk, header, env);
			} else if (x5c != null && !x5c.isEmpty()) {
				// Verify signature using x5c certificate chain
				verifySignatureWithX5c(signedCredentialMetadataJwt, jwt, x5c, header, env);
			} else {
				throw error("Signed credential issuer metadata JWT header contains neither jwk nor x5c",
					args("header", header.toJSONObject()));
			}

			return env;

		} catch (ParseException | JOSEException e) {
			throw error("Failed to parse or verify signed credential issuer metadata JWT", e,
				args("signed_jwt", signedCredentialMetadataJwt));
		}
	}

	private void verifySignatureWithJwk(String encodedJwt, SignedJWT parsedJwt, JWK jwk, JWSHeader header, Environment env)
		throws ParseException {

		// Verify the signature using the embedded JWK
		JWKSet jwkSet = new JWKSet(jwk);
		JsonObject jwkSetObj = (JsonObject) JsonParser.parseString(jwkSet.toString());
		verifyJwsSignature(encodedJwt, jwkSetObj, "signed credential issuer metadata", false, "JWT header jwk");

		verifySignedIssuerMetadataClaims(parsedJwt, env);

		// Extract and store claims
		extractAndStoreClaims(parsedJwt, env);

		logSuccess("Successfully decoded and verified signed credential issuer metadata using header jwk",
			args("credential_issuer_metadata", env.getElementFromObject("vci", "credential_issuer_metadata"),
				"credential_issuer_metadata_signed", env.getElementFromObject("vci", "credential_issuer_metadata_signed"),
				"jwk", JsonParser.parseString(jwk.toPublicJWK().toJSONString()),
				"header", header.toJSONObject(),
				"credential_issuer_metadata_jwt", encodedJwt));
	}

	private void verifySignatureWithX5c(String encodedJwt, SignedJWT parsedJwt, List<Base64> x5c, JWSHeader header, Environment env)
		throws JOSEException, ParseException {

		// Parse the first certificate in the chain (the signing certificate)
		String encodedCert = x5c.get(0).toString();
		byte[] der = java.util.Base64.getDecoder().decode(encodedCert);
		X509Certificate cert = X509CertUtils.parse(der);

		if (cert == null) {
			throw error("Failed to parse X.509 certificate from x5c header",
				args("x5c", x5c.get(0).toString()));
		}

		PublicKey pubKey = cert.getPublicKey();
		JWK jwk;

		if (pubKey instanceof RSAPublicKey) {
			jwk = RSAKey.parse(cert);
		} else if (pubKey instanceof ECPublicKey) {
			jwk = ECKey.parse(cert);
		} else {
			throw error("Unsupported key type in x5c certificate",
				args("key_type", pubKey.getAlgorithm()));
		}

		// Verify the signature
		JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
		JWSVerifier verifier = factory.createJWSVerifier(parsedJwt.getHeader(), pubKey);

		if (!parsedJwt.verify(verifier)) {
			throw error("Failed to verify signature using key from x5c header",
				args("certificate_subject", cert.getSubjectX500Principal().getName(),
					"header", header.toJSONObject()));
		}

		verifySignedIssuerMetadataClaims(parsedJwt, env);

		// Extract and store claims
		extractAndStoreClaims(parsedJwt, env);

		logSuccess("Successfully decoded and verified signed credential issuer metadata using x5c certificate",
			args("credential_issuer_metadata", env.getElementFromObject("vci", "credential_issuer_metadata"),
				"credential_issuer_metadata_signed", env.getElementFromObject("vci", "credential_issuer_metadata_signed"),
				"certificate_subject", cert.getSubjectX500Principal().getName(),
				"certificate_issuer", cert.getIssuerX500Principal().getName(),
				"jwk", JsonParser.parseString(jwk.toPublicJWK().toJSONString()),
				"header", header.toJSONObject(),
				"credential_issuer_metadata_jwt", encodedJwt));
	}

	private void verifySignedIssuerMetadataClaims(SignedJWT parsedJwt, Environment env) throws ParseException {

		Map<String, Object> payload = parsedJwt.getJWTClaimsSet().toJSONObject();

		// Validating Signed Metadata JWS claims
		// See: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-12.2.3-2.2.1
		// sub: REQUIRED. String matching the Credential Issuer Identifier
		Object sub = payload.get("sub");
		if (sub == null) {
			throw error("Missing 'sub' claim in signed credential issuer metadata JWT",
					args("payload", payload));
		}
		String issuer = env.getString("config", "vci.credential_issuer_url");
		if (!issuer.equals(sub)) {
			throw error("Issuer in 'sub' claim does not match the Credential Issuer Identifier in the test configuration",
					args("sub", sub, "issuer", issuer));
		}

		// iat: REQUIRED. Integer for the time at which the Credential Issuer Metadata was issued using the syntax defined in [RFC7519].
		Object iat = payload.get("iat");
		if (iat == null) {
			throw error("Missing 'iat' claim in signed credential issuer metadata JWT",
				args("payload", payload));
		}
		if (!(iat instanceof Number) || ((Number) iat).longValue() < 0L) {
			throw error("Invalid 'iat' claim in signed credential issuer metadata JWT",
				args("payload", payload));
		}

		// exp: OPTIONAL. Integer for the time at which the Credential Issuer Metadata is expiring, using the syntax defined in [RFC7519].
		Object exp = payload.get("exp");
		// only check exp if present
		if (exp != null && (!(exp instanceof Number) || ((Number) exp).longValue() < 0L)) {
			throw error("Invalid 'exp' claim in signed credential issuer metadata JWT",
				args("payload", payload));
		}

		// iss: OPTIONAL. String denoting the party attesting to the claims in the signed metadata
		Object iss = payload.get("iss");
		if (iss != null && !(iss instanceof String)) {
			throw error("Invalid 'iss' claim in signed credential issuer metadata JWT",
				args("payload", payload));
		}
	}

	private void extractAndStoreClaims(SignedJWT parsedJwt, Environment env) throws ParseException {
		JWTClaimsSet claimsSet = parsedJwt.getJWTClaimsSet();
		// We need to use toJSONObject() here, because getClaims() already converts values (e.g. Date for exp)
		Map<String, Object> claims = claimsSet.toJSONObject();

		// Convert claims to JsonObject for credential_issuer_metadata
		JsonObject credentialIssuerMetadata = (JsonObject) JsonParser.parseString(new Gson().toJson(claims));

		// Keep the original claims from the signed metadata payload
		JsonObject credentialIssuerMetadataSigned = credentialIssuerMetadata.deepCopy();
		env.putObject("vci", "credential_issuer_metadata_signed", credentialIssuerMetadataSigned);

		// remove JWT claims from the stored credential issuer metadata
		credentialIssuerMetadata.remove("iat");
		credentialIssuerMetadata.remove("exp");
		credentialIssuerMetadata.remove("iss");
		credentialIssuerMetadata.remove("sub");

		env.putObject("vci", "credential_issuer_metadata", credentialIssuerMetadata);
	}
}
