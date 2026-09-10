package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFGenerateWrongStateStreamVerificationSET_UnitTest {

	private static final String STREAM_ID = "stream-1";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFInMemoryEventStore eventStore;

	private JsonObject streamConfig;

	@BeforeEach
	public void setUp() throws Exception {
		RSAKey serverKey = new RSAKeyGenerator(2048).keyID("server-signing-key").keyUse(KeyUse.SIGNATURE).generate();
		env.putObject("server_jwks", JsonParser.parseString(new JWKSet(serverKey).toString(false)).getAsJsonObject());

		streamConfig = new JsonObject();
		JsonObject streams = new JsonObject();
		streams.add(STREAM_ID, streamConfig);
		JsonObject ssf = new JsonObject();
		ssf.addProperty("issuer", "https://transmitter.example.com");
		ssf.add("streams", streams);
		env.putObject("ssf", ssf);

		env.putObject("config", JsonParser.parseString("{\"ssf\":{\"stream\":{\"audience\":\"https://receiver.example.com\"}}}").getAsJsonObject());

		// the verification request being answered
		env.putObject("incoming_request", JsonParser.parseString("{\"body_json\":{\"stream_id\":\"" + STREAM_ID + "\"}}").getAsJsonObject());

		eventStore = new OIDSSFInMemoryEventStore();
	}

	private OIDSSFSecurityEvent generate() {
		List<OIDSSFSecurityEvent> generated = new ArrayList<>();
		OIDSSFGenerateWrongStateStreamVerificationSET condition = new OIDSSFGenerateWrongStateStreamVerificationSET(eventStore, generated::add);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		condition.execute(env);
		assertEquals(1, generated.size());
		return generated.get(0);
	}

	@SuppressWarnings("unchecked")
	private static String verificationState(OIDSSFSecurityEvent event) throws Exception {
		Map<String, Object> events = SignedJWT.parse(event.securityEventToken()).getJWTClaimsSet().getJSONObjectClaim("events");
		Map<String, Object> verification = (Map<String, Object>) events.get(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
		return (String) verification.get("state");
	}

	@Test
	public void echoesADifferentStateWhenTheRequestCarriedOne() throws Exception {
		streamConfig.addProperty("_verification_state", "receiver-state-123");

		OIDSSFSecurityEvent event = generate();

		String state = verificationState(event);
		assertNotNull(state);
		assertNotEquals("receiver-state-123", state);
		assertEquals(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE, event.type());
	}

	@Test
	public void addsAStateWhenTheRequestCarriedNone() throws Exception {
		OIDSSFSecurityEvent event = generate();

		assertNotNull(verificationState(event));
	}

	@Test
	public void enqueuesTheEventAndClearsThePendingVerificationState() throws Exception {
		streamConfig.addProperty("_verification_state", "receiver-state-123");

		OIDSSFSecurityEvent event = generate();

		List<OIDSSFSecurityEvent> queued = eventStore.getQueuedEvents(STREAM_ID);
		assertEquals(1, queued.size());
		assertEquals(event.jti(), queued.get(0).jti());
		assertEquals(event.jti(), SignedJWT.parse(event.securityEventToken()).getJWTClaimsSet().getJWTID());
		assertFalse(streamConfig.has("_verification_state"), "a later verification request must not inherit the state");
	}
}
