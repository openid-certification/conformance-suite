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
class WarnAuthzenEntitySearchResponseUnknownMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenEntitySearchResponseUnknownMembers cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenEntitySearchResponseUnknownMembers();
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
			{ "page": { "next_token": "", "count": 2, "total": 2, "properties": {} }, "context": {}, "results": [ { "type": "user", "id": "alice", "properties": { "department": "sales" } }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void unknownTopLevelMember_warns() {
		assertUnknown("""
			{ "results": [], "total": 0 }""");
	}

	@Test
	public void unknownPageMember_warns() {
		assertUnknown("""
			{ "page": { "next_token": "", "limit": 10 }, "results": [] }""");
	}

	@Test
	public void unknownEntityMember_warns() {
		assertUnknown("""
			{ "results": [ { "type": "user", "id": "alice", "name": "Alice" } ] }""");
	}

	@Test
	public void actionShapedResult_warns() {
		assertUnknown("""
			{ "results": [ { "name": "read" } ] }""");
	}
}
