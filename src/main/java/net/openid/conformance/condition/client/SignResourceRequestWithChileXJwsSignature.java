package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.ChileXJwsSignatureUtil;
import net.openid.conformance.util.JWKUtil;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Locale;

/**
 * Signs a resource request with the Chile Open Finance x-jws-signature (CMF-XJWS-5.2.1): a detached
 * PS256 JWS with an empty payload, whose protected header binds the method, path, query, body
 * digest, interaction id and signing time of the request.
 *
 * The path and query are taken from the URI the default RestTemplate sends, which expands the
 * configured URL with {@link DefaultUriBuilderFactory} in {@code URI_COMPONENT} mode, so that the
 * signed values are the ones on the request line.
 */
public class SignResourceRequestWithChileXJwsSignature extends AbstractCondition {

	public static final String X_JWS_SIGNATURE = ChileXJwsSignatureUtil.X_JWS_SIGNATURE;

	/** The exact x-jws-signature value sent, which the response signature's ofc.req is bound to. */
	public static final String SENT_X_JWS_SIGNATURE = "resource_endpoint_request_x_jws_signature";

	@Override
	@PreEnvironment(required = {"client_jwks", "resource_endpoint_request_headers"}, strings = "protected_resource_url")
	@PostEnvironment(strings = SENT_X_JWS_SIGNATURE)
	public Environment evaluate(Environment env) {
		RSAKey key = signingKey(env);

		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		String interactionId = env.getString("resource_endpoint_request_headers", "x-fapi-interaction-id");
		if (Strings.isNullOrEmpty(interactionId)) {
			throw error("The resource request has no x-fapi-interaction-id header to bind into ofc.interaction");
		}

		String url = env.getString("protected_resource_url");
		URI uri = uriAsSent(url);
		String path = Strings.isNullOrEmpty(uri.getRawPath()) ? "/" : uri.getRawPath();
		String method = method(env);
		String body = Strings.nullToEmpty(env.getString("resource_request_entity"));

		JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.PS256)
			.type(JOSEObjectType.JOSE)
			.keyID(key.getKeyID())
			.customParam("ofc.method", method)
			.customParam("ofc.uri", path)
			.customParam("ofc.interaction", interactionId)
			.customParam("ofc.ts", nowEpochSeconds())
			.customParam("ofc.digest", ChileXJwsSignatureUtil.s256(body.getBytes(StandardCharsets.UTF_8)));
		String canonicalQuery = canonicalQuery(uri.getRawQuery());
		if (canonicalQuery != null) {
			builder.customParam("ofc.query", ChileXJwsSignatureUtil.s256(canonicalQuery.getBytes(StandardCharsets.UTF_8)));
		}
		JWSHeader header = builder.build();

		JWSObject jws = new JWSObject(header, new Payload(new byte[0]));
		try {
			jws.sign(new RSASSASigner(key));
		} catch (JOSEException e) {
			throw error("Unable to sign the resource request with PS256", e, args("kid", key.getKeyID()));
		}
		String signature = jws.serialize(true);

		headers.addProperty(X_JWS_SIGNATURE, signature);
		env.putString(SENT_X_JWS_SIGNATURE, signature);

		logSuccess("Signed the resource request with an x-jws-signature header",
			args("x-jws-signature", signature,
				"protected_header", header.toJSONObject(),
				"canonical_query", canonicalQuery,
				"body", body));
		return env;
	}

	/** Signing time in seconds since the epoch; overridable so tests can fix it. */
	protected long nowEpochSeconds() {
		return Instant.now().getEpochSecond();
	}

	private RSAKey signingKey(Environment env) {
		JWKSet jwks;
		try {
			jwks = JWKUtil.parseJWKSet(env.getObject("client_jwks").toString());
		} catch (ParseException e) {
			throw error("The client JWKS is not a valid JSON Web Key Set", e);
		}
		for (JWK jwk : jwks.getKeys()) {
			if (jwk instanceof RSAKey rsaKey && rsaKey.isPrivate()
				&& (rsaKey.getKeyUse() == null || KeyUse.SIGNATURE.equals(rsaKey.getKeyUse()))) {
				return rsaKey;
			}
		}
		throw error("The client JWKS has no RSA private signing key. The Chile x-jws-signature is PS256 and must be "
			+ "signed with an RSA key from the client's registered JWKS ('jwks' field in the 'Client' section of the test configuration).");
	}

	private String method(Environment env) {
		String configured = env.getString("resource", "resourceMethod");
		return Strings.isNullOrEmpty(configured) ? "GET" : configured.toUpperCase(Locale.ROOT);
	}

	private URI uriAsSent(String url) {
		DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);
		try {
			return factory.expand(url);
		} catch (IllegalArgumentException e) {
			throw error("The resource URL is not a valid URI", e, args("url", url));
		}
	}

	/** The canonical query of a raw query, or null when it has no parameters. */
	String canonicalQuery(String rawQuery) {
		try {
			return ChileXJwsSignatureUtil.canonicalQuery(ChileXJwsSignatureUtil.decodeFormPairs(rawQuery));
		} catch (IllegalArgumentException e) {
			throw error("The resource URL query cannot be decoded as application/x-www-form-urlencoded", e,
				args("query", rawQuery));
		}
	}
}
