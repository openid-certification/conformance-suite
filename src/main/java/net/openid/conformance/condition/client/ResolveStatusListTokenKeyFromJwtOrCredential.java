package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
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
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * First step of resolving the Token Status List JWT signing key outside HAIP
 * (draft-ietf-oauth-status-list section 11.3): the sources that need no network request.
 * The key is taken from the first of these that is present:
 * <ol>
 *   <li>the {@code jwk} JOSE header</li>
 *   <li>the end-entity certificate of the {@code x5c} JOSE header (signature only; the chain is
 *       validated under HAIP by {@link ValidateStatusListTokenX5cCertificateChain})</li>
 *   <li>a {@code kid} JOSE header holding a {@code did:jwk} DID, which encodes the key itself</li>
 *   <li>the {@code jwk} or {@code x5c} header of the referenced credential, when the Status List
 *       Token's {@code kid} names the credential's key (the same key as the Referenced Token)</li>
 * </ol>
 * The key is stored as {@code status_list_token_verification_key} ({@code jwks}, {@code source}),
 * given the token's {@code kid} so the standard kid-based selection applies to it.
 *
 * When none of these is present and {@code iss} of the token (or of the referenced credential,
 * when the token has none) is an HTTPS URL, {@code status_list_token_jwt_vc_issuer} is stored
 * with the JWT VC Issuer Metadata URL for {@link CallJwtVcIssuerMetadataEndpoint}.
 *
 * Clears the key resolution state of a previous credential first. Skips when no status list
 * fetch was performed (credential had no status claim).
 */
public class ResolveStatusListTokenKeyFromJwtOrCredential extends AbstractCondition {

	public static final String DID_JWK_PREFIX = "did:jwk:";

	public static final String WELL_KNOWN_PATH = "/.well-known/jwt-vc-issuer";

	@Override
	public Environment evaluate(Environment env) {

		env.removeObject("status_list_token_verification_key");
		env.removeObject("status_list_token_jwt_vc_issuer");
		env.removeObject("jwt_vc_issuer_metadata_endpoint_response");
		env.removeObject("jwt_vc_issuer_metadata");
		env.removeNativeValue("jwt_vc_issuer_metadata_jwks_uri");
		env.removeObject("jwt_vc_issuer_metadata_jwks_uri_endpoint_response");
		env.removeObject("jwt_vc_issuer_metadata_jwks");
		env.removeObject("status_list_token_server_jwks");

		if (!env.containsObject("status_list_token")) {
			log("No parsed status list token in environment, skipping key resolution");
			return env;
		}

		String statusListTokenJwtString = env.getString("status_list_token", "value");
		if (statusListTokenJwtString == null) {
			throw error("status_list_token has no raw JWT value");
		}

		JWSHeader header;
		try {
			header = SignedJWT.parse(statusListTokenJwtString).getHeader();
		} catch (ParseException e) {
			throw error("Unable to parse status list token", e);
		}
		String kid = header.getKeyID();

		if (header.getJWK() != null) {
			return storeKey(env, singleKeyJwks(header.getJWK(), kid), "JWT header jwk");
		}

		List<Base64> x5c = header.getX509CertChain();
		if (x5c != null && !x5c.isEmpty()) {
			return storeKey(env, singleKeyJwks(jwkFromCertificate(x5c.get(0), header), kid),
				"JWT header x5c end-entity certificate");
		}

		if (isDidJwk(kid)) {
			return storeKey(env, singleKeyJwks(jwkFromDidJwk(kid, header), kid), "did:jwk in JWT header kid");
		}

		JsonObject credentialKeys = keyEmbeddedInReferencedCredential(env, kid);
		if (credentialKeys != null) {
			return storeKey(env, credentialKeys, "referenced credential's embedded key");
		}

		String iss = env.getString("status_list_token", "claims.iss");
		String issSource = "status list token";
		if (iss == null) {
			iss = env.getString("sdjwt", "credential.claims.iss");
			issSource = "referenced credential";
		}
		if (iss == null) {
			log("The status list token does not carry its signing key, and neither it nor the referenced credential has an iss claim, so JWT VC Issuer Metadata cannot be located",
				args("header", header.toJSONObject()));
			return env;
		}

		String metadataUrl = jwtVcIssuerMetadataUrl(iss);
		if (metadataUrl == null) {
			log("The status list token does not carry its signing key, and the iss claim is not an HTTPS URL without query or fragment, so JWT VC Issuer Metadata cannot be located",
				args("header", header.toJSONObject(), "iss", iss, "iss_source", issSource));
			return env;
		}

		JsonObject jwtVcIssuer = new JsonObject();
		jwtVcIssuer.addProperty("iss", iss);
		jwtVcIssuer.addProperty("iss_source", issSource);
		jwtVcIssuer.addProperty("metadata_url", metadataUrl);
		env.putObject("status_list_token_jwt_vc_issuer", jwtVcIssuer);

		log("The status list token does not carry its signing key and the referenced credential does not embed a key with the same kid, so the key is resolved from the issuer's JWT VC Issuer Metadata",
			args("header", header.toJSONObject(), "iss", iss, "iss_source", issSource, "url", metadataUrl));
		return env;
	}

	private Environment storeKey(Environment env, JsonObject jwks, String source) {
		JsonObject key = new JsonObject();
		key.add("jwks", jwks);
		key.addProperty("source", source);
		env.putObject("status_list_token_verification_key", key);
		logSuccess("Resolved the status list token verification key from the " + source,
			args("jwks", jwks));
		return env;
	}

	public static boolean isDidJwk(String kid) {
		return kid != null && kid.startsWith(DID_JWK_PREFIX);
	}

	/**
	 * The metadata URL per SD-JWT VC: the well-known string inserted between the host and the
	 * path of {@code iss}. Null when {@code iss} is not an HTTPS URL or has a query or fragment.
	 */
	public static String jwtVcIssuerMetadataUrl(String iss) {
		URI issUri;
		try {
			issUri = new URI(iss);
		} catch (URISyntaxException e) {
			return null;
		}
		if (!"https".equals(issUri.getScheme()) || issUri.getRawAuthority() == null
			|| issUri.getRawQuery() != null || issUri.getRawFragment() != null) {
			return null;
		}
		String path = Strings.nullToEmpty(issUri.getRawPath());
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return "https://" + issUri.getRawAuthority() + WELL_KNOWN_PATH + path;
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
			throw error("Unable to parse the end-entity certificate in the x5c header",
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
			throw error("Unable to read the public key from the x5c end-entity certificate", e,
				args("certificate_subject", cert.getSubjectX500Principal().getName()));
		}
		throw error("The x5c end-entity certificate holds a key type that cannot verify a JWS",
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
	 * token's kid names that key (the same key as the Referenced Token). Null when there is no
	 * such key.
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
