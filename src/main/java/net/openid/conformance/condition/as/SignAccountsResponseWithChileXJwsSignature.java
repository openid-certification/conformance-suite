package net.openid.conformance.condition.as;

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
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.ChileXJwsSignatureUtil;
import net.openid.conformance.util.JWKUtil;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;

/**
 * Signs the accounts endpoint response with the Chile Open Finance x-jws-signature (CMF-XJWS-5.3.1):
 * a detached PS256 JWS whose payload is the exact response body, with a protected header that binds the
 * HTTP status and the request it answers (its x-fapi-interaction-id and x-jws-signature). The request's
 * x-fapi-interaction-id is echoed on the response.
 */
public class SignAccountsResponseWithChileXJwsSignature extends AbstractCondition {

	@Override
	@PreEnvironment(
		required = {ExtractChileResponseSigningJwksFromConfiguration.RESPONSE_SIGNING_JWKS, "accounts_endpoint_response_headers", "incoming_request"},
		strings = SerializeAccountsEndpointResponseBody.ACCOUNTS_RESPONSE_BODY)
	public Environment evaluate(Environment env) {
		RSAKey key = signingKey(env);
		String body = env.getString(SerializeAccountsEndpointResponseBody.ACCOUNTS_RESPONSE_BODY);
		String interactionId = env.getString("incoming_request", "headers.x-fapi-interaction-id");
		String requestSignature = env.getString("incoming_request", "headers." + ChileXJwsSignatureUtil.X_JWS_SIGNATURE);

		JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.PS256)
			.type(JOSEObjectType.JOSE)
			.keyID(key.getKeyID())
			.customParam("ofc.status", 200)
			.customParam("ofc.ts", nowEpochSeconds());
		if (!Strings.isNullOrEmpty(interactionId)) {
			builder.customParam("ofc.interaction", interactionId);
		}
		if (!Strings.isNullOrEmpty(requestSignature)) {
			builder.customParam("ofc.req", ChileXJwsSignatureUtil.s256(requestSignature.getBytes(StandardCharsets.US_ASCII)));
		}
		JWSHeader header = builder.build();

		JWSObject jws = new JWSObject(header, new Payload(body.getBytes(StandardCharsets.UTF_8)));
		try {
			jws.sign(new RSASSASigner(key));
		} catch (JOSEException e) {
			throw error("Unable to sign the accounts response with PS256", e, args("kid", key.getKeyID()));
		}
		String signature = jws.serialize(true);

		JsonObject headers = env.getObject("accounts_endpoint_response_headers");
		headers.addProperty(ChileXJwsSignatureUtil.X_JWS_SIGNATURE, signature);
		if (!Strings.isNullOrEmpty(interactionId)) {
			headers.addProperty("x-fapi-interaction-id", interactionId);
		}

		logSuccess("Signed the accounts response with an x-jws-signature header",
			args("x-jws-signature", signature, "protected_header", header.toJSONObject(), "body", body));
		return env;
	}

	/** Signing time in seconds since the epoch; overridable so tests can fix it. */
	protected long nowEpochSeconds() {
		return Instant.now().getEpochSecond();
	}

	private RSAKey signingKey(Environment env) {
		try {
			JWKSet jwks = JWKUtil.parseJWKSet(env.getObject(ExtractChileResponseSigningJwksFromConfiguration.RESPONSE_SIGNING_JWKS).toString());
			JWK jwk = jwks.getKeys().get(0);
			if (jwk instanceof RSAKey rsaKey && rsaKey.isPrivate()) {
				return rsaKey;
			}
		} catch (ParseException | IndexOutOfBoundsException e) {
			throw error("The response signing key set is not usable", e);
		}
		throw error("The response signing key is not an RSA private key");
	}
}
