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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureStreamDeliveryDefaultsToPoll_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureStreamDeliveryDefaultsToPoll condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFEnsureStreamDeliveryDefaultsToPoll();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void prepareStream(JsonObject delivery) {
		JsonObject stream = new JsonObject();
		stream.addProperty("stream_id", "stream_123");
		if (delivery != null) {
			stream.add("delivery", delivery);
		}
		JsonObject ssf = new JsonObject();
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);
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

	@Test
	public void shouldPassForPollDeliveryWithHttpsEndpointUrl() {
		prepareStream(delivery(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI, "https://transmitter.example.com/events?stream_id=stream_123"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenDeliveryIsMissing() {
		prepareStream(null);
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("does not contain a 'delivery' object"));
	}

	@Test
	public void shouldFailWhenDeliveryIsNotAnObject() {
		JsonObject stream = new JsonObject();
		stream.addProperty("delivery", SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI);
		JsonObject ssf = new JsonObject();
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenMethodIsPush() {
		prepareStream(delivery(SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI, "https://receiver.example.com/push"));
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("does not default to poll delivery"));
	}

	@Test
	public void shouldFailWhenMethodIsMissing() {
		prepareStream(delivery(null, "https://transmitter.example.com/events"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenEndpointUrlIsMissing() {
		prepareStream(delivery(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI, null));
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("non-empty 'endpoint_url'"));
	}

	@Test
	public void shouldFailWhenMethodIsNotAString() {
		JsonObject delivery = delivery(null, "https://transmitter.example.com/events");
		delivery.addProperty("method", 8936);
		prepareStream(delivery);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenEndpointUrlIsBlank() {
		prepareStream(delivery(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI, "  "));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenEndpointUrlIsNotHttps() {
		prepareStream(delivery(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI, "http://transmitter.example.com/events"));
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("not an https URL"));
	}

	@Test
	public void shouldFailWhenEndpointUrlIsNotAUrl() {
		prepareStream(delivery(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI, "not a url"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
