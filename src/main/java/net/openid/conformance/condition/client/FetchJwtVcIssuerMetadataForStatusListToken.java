package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

/**
 * Resolves the Status List Token signing keys through the web-based mechanism SD-JWT VC defines
 * for the referenced credential (draft-ietf-oauth-status-list section 11.3): the JWT VC Issuer
 * Metadata published at {@code /.well-known/jwt-vc-issuer} under the token's {@code iss}, or
 * under the credential's {@code iss} when the token has none. The resolved keys are stored as
 * {@code status_list_token_issuer_jwks} for {@link VerifyStatusListTokenSignature}.
 *
 * Nothing is fetched when the token already carries its key ({@code jwk}, {@code x5c} or a
 * {@code did:jwk} kid) or when {@code iss} is not an HTTPS URL. Publishing JWT VC Issuer Metadata
 * is optional, so a 404 only logs; a malformed response, an {@code issuer} that differs from
 * {@code iss}, or metadata without keys is an error.
 *
 * Skips when no status list fetch was performed (credential had no status claim).
 */
public class FetchJwtVcIssuerMetadataForStatusListToken extends AbstractCondition {

	public static final String WELL_KNOWN_PATH = "/.well-known/jwt-vc-issuer";

	@Override
	public Environment evaluate(Environment env) {

		env.removeObject("status_list_token_issuer_jwks");

		if (!env.containsObject("status_list_token")) {
			log("No parsed status list token in environment, skipping JWT VC Issuer Metadata resolution");
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

		if (header.getJWK() != null
			|| (header.getX509CertChain() != null && !header.getX509CertChain().isEmpty())
			|| VerifyStatusListTokenSignature.isDidJwk(header.getKeyID())) {
			log("The status list token carries its signing key in the JOSE header, no web-based key resolution is needed",
				args("header", header.toJSONObject()));
			return env;
		}

		String iss = env.getString("status_list_token", "claims.iss");
		String issSource = "status list token";
		if (iss == null) {
			iss = env.getString("sdjwt", "credential.claims.iss");
			issSource = "referenced credential";
		}
		if (iss == null) {
			log("Neither the status list token nor the referenced credential has an iss claim, so JWT VC Issuer Metadata cannot be located");
			return env;
		}

		String metadataUrl = jwtVcIssuerMetadataUrl(iss);
		if (metadataUrl == null) {
			log("The iss claim is not an HTTPS URL without query or fragment, so JWT VC Issuer Metadata cannot be located",
				args("iss", iss, "iss_source", issSource));
			return env;
		}

		JsonObject metadata = fetchJson(env, metadataUrl, "JWT VC Issuer Metadata");
		if (metadata == null) {
			log("No JWT VC Issuer Metadata is published for the issuer",
				args("iss", iss, "iss_source", issSource, "url", metadataUrl));
			return env;
		}

		JsonElement issuerEl = metadata.get("issuer");
		if (issuerEl == null || !issuerEl.isJsonPrimitive() || !issuerEl.getAsJsonPrimitive().isString()) {
			throw error("JWT VC Issuer Metadata does not contain an 'issuer' string",
				args("url", metadataUrl, "metadata", metadata));
		}
		String issuer = OIDFJSON.getString(issuerEl);
		if (!issuer.equals(iss)) {
			throw error("The 'issuer' in the JWT VC Issuer Metadata is not identical to the iss claim it was fetched for, so the metadata must not be used",
				args("url", metadataUrl, "issuer", issuer, "iss", iss, "iss_source", issSource));
		}

		JsonObject jwks;
		JsonElement jwksEl = metadata.get("jwks");
		JsonElement jwksUriEl = metadata.get("jwks_uri");
		if (jwksEl != null) {
			if (!jwksEl.isJsonObject()) {
				throw error("'jwks' in the JWT VC Issuer Metadata is not a JSON object", args("url", metadataUrl, "metadata", metadata));
			}
			jwks = jwksEl.getAsJsonObject();
		} else if (jwksUriEl != null) {
			if (!jwksUriEl.isJsonPrimitive() || !jwksUriEl.getAsJsonPrimitive().isString()) {
				throw error("'jwks_uri' in the JWT VC Issuer Metadata is not a string", args("url", metadataUrl, "metadata", metadata));
			}
			String jwksUri = OIDFJSON.getString(jwksUriEl);
			jwks = fetchJson(env, jwksUri, "JWT VC Issuer Metadata jwks_uri");
			if (jwks == null) {
				throw error("The jwks_uri in the JWT VC Issuer Metadata returned 404",
					args("url", metadataUrl, "jwks_uri", jwksUri));
			}
		} else {
			throw error("JWT VC Issuer Metadata contains neither 'jwks' nor 'jwks_uri'",
				args("url", metadataUrl, "metadata", metadata));
		}

		env.putObject("status_list_token_issuer_jwks", jwks);
		logSuccess("Resolved the status list token issuer keys from JWT VC Issuer Metadata",
			args("iss", iss, "iss_source", issSource, "url", metadataUrl, "jwks", jwks));
		return env;
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

	/** Fetches a JSON object; null on 404, an error on any other failure. */
	private JsonObject fetchJson(Environment env, String url, String what) {
		ResponseEntity<String> response;
		try {
			RestTemplate restTemplate = createRestTemplateWithCache(env);
			response = restTemplate.getForEntity(url, String.class);
		} catch (HttpStatusCodeException e) {
			if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
				return null;
			}
			throw error("Fetching " + what + " returned an error status", e,
				args("url", url, "status", e.getStatusCode().value(), "body", e.getResponseBodyAsString()));
		} catch (RestClientException e) {
			throw error("Fetching " + what + " failed", e, args("url", url));
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException
				| NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e, args("url", url));
		}

		if (response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
			return null;
		}
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Fetching " + what + " returned an error status",
				args("url", url, "status", response.getStatusCode().value(), "body", response.getBody()));
		}
		String body = response.getBody();
		if (Strings.isNullOrEmpty(body)) {
			throw error(what + " response body is empty", args("url", url));
		}
		try {
			JsonElement json = JsonParser.parseString(body);
			if (!json.isJsonObject()) {
				throw error(what + " response body is not a JSON object", args("url", url, "body", body));
			}
			return json.getAsJsonObject();
		} catch (JsonSyntaxException e) {
			throw error(what + " response body is not valid JSON", e, args("url", url, "body", body));
		}
	}
}
