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

	public AbstractOIDSSFGenerateSET(OIDSSFEventStore eventStore) {
		this.eventStore = eventStore;
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

		String serverIssuer = env.getString("ssf", "issuer");
		String audience = env.getString("config", "ssf.stream.audience");

		try {
			JWKSet jwkSet = JWKUtil.parseJWKSet(env.getObject("server_jwks").toString());
			JWK jwk = jwkSet.getKeys().get(0);
			RSAKey rsaKey = RSAKey.parse(jwk.toJSONString());

			JWSSigner signer = new RSASSASigner(rsaKey);

			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
				.type(new JOSEObjectType("secevent+jwt"))
				.keyID(jwk.getKeyID())
				.build();

			Instant now = Instant.now();
			Date issueTime = Date.from(now);
			String setJti = UUID.randomUUID().toString();

			JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
				.jwtID(setJti)
				.issuer(serverIssuer)
				.audience(audience)
				.issueTime(issueTime);

			addSubjectAndEvents(streamId, streamConfig, claimsBuilder);

			JWTClaimsSet claimsSet = claimsBuilder.build();

			SignedJWT signedJWT = new SignedJWT(header, claimsSet);
			signedJWT.sign(signer);

			String setTokenString = signedJWT.serialize();

			JsonObject setObject = JWTUtil.jwtStringToJsonObjectForEnvironment(setTokenString);
			setObject.remove("jwt");
			logSuccess("Created SET for event for stream_id=" + streamId + " with jti=" + setJti, args("jwt", setTokenString, "decoded_jwt_json", setObject, "jti", setJti));

			afterSecurityEventTokenGenerated(env, streamId, streamConfig, setJti, setTokenString, setObject);
		} catch (ParseException | JOSEException e) {
			throw error("Couldn't create Security Event Token JWT", e);
		}

		return env;
	}

	protected String getCurrentStreamId(Environment env) {
		return env.getString("ssf", "current_stream_id");
	}

	protected void afterSecurityEventTokenGenerated(Environment env, String streamId, JsonObject streamConfig, String setJti, String setTokenString, JsonObject setObject) {
	}

	protected abstract void addSubjectAndEvents(String streamId, JsonObject streamConfig, JWTClaimsSet.Builder claimsBuilder);
}
