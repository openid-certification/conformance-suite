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
public class OIDSSFEnsurePollEndpointUrlsDifferPerStream_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsurePollEndpointUrlsDifferPerStream condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFEnsurePollEndpointUrlsDifferPerStream();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private static JsonObject stream(String streamId, String endpointUrl) {
		JsonObject stream = new JsonObject();
		stream.addProperty("stream_id", streamId);
		JsonObject delivery = new JsonObject();
		delivery.addProperty("method", SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI);
		if (endpointUrl != null) {
			delivery.addProperty("endpoint_url", endpointUrl);
		}
		stream.add("delivery", delivery);
		return stream;
	}

	private void prepareStreams(JsonObject first, JsonObject second) {
		JsonObject ssf = new JsonObject();
		if (first != null) {
			ssf.add("first_stream", first);
		}
		if (second != null) {
			ssf.add("second_stream", second);
		}
		env.putObject("ssf", ssf);
	}

	@Test
	public void shouldPassWhenEndpointUrlsDiffer() {
		prepareStreams(
			stream("stream_1", "https://transmitter.example.com/events?stream_id=stream_1"),
			stream("stream_2", "https://transmitter.example.com/events?stream_id=stream_2"));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenEndpointUrlsAreEqual() {
		prepareStreams(
			stream("stream_1", "https://transmitter.example.com/events"),
			stream("stream_2", "https://transmitter.example.com/events"));
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("unique per stream"));
	}

	@Test
	public void shouldFailWhenSecondCreateReturnedTheSameStream() {
		prepareStreams(
			stream("stream_1", "https://transmitter.example.com/events?stream_id=stream_1"),
			stream("stream_1", "https://transmitter.example.com/events?stream_id=stream_1"));
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("returned the same stream again"));
	}

	@Test
	public void shouldFailWhenSecondStreamHasNoEndpointUrl() {
		prepareStreams(
			stream("stream_1", "https://transmitter.example.com/events?stream_id=stream_1"),
			stream("stream_2", null));
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("second stream"));
	}

	@Test
	public void shouldFailWhenFirstStreamHasNoDelivery() {
		JsonObject first = new JsonObject();
		first.addProperty("stream_id", "stream_1");
		prepareStreams(first, stream("stream_2", "https://transmitter.example.com/events?stream_id=stream_2"));
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("first stream"));
	}

	@Test
	public void shouldFailWhenSecondStreamIsMissing() {
		prepareStreams(stream("stream_1", "https://transmitter.example.com/events?stream_id=stream_1"), null);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
