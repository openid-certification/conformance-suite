package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
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
class EnsureSearchPageResultsAreSubsetOfExpected_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureSearchPageResultsAreSubsetOfExpected cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureSearchPageResultsAreSubsetOfExpected();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String expectedResultsJson, String pageResultsJson) {
		JsonObject expected = new JsonObject();
		expected.add("results", JsonParser.parseString(expectedResultsJson).getAsJsonArray());
		env.putObject("authzen_search_endpoint_expected_response", expected);

		JsonObject page = new JsonObject();
		page.add("results", JsonParser.parseString(pageResultsJson).getAsJsonArray());
		env.putObject("authzen_search_endpoint_response", page);
	}

	@Test
	public void pageIsExactSubset_succeeds() {
		put("""
			[ { "name": "view" }, { "name": "edit" }, { "name": "delete" } ]""",
			"""
			[ { "name": "view" }, { "name": "edit" } ]""");
		cond.execute(env);
	}

	@Test
	public void pageIsFullSet_succeeds() {
		put("""
			[ { "name": "view" } ]""",
			"""
			[ { "name": "view" } ]""");
		cond.execute(env);
	}

	@Test
	public void emptyPage_succeeds() {
		put("""
			[ { "name": "view" } ]""",
			"""
			[]""");
		cond.execute(env);
	}

	@Test
	public void pageHasUnexpectedElement_throws() {
		put("""
			[ { "name": "view" } ]""",
			"""
			[ { "name": "view" }, { "name": "delete" } ]""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not in the expected set"));
	}

	@Test
	public void allExpectedEmpty_pageNonEmpty_throws() {
		put("""
			[]""",
			"""
			[ { "name": "view" } ]""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not in the expected set"));
	}
}
