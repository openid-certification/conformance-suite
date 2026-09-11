package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureStreamDeliveryMatchesRequest_UnitTest {

	private static final String PUSH = SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI;

	private static final String POLL = SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureStreamDeliveryMatchesRequest condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFEnsureStreamDeliveryMatchesRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private static JsonObject delivery(String method, String endpointUrl) {
		JsonObject delivery = new JsonObject();
		if (method != null) {
			delivery.addProperty("method", method);
		}
		if (endpointUrl != null) {
			delivery.addProperty("endpoint_url", endpointUrl);
		}
		return delivery;
	}

	private void prepare(JsonObject sentDelivery, JsonObject actualDelivery) {
		JsonObject sent = new JsonObject();
		if (sentDelivery != null) {
			sent.add("delivery", sentDelivery);
		}
		JsonObject stream = new JsonObject();
		stream.addProperty("stream_id", "stream_123");
		if (actualDelivery != null) {
			stream.add("delivery", actualDelivery);
		}
		JsonObject ssf = new JsonObject();
		ssf.add("expected_stream_config", sent);
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);
	}

	@Test
	public void passesWhenThePushDeliveryIsEchoed() {
		prepare(delivery(PUSH, "https://receiver.example.com/push"), delivery(PUSH, "https://receiver.example.com/push"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void passesWhenThePollDeliveryCarriesAnHttpsEndpoint() {
		prepare(delivery(POLL, null), delivery(POLL, "https://transmitter.example.com/events?stream_id=stream_123"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void skipsWhenTheRequestCarriedNoDelivery() {
		prepare(null, delivery(POLL, "https://transmitter.example.com/events"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void failsWhenThePushRequestWasAnsweredWithPoll() {
		prepare(delivery(PUSH, "https://receiver.example.com/push"), delivery(POLL, "https://transmitter.example.com/events"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenThePushEndpointDiffers() {
		prepare(delivery(PUSH, "https://receiver.example.com/push"), delivery(PUSH, "https://receiver.example.com/other"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenThePollEndpointIsMissing() {
		prepare(delivery(POLL, null), delivery(POLL, null));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenThePollEndpointIsNotHttps() {
		prepare(delivery(POLL, null), delivery(POLL, "http://transmitter.example.com/events"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenTheDeliveryObjectIsMissing() {
		prepare(delivery(POLL, null), null);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
