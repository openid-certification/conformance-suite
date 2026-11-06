package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;

public class ValidatePoppySessionJwt extends AbstractCondition {

	static final long ACCEPTANCE_WINDOW_SECONDS = 60;

	@Override
	@PreEnvironment(required = {"incoming_request", "poppy_delegated_public_jwk"}, strings = {"poppy_session_jwt", "issuer", "poppy_incoming_client_id"})
	@PostEnvironment(required = "poppy_session_claims")
	public Environment evaluate(Environment env) {
		String jwtString = env.getString("poppy_session_jwt");

		SignedJWT jwt;
		JsonObject claims;
		try {
			jwt = SignedJWT.parse(jwtString);
			claims = JsonParser.parseString(jwt.getPayload().toString()).getAsJsonObject();
		} catch (ParseException | RuntimeException e) {
			throw error("The Session JWT could not be parsed", e, args("poppy_session_jwt", jwtString));
		}

		if (!JWSAlgorithm.ES256.equals(jwt.getHeader().getAlgorithm())) {
			throw error("The Session JWT 'alg' must be ES256 (section 5.2)", args("header", jwt.getHeader().toJSONObject()));
		}
		if (jwt.getHeader().getType() == null || !PoppyConstants.SESSION_JWT_TYP.equals(jwt.getHeader().getType().getType())) {
			throw error("The Session JWT 'typ' header must be '" + PoppyConstants.SESSION_JWT_TYP + "' (section 5.2)",
				args("header", jwt.getHeader().toJSONObject()));
		}

		verifySignatureAndKid(env, jwt);

		requireClaimEquals(claims, "iss", env.getString("poppy_incoming_client_id"), "the client_id the request authenticated as");
		requireClaimEquals(claims, "aud", env.getString("issuer"), "the Company's identifier (the issuer)");
		for (String name : new String[] {"sid", "sub", "jti"}) {
			String v = PoppyUtil.stringMember(claims, name);
			if (v == null || v.isEmpty()) {
				throw error("The Session JWT has no '" + name + "' string claim, which section 5.2 requires", args("claims", claims));
			}
		}

		String method = env.getString("incoming_request", "method");
		requireClaimEquals(claims, "htm", method, "the request's HTTP method");
		checkHtu(env, claims);

		Long iat = PoppyUtil.longMember(claims, "iat");
		if (iat == null) {
			throw error("The Session JWT has no numeric 'iat' claim, which section 5.2 requires", args("claims", claims));
		}
		long now = Instant.now().getEpochSecond();
		if (Math.abs(now - iat) > ACCEPTANCE_WINDOW_SECONDS) {
			throw error("The Session JWT 'iat' is outside the " + ACCEPTANCE_WINDOW_SECONDS + " second acceptance window the Company applies (section 5.3)",
				args("iat", iat, "now", now, "difference_seconds", now - iat));
		}

		checkAth(env, claims);

		env.putObject("poppy_session_claims", claims);
		logSuccess("The Session JWT is valid", args("claims", claims));
		return env;
	}

	private void verifySignatureAndKid(Environment env, SignedJWT jwt) {
		JsonObject jwkJson = env.getObject("poppy_delegated_public_jwk");
		ECKey key;
		try {
			key = ECKey.parse(jwkJson.toString());
		} catch (ParseException e) {
			throw error("The delegation's 'cnf.jwk' is not a valid EC public key, so it cannot verify an ES256 Session JWT", e, args("jwk", jwkJson));
		}
		try {
			if (!jwt.verify(new ECDSAVerifier(key.toPublicJWK()))) {
				throw error("The Session JWT signature is not valid for the key in the delegation's 'cnf.jwk' (section 5.2)", args("jwk", jwkJson));
			}
			String thumbprint = key.computeThumbprint().toString();
			if (!thumbprint.equals(jwt.getHeader().getKeyID())) {
				throw error("The Session JWT 'kid' is not the RFC 7638 thumbprint of the delegated key",
					args("kid", jwt.getHeader().getKeyID(), "expected", thumbprint));
			}
		} catch (JOSEException e) {
			throw error("The Session JWT signature could not be verified with the delegated key", e, args("jwk", jwkJson));
		}
	}

	private void requireClaimEquals(JsonObject claims, String name, String expected, String what) {
		if (claims.has(name) && !claims.get(name).isJsonPrimitive()) {
			throw error("The Session JWT '" + name + "' claim must be a single string (section 5.2)", args("claims", claims));
		}
		String actual = PoppyUtil.stringMember(claims, name);
		if (actual == null || !actual.equals(expected)) {
			throw error("The Session JWT '" + name + "' claim is not " + what + " (sections 5.2 and 5.3)",
				args(name, actual, "expected", expected));
		}
	}

	private void checkHtu(Environment env, JsonObject claims) {
		String htu = PoppyUtil.stringMember(claims, "htu");
		String requestUrl = env.getString("incoming_request", "request_url");
		if (htu == null) {
			throw error("The Session JWT has no 'htu' string claim, which section 5.2 requires", args("claims", claims));
		}
		if (htu.contains("?") || htu.contains("#")) {
			throw error("The Session JWT 'htu' contains a query or fragment, which section 5.2 forbids", args("htu", htu));
		}
		String normalisedHtu;
		String normalisedRequest;
		try {
			normalisedHtu = normalise(htu);
			normalisedRequest = normalise(requestUrl);
		} catch (URISyntaxException | RuntimeException e) {
			throw error("The Session JWT 'htu' or the request URL is not a valid URL", e, args("htu", htu, "request_url", requestUrl));
		}
		if (!normalisedHtu.equals(normalisedRequest)) {
			throw error("The Session JWT 'htu' does not match the request URL without query or fragment (RFC 9449 section 4.3)",
				args("htu", htu, "request_url", requestUrl));
		}
	}

	/** Scheme and host lowercased, default port dropped, query and fragment removed (RFC 3986 sections 6.2.2 and 6.2.3). */
	private String normalise(String url) throws URISyntaxException {
		URI uri = new URI(url).normalize();
		String scheme = uri.getScheme().toLowerCase(java.util.Locale.ROOT);
		int port = uri.getPort();
		if (("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80)) {
			port = -1;
		}
		String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
		return scheme + "://" + uri.getHost().toLowerCase(java.util.Locale.ROOT) + (port == -1 ? "" : ":" + port) + path;
	}

	private void checkAth(Environment env, JsonObject claims) {
		String presented = env.getString("incoming_access_token");
		String ath = PoppyUtil.stringMember(claims, "ath");
		if (claims.has("ath") && ath == null) {
			throw error("The Session JWT 'ath' claim is not a string", args("claims", claims));
		}
		if (presented == null) {
			if (claims.has("ath")) {
				throw error("The Session JWT has an 'ath' claim but the request carries no access token. Section 5.3 requires 'ath' to be absent without an access token.",
					args("ath", ath));
			}
			return;
		}
		if (ath == null) {
			throw error("The request carries an access token but the Session JWT has no 'ath' claim (section 5.2)", args("claims", claims));
		}
		if (!ath.equals(athOf(presented))) {
			throw error("The Session JWT 'ath' is not the base64url SHA-256 hash of the access token the request carries (section 5.3)",
				args("ath", ath, "expected", athOf(presented)));
		}
		String issued = env.getString("access_token");
		if (!presented.equals(issued)) {
			throw error("The request carries an access token that this Company did not issue to the Agent, so it cannot have been issued to the Session JWT's 'iss' (section 5.3)");
		}
	}

	static String athOf(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
