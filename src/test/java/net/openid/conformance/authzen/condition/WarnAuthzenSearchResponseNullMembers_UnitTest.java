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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the traversal in {@link AbstractWarnAuthzenNullMembers} through the search response,
 * which has the deepest structure of the three responses.
 */
@ExtendWith(MockitoExtension.class)
class WarnAuthzenSearchResponseNullMembers_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenSearchResponseNullMembers cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenSearchResponseNullMembers();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void putResponse(String json) {
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void noNullMembers_succeeds() {
		putResponse("""
			{ "page": { "next_token": "", "count": 1 },
			  "results": [ { "type": "user", "id": "alice", "properties": { "department": "Sales" } } ] }""");
		cond.execute(env);
	}

	@Test
	public void nullMembersAtEveryDepth_areReportedWithTheirPaths() {
		putResponse("""
			{ "page": null, "context": { "reason": null },
			  "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob", "properties": { "manager": null } } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("whose value is null"), e.getMessage());
		List<String> paths = new ArrayList<>();
		AbstractWarnAuthzenNullMembers.collectNullMembers(env.getObject("authzen_search_endpoint_response"), "$", paths);
		assertTrue(paths.contains("$.page"), paths.toString());
		assertTrue(paths.contains("$.context.reason"), paths.toString());
		assertTrue(paths.contains("$.results[1].properties.manager"), paths.toString());
		assertEquals(3, paths.size(), paths.toString());
	}

	@Test
	public void nullArrayElement_isNotAMember() {
		// Section 11.5 is about properties of JSON objects; an array element that is null is a
		// structural error the response validation reports.
		putResponse("""
			{ "results": [ null ] }""");
		cond.execute(env);
	}
}
