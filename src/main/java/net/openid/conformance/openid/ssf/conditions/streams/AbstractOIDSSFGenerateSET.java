package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public abstract class AbstractOIDSSFGenerateSET extends AbstractCondition {

	protected final OIDSSFEventStore eventStore;

	protected final String eventType;

	public AbstractOIDSSFGenerateSET(OIDSSFEventStore eventStore, String eventType) {
		this.eventStore = eventStore;
		this.eventType = eventType;
	}

	@Override
	@PreEnvironment(required = {"server_jwks", "ssf"})
	public Environment evaluate(Environment env) {

		String streamId = getCurrentStreamId(env);

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			log("Failed to generate verification event token: Could not find stream by stream_id", args("stream_id", streamId));
			return env;
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		String serverIssuer = getIssuer(env);
		String audience = getAudience(env);

		try {
			RSAKey rsaKey = getSigningKey(env);

			JWSSigner signer = new RSASSASigner(rsaKey);

			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
				.type(new JOSEObjectType("secevent+jwt"))
				.keyID(rsaKey.getKeyID())
				.build();

			Instant now = Instant.now();
			Date issueTime = Date.from(now);
			String setJti = UUID.randomUUID().toString();

			JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
				.jwtID(setJti)
				.issuer(serverIssuer)
				.audience(audience)
				.issueTime(issueTime)
				// SSF 1.0 4.1.9: transmitters SHOULD set txn; each emulated SET stems from its own event
				.claim("txn", UUID.randomUUID().toString());

			addSubjectAndEvents(streamId, streamConfig, claimsBuilder);

			JWTClaimsSet claimsSet = claimsBuilder.build();

			SignedJWT signedJWT = new SignedJWT(header, claimsSet);
			signedJWT.sign(signer);

			String setTokenString = postProcessSerializedSecurityEventToken(signedJWT.serialize());

			JsonObject setObject = JWTUtil.jwtStringToJsonObjectForEnvironment(setTokenString);
			setObject.remove("value");
			logSuccess("Created SET for event=%s for stream_id=%s with jti=%s".formatted(eventType, streamId, setJti),
				args("jwt", setTokenString, "decoded_jwt_json", setObject, "jti", setJti));

			afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);
		} catch (ParseException | JOSEException e) {
			throw error("Couldn't create Security Event Token JWT", e);
		}

		return env;
	}

	protected String getCurrentStreamId(Environment env) {
		return env.getString("ssf", "current_stream_id");
	}

	/**
	 * The key the SET is signed with; its key ID becomes the {@code kid} of the JWS header.
	 * Defaults to the first key of the transmitter's {@code server_jwks}, so the receiver can
	 * resolve it via the advertised {@code jwks_uri}. Negative tests override this to sign with
	 * a key the receiver cannot resolve.
	 */
	protected RSAKey getSigningKey(Environment env) throws ParseException, JOSEException {
		JWKSet jwkSet = JWKUtil.parseJWKSet(env.getObject("server_jwks").toString());
		JWK jwk = jwkSet.getKeys().get(0);
		return RSAKey.parse(jwk.toJSONString());
	}

	protected String getIssuer(Environment env) {
		return env.getString("ssf", "issuer");
	}

	protected String getAudience(Environment env) {
		return env.getString("config", "ssf.stream.audience");
	}

	/**
	 * Hook for subclasses that need to alter the serialized SET after signing
	 * (e.g. to deliberately corrupt the signature for negative tests). The
	 * default implementation returns the token unchanged.
	 */
	protected String postProcessSerializedSecurityEventToken(String setTokenString) {
		return setTokenString;
	}

	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
		eventStore.storeEvent(streamId, new OIDSSFSecurityEvent(setJti, setTokenString, eventType));
	}

	protected abstract void addSubjectAndEvents(String streamId, JsonObject streamConfig, JWTClaimsSet.Builder claimsBuilder);
}
