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

	private void put(String baseJson, String resultsJson) {
		env.putObject("authzen_search_base_results", JsonParser.parseString(baseJson).getAsJsonObject());
		env.putObject("authzen_search_endpoint_aggregated_results", JsonParser.parseString(resultsJson).getAsJsonObject());
	}

	@Test
	public void sameResultsInAnotherOrder_succeeds() {
		put("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" }, { "type": "user", "id": "carol" } ] }""", """
			{ "results": [ { "type": "user", "id": "carol" }, { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void sameEntitiesWithDifferentProperties_succeeds() {
		put("""
			{ "results": [ { "type": "record", "id": "record-1", "properties": { "status": "active" } } ] }""", """
			{ "results": [ { "type": "record", "id": "record-1" } ] }""");
		cond.execute(env);
	}

	@Test
	public void droppedNonFixtureEntity_isReported() {
		// The fixture check still passes (alice and bob are there); this is what it cannot see.
		put("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" }, { "type": "user", "id": "carol" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("different results from the same search without a context"), e.getMessage());
	}

	@Test
	public void addedEntity_isReported() {
		put("""
			{ "results": [ { "name": "read" }, { "name": "write" } ] }""", """
			{ "results": [ { "name": "read" }, { "name": "write" }, { "name": "delete" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void differentMultiplicity_isReported() {
		put("""
			{ "results": [ { "name": "read" }, { "name": "read" } ] }""", """
			{ "results": [ { "name": "read" }, { "name": "write" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void bothEmpty_succeeds() {
		put("""
			{ "results": [] }""", """
			{ "results": [] }""");
		cond.execute(env);
	}

	@Test
	public void missingResults_fails() {
		put("""
			{ "results": [] }""", """
			{ }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
