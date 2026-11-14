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
class EnsureSearchResultsMatchBaseSearch_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureSearchResultsMatchBaseSearch cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureSearchResultsMatchBaseSearch();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env.putString("authzen_search_base_change", "a context");
	}

	private static final String USERS_FIXTURE = """
		{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""";

	private static final String ACTIONS_FIXTURE = """
		{ "results": [ { "name": "read" }, { "name": "write" } ] }""";

	private void put(String fixtureJson, String baseJson, String resultsJson) {
		env.putObject("authzen_search_endpoint_expected_response", JsonParser.parseString(fixtureJson).getAsJsonObject());
		env.putObject("authzen_search_base_results", JsonParser.parseString(baseJson).getAsJsonObject());
		env.putObject("authzen_search_endpoint_aggregated_results", JsonParser.parseString(resultsJson).getAsJsonObject());
	}

	@Test
	public void sameResultsInAnotherOrder_succeeds() {
		put(USERS_FIXTURE, """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" }, { "type": "user", "id": "carol" } ] }""", """
			{ "results": [ { "type": "user", "id": "carol" }, { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void sameEntitiesWithDifferentProperties_succeeds() {
		put(USERS_FIXTURE, """
			{ "results": [ { "type": "user", "id": "carol", "properties": { "status": "active" } } ] }""", """
			{ "results": [ { "type": "user", "id": "carol" } ] }""");
		cond.execute(env);
	}

	@Test
	public void droppedEntityOutsideTheFixture_isReported() {
		put(USERS_FIXTURE, """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" }, { "type": "user", "id": "carol" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("The results outside the fixture differ from those of the same search without a context"), e.getMessage());
	}

	@Test
	public void addedEntityOutsideTheFixture_isReported() {
		put(ACTIONS_FIXTURE, """
			{ "results": [ { "name": "read" }, { "name": "write" } ] }""", """
			{ "results": [ { "name": "read" }, { "name": "write" }, { "name": "delete" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void differentMultiplicityOutsideTheFixture_isReported() {
		put(ACTIONS_FIXTURE, """
			{ "results": [ { "name": "read" }, { "name": "delete" }, { "name": "delete" } ] }""", """
			{ "results": [ { "name": "read" }, { "name": "delete" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void differingFixtureEntity_isLeftToTheFixtureCheck() {
		// EnsureSearchFixtureEntitiesMatchBaseSearch fails this at FAILURE; it is not reported twice.
		put(USERS_FIXTURE, """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "carol" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" }, { "type": "user", "id": "carol" } ] }""");
		cond.execute(env);
	}

	@Test
	public void bothEmpty_succeeds() {
		put(USERS_FIXTURE, """
			{ "results": [] }""", """
			{ "results": [] }""");
		cond.execute(env);
	}

	@Test
	public void missingResults_fails() {
		put(USERS_FIXTURE, """
			{ "results": [] }""", """
			{ }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
