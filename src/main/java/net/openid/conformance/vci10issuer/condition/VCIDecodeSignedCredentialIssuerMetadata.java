package net.openid.conformance.vci10issuer.condition;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
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

		// Extract and store claims
		extractAndStoreClaims(parsedJwt, env);

		logSuccess("Successfully decoded and verified signed credential issuer metadata using header jwk",
			args("credential_issuer_metadata", env.getElementFromObject("vci", "credential_issuer_metadata"),
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

		// Extract and store claims
		extractAndStoreClaims(parsedJwt, env);

		logSuccess("Successfully decoded and verified signed credential issuer metadata using x5c certificate",
			args("credential_issuer_metadata", env.getElementFromObject("vci", "credential_issuer_metadata"),
				"certificate_subject", cert.getSubjectX500Principal().getName(),
				"certificate_issuer", cert.getIssuerX500Principal().getName(),
				"jwk", JsonParser.parseString(jwk.toPublicJWK().toJSONString()),
				"header", header.toJSONObject(),
				"credential_issuer_metadata_jwt", encodedJwt));
	}

	private void extractAndStoreClaims(SignedJWT parsedJwt, Environment env) throws ParseException {
		JWTClaimsSet claimsSet = parsedJwt.getJWTClaimsSet();
		Map<String, Object> claims = claimsSet.getClaims();

		// Convert claims to JsonObject for credential_issuer_metadata
		JsonObject credentialIssuerMetadata = (JsonObject) JsonParser.parseString(new Gson().toJson(claims));

		env.putObject("vci", "credential_issuer_metadata", credentialIssuerMetadata);
	}
}
