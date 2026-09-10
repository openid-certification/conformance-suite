package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFInMemoryEventStore;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class OIDSSFGenerateStreamStatusUpdatedSET_UnitTest {

	private static final String STREAM_ID = "stream-1";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFInMemoryEventStore eventStore;

	private final List<String> enqueuedJtis = new ArrayList<>();

	@BeforeEach
	void setUp() throws Exception {
		eventStore = new OIDSSFInMemoryEventStore();

		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		RSAKey key = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
			.privateKey((RSAPrivateKey) keyPair.getPrivate())
			.keyUse(KeyUse.SIGNATURE)
			.keyID("k1")
			.build();
		env.putObject("server_jwks", JsonParser.parseString(new JWKSet(key).toString(false)).getAsJsonObject());

		JsonObject config = new JsonObject();
		config.addProperty("ssf.stream.audience", "https://receiver.example.com");
		env.putObject("config", config);

		JsonObject ssf = new JsonObject();
		ssf.addProperty("issuer", "https://transmitter.example.com");
		ssf.add("streams", new JsonObject());
		env.putObject("ssf", ssf);
	}

	private void prepareStream(String status, String reason) {
		JsonObject streamConfig = new JsonObject();
		streamConfig.addProperty("stream_id", STREAM_ID);
		JsonObject streamStatus = new JsonObject();
		streamStatus.addProperty("stream_id", STREAM_ID);
		streamStatus.addProperty("status", status);
		if (reason != null) {
			streamStatus.addProperty("reason", reason);
		}
		streamConfig.add("_status", streamStatus);
		env.getObject("ssf").getAsJsonObject("streams").add(STREAM_ID, streamConfig);
	}

	private <T extends OIDSSFGenerateStreamStatusUpdatedSET> T configure(T condition) {
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private JWTClaimsSet generatedClaims() throws Exception {
		assertEquals(1, enqueuedJtis.size());
		OIDSSFSecurityEvent event = eventStore.getRegisteredSecurityEvent(STREAM_ID, enqueuedJtis.get(0));
		assertNotNull(event, "the generated SET is stored for the stream");
		assertEquals(SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE, event.type());
		return SignedJWT.parse(event.securityEventToken()).getJWTClaimsSet();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> streamUpdatedEvent(JWTClaimsSet claims) throws Exception {
		Map<String, Object> events = claims.getJSONObjectClaim("events");
		assertEquals(1, events.size(), "exactly one event in the SET");
		return (Map<String, Object>) events.get(SsfEvents.SSF_STREAM_UPDATED_EVENT_TYPE);
	}

	@Test
	void reportsStatusAndReasonWithStreamAsSubject() throws Exception {
		prepareStream("paused", "Receiver requested a pause");

		configure(new OIDSSFGenerateStreamStatusUpdatedSET(eventStore, STREAM_ID, (streamId, jti) -> enqueuedJtis.add(jti))).execute(env);

		JWTClaimsSet claims = generatedClaims();
		assertEquals(Map.of("format", "opaque", "id", STREAM_ID), claims.getJSONObjectClaim("sub_id"));
		assertEquals(Map.of("status", "paused", "reason", "Receiver requested a pause"), streamUpdatedEvent(claims));
	}

	@Test
	void omitsReasonWhenStreamStatusHasNone() throws Exception {
		prepareStream("enabled", null);

		configure(new OIDSSFGenerateStreamStatusUpdatedSET(eventStore, STREAM_ID, (streamId, jti) -> enqueuedJtis.add(jti))).execute(env);

		assertEquals(Map.of("status", "enabled"), streamUpdatedEvent(generatedClaims()));
	}

	@Test
	void usesStreamIdOfIncomingRequestWhenNoneGiven() throws Exception {
		prepareStream("enabled", null);
		JsonObject body = new JsonObject();
		body.addProperty("stream_id", STREAM_ID);
		JsonObject request = new JsonObject();
		request.add("body_json", body);
		env.putObject("incoming_request", request);

		configure(new OIDSSFGenerateStreamStatusUpdatedSET(eventStore)).execute(env);

		assertEquals(1, eventStore.getQueuedEvents(STREAM_ID).size());
	}

	@Test
	void unknownMemberVariantAddsExactlyOneUndefinedMember() throws Exception {
		prepareStream("enabled", "Receiver enabled the stream");

		configure(new OIDSSFGenerateStreamStatusUpdatedSETWithUnknownMember(eventStore, STREAM_ID, (streamId, jti) -> enqueuedJtis.add(jti))).execute(env);

		Map<String, Object> event = streamUpdatedEvent(generatedClaims());
		assertEquals(Map.of(
			"status", "enabled",
			"reason", "Receiver enabled the stream",
			SsfEvents.UNKNOWN_EVENT_MEMBER_NAME, SsfEvents.UNKNOWN_EVENT_MEMBER_VALUE), event);
		assertFalse(event.containsKey("stream_id"), "the stream id is carried by sub_id only");
	}
}
