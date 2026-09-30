package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureStreamListContainsStream_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureStreamListContainsStream createCondition(boolean expectPresent) {
		OIDSSFEnsureStreamListContainsStream condition = new OIDSSFEnsureStreamListContainsStream(expectPresent);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void prepare(String streamListJson) {
		JsonObject ssf = new JsonObject();
		JsonObject stream = new JsonObject();
		stream.addProperty("stream_id", "s1");
		ssf.add("stream", stream);
		if (streamListJson != null) {
			ssf.add("stream_list", JsonParser.parseString(streamListJson));
		}
		env.putObject("ssf", ssf);
	}

	@Test
	void passesWhenExpectedStreamIsListed() {
		prepare("[{\"stream_id\":\"s0\"},{\"stream_id\":\"s1\"}]");
		assertDoesNotThrow(() -> createCondition(true).execute(env));
	}

	@Test
	void failsWhenExpectedStreamIsNotListed() {
		prepare("[{\"stream_id\":\"s0\"}]");
		assertThrows(ConditionError.class, () -> createCondition(true).execute(env));
	}

	@Test
	void passesForEmptyListAfterDeletion() {
		prepare("[]");
		assertDoesNotThrow(() -> createCondition(false).execute(env));
	}

	@Test
	void passesForListOfOtherStreamsAfterDeletion() {
		prepare("[{\"stream_id\":\"s0\"}]");
		assertDoesNotThrow(() -> createCondition(false).execute(env));
	}

	@Test
	void failsWhenDeletedStreamIsStillListed() {
		prepare("[{\"stream_id\":\"s1\"}]");
		assertThrows(ConditionError.class, () -> createCondition(false).execute(env));
	}

	@Test
	void failsWhenResponseIsNotAList() {
		prepare(null);
		assertThrows(ConditionError.class, () -> createCondition(true).execute(env));
	}

	@Test
	void failsWhenListEntryIsNotAnObject() {
		prepare("[\"s1\"]");
		assertThrows(ConditionError.class, () -> createCondition(true).execute(env));
	}
}
