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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(MockitoExtension.class)
public class OIDSSFGenerateWrongSubjectStreamVerificationSET_UnitTest {

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
		streamConfig.addProperty("_verification_state", "receiver-state-123");
		JsonObject streams = new JsonObject();
		streams.add(STREAM_ID, streamConfig);
		JsonObject ssf = new JsonObject();
		ssf.addProperty("issuer", "https://transmitter.example.com");
		ssf.add("streams", streams);
		env.putObject("ssf", ssf);

		env.putObject("config", JsonParser.parseString("{\"ssf\":{\"stream\":{\"audience\":\"https://receiver.example.com\"}}}").getAsJsonObject());
		env.putObject("incoming_request", JsonParser.parseString("{\"body_json\":{\"stream_id\":\"" + STREAM_ID + "\"}}").getAsJsonObject());

		eventStore = new OIDSSFInMemoryEventStore();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void namesAnotherStreamInTheSubjectAndEchoesTheState() throws Exception {
		List<OIDSSFSecurityEvent> generated = new ArrayList<>();
		OIDSSFGenerateWrongSubjectStreamVerificationSET condition = new OIDSSFGenerateWrongSubjectStreamVerificationSET(eventStore, generated::add);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		condition.execute(env);

		assertEquals(1, generated.size());
		OIDSSFSecurityEvent event = generated.get(0);
		assertEquals(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE, event.type());

		SignedJWT jwt = SignedJWT.parse(event.securityEventToken());
		Map<String, Object> subId = jwt.getJWTClaimsSet().getJSONObjectClaim("sub_id");
		assertEquals("opaque", subId.get("format"));
		assertNotEquals(STREAM_ID, subId.get("id"));

		Map<String, Object> events = jwt.getJWTClaimsSet().getJSONObjectClaim("events");
		Map<String, Object> verification = (Map<String, Object>) events.get(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
		assertEquals("receiver-state-123", verification.get("state"), "the state is echoed correctly; the subject is the only defect");
		assertEquals(1, eventStore.getQueuedEvents(STREAM_ID).size(), "the SET is queued for the stream being verified");
	}
}
