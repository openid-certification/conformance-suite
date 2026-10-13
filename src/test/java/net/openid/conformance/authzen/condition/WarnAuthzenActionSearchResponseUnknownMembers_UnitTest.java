package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class WarnAuthzenActionSearchResponseUnknownMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenActionSearchResponseUnknownMembers cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenActionSearchResponseUnknownMembers();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String json) {
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	private void assertUnknown(String json) {
		put(json);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not define"), e.getMessage());
	}

	@Test
	public void definedMembers_succeed() {
		put("""
			{ "results": [ { "name": "read" }, { "name": "delete", "properties": { "soft": true } } ] }""");
		cond.execute(env);
	}

	@Test
	public void entityShapedResult_warns() {
		assertUnknown("""
			{ "results": [ { "type": "action", "id": "read" } ] }""");
	}
}
