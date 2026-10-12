package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.Environment;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Verifies the Token Status List JWT signature outside HAIP, where no trust anchor is mandated
 * and the Status Issuer may name its key in any of the ways draft-ietf-oauth-status-list
 * section 11.3 lists. The key is taken from the first of these sources that is present:
 * <ol>
 *   <li>the {@code jwk} JOSE header</li>
 *   <li>the end-entity certificate of the {@code x5c} JOSE header (signature only; the chain is
 *       validated under HAIP by {@link ValidateStatusListTokenX5cCertificateChain})</li>
 *   <li>a {@code kid} JOSE header holding a {@code did:jwk} DID, which encodes the key itself</li>
 *   <li>the {@code jwk} or {@code x5c} header of the referenced credential, when the Status List
 *       Token's {@code kid} names the credential's key (the same key as the Referenced Token)</li>
 *   <li>{@code status_list_token_issuer_jwks}, the keys {@link FetchJwtVcIssuerMetadataForStatusListToken}
 *       resolved from the issuer's JWT VC Issuer Metadata</li>
 *   <li>{@code server_jwks}, the keys the authorization server publishes at its {@code jwks_uri}</li>
 * </ol>
 * A key from a single-key source is given the token's {@code kid} so the standard kid-based
 * selection applies to it.
 *
 * Skips when no status list fetch was performed (credential had no status claim).
 */
public class VerifyStatusListTokenSignature extends AbstractVerifyJwsSignature {

	public static final String DID_JWK_PREFIX = "did:jwk:";

	private static final String TOKEN_NAME = "status list token";

	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObject("status_list_token")) {
			log("No parsed status list token in environment, skipping signature verification");
			return env;
		}

		String statusListTokenJwtString = env.getString("status_list_token", "value");
		if (statusListTokenJwtString == null) {
			throw error("status_list_token has no raw JWT value");
		}

		SignedJWT statusListTokenJwt;
		try {
			statusListTokenJwt = SignedJWT.parse(statusListTokenJwtString);
		} catch (ParseException e) {
			throw error("Unable to parse status list token", e);
		}

		JWSHeader header = statusListTokenJwt.getHeader();
		String kid = header.getKeyID();

		if (header.getJWK() != null) {
			verifyWith(statusListTokenJwtString, singleKeyJwks(header.getJWK(), kid), "JWT header jwk");
			return env;
		}

		List<Base64> x5c = header.getX509CertChain();
		if (x5c != null && !x5c.isEmpty()) {
			JWK leafKey = jwkFromCertificate(x5c.get(0), header);
			verifyWith(statusListTokenJwtString, singleKeyJwks(leafKey, kid), "JWT header x5c end-entity certificate");
			return env;
		}

		if (isDidJwk(kid)) {
			verifyWith(statusListTokenJwtString, singleKeyJwks(jwkFromDidJwk(kid, header), kid), "did:jwk in JWT header kid");
			return env;
		}

		JsonObject credentialKeys = keyEmbeddedInReferencedCredential(env, kid);
		if (credentialKeys != null) {
			verifyWith(statusListTokenJwtString, credentialKeys, "referenced credential's embedded key");
			return env;
		}

		if (env.containsObject("status_list_token_issuer_jwks")) {
			verifyWith(statusListTokenJwtString, env.getObject("status_list_token_issuer_jwks"), "JWT VC Issuer Metadata");
			return env;
		}

		if (env.containsObject("server_jwks")) {
			verifyWith(statusListTokenJwtString, env.getObject("server_jwks"), "authorization server");
			return env;
		}

		throw error("Unable to verify the status list token signature because no verification key could be resolved. "
				+ "The JOSE header carries neither 'jwk' nor 'x5c', 'kid' is not a did:jwk, the referenced credential "
				+ "does not embed a key with the same 'kid', no JWT VC Issuer Metadata was found for the issuer, "
				+ "and the authorization server metadata does not advertise a 'jwks_uri'",
			args("header", header.toJSONObject(),
				"iss", env.getElementFromObject("status_list_token", "claims.iss")));
	}

	private void verifyWith(String token, JsonObject jwks, String source) {
		log("Verifying status list token signature using the " + source, args("jwks", jwks));
		verifyJwsSignature(token, jwks, TOKEN_NAME, false, source);
	}

	public static boolean isDidJwk(String kid) {
		return kid != null && kid.startsWith(DID_JWK_PREFIX);
	}

	/**
	 * Wraps a key from a source that identifies it by itself (an embedded jwk, a certificate, a
	 * did:jwk) into a JWK set, carrying the token's kid so kid-based selection finds it.
	 */
	private JsonObject singleKeyJwks(JWK key, String kid) {
		JWK publicKey = key.toPublicJWK();
		if (publicKey == null) {
			throw error("The resolved status list token key has no public part", args("jwk", key.toJSONObject()));
		}
		if (kid != null && !kid.equals(publicKey.getKeyID())) {
			Map<String, Object> jwkJson = publicKey.toJSONObject();
			jwkJson.put("kid", kid);
			try {
				publicKey = JWK.parse(jwkJson);
			} catch (ParseException e) {
				throw error("Unable to assign the JWT header kid to the resolved status list token key", e,
					args("jwk", jwkJson, "kid", kid));
			}
		}
		return JsonParser.parseString(new JWKSet(publicKey).toString()).getAsJsonObject();
	}

	private JWK jwkFromCertificate(Base64 encodedCert, JWSHeader header) {
		X509Certificate cert = X509CertUtils.parse(encodedCert.decode());
		if (cert == null) {
			throw error("Unable to parse the end-entity certificate in the status list token x5c header",
				args("header", header.toJSONObject()));
		}
		PublicKey publicKey = cert.getPublicKey();
		try {
			if (publicKey instanceof RSAPublicKey) {
				return RSAKey.parse(cert);
			}
			if (publicKey instanceof ECPublicKey) {
				return ECKey.parse(cert);
			}
		} catch (JOSEException e) {
			throw error("Unable to read the public key from the status list token x5c end-entity certificate", e,
				args("certificate_subject", cert.getSubjectX500Principal().getName()));
		}
		throw error("The status list token x5c end-entity certificate holds a key type that cannot verify a JWS",
			args("key_algorithm", publicKey.getAlgorithm(),
				"certificate_subject", cert.getSubjectX500Principal().getName()));
	}

	/**
	 * A did:jwk DID is the base64url-encoded JWK itself, optionally followed by the
	 * {@code #0} verification method fragment.
	 */
	private JWK jwkFromDidJwk(String kid, JWSHeader header) {
		String encoded = kid.substring(DID_JWK_PREFIX.length());
		int fragment = encoded.indexOf('#');
		if (fragment >= 0) {
			encoded = encoded.substring(0, fragment);
		}
		try {
			return JWK.parse(new Base64URL(encoded).decodeToString());
		} catch (ParseException | IllegalArgumentException e) {
			throw error("The status list token kid is a did:jwk, but the JWK encoded in it could not be decoded", e,
				args("kid", kid, "header", header.toJSONObject()));
		}
	}

	/**
	 * The key the referenced credential embeds in its own JOSE header, when the status list
	 * token's kid names that key (draft-ietf-oauth-status-list section 11.3, same key as the
	 * Referenced Token). Null when there is no such key.
	 */
	private JsonObject keyEmbeddedInReferencedCredential(Environment env, String kid) {
		if (kid == null) {
			return null;
		}
		JsonElement credentialHeaderEl = env.getElementFromObject("sdjwt", "credential.header");
		if (credentialHeaderEl == null || !credentialHeaderEl.isJsonObject()) {
			return null;
		}
		JWSHeader credentialHeader;
		try {
			credentialHeader = JWSHeader.parse(credentialHeaderEl.toString());
		} catch (ParseException e) {
			return null;
		}
		if (!kid.equals(credentialHeader.getKeyID())) {
			return null;
		}
		if (credentialHeader.getJWK() != null) {
			return singleKeyJwks(credentialHeader.getJWK(), kid);
		}
		List<Base64> credentialX5c = credentialHeader.getX509CertChain();
		if (credentialX5c != null && !credentialX5c.isEmpty()) {
			return singleKeyJwks(jwkFromCertificate(credentialX5c.get(0), credentialHeader), kid);
		}
		return null;
	}
}
