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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsureSearchFixtureEntitiesMatchBaseSearch_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureSearchFixtureEntitiesMatchBaseSearch cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureSearchFixtureEntitiesMatchBaseSearch();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env.putString("authzen_search_base_change", "a context");
		env.putObject("authzen_search_endpoint_expected_response", JsonParser.parseString("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""").getAsJsonObject());
	}

	private void put(String baseJson, String resultsJson) {
		env.putObject("authzen_search_base_results", JsonParser.parseString(baseJson).getAsJsonObject());
		env.putObject("authzen_search_endpoint_aggregated_results", JsonParser.parseString(resultsJson).getAsJsonObject());
	}

	@Test
	public void entityOutsideTheFixtureDiffers_succeeds() {
		// A context may affect a PDP's own policies (scenario 4.6): carol is not a fixture entity.
		put("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" }, { "type": "user", "id": "carol" } ] }""", """
			{ "results": [ { "type": "user", "id": "bob" }, { "type": "user", "id": "alice" } ] }""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void fixtureEntityWithPropertiesOnlyInOneSearch_succeeds() {
		put("""
			{ "results": [ { "type": "user", "id": "alice", "properties": { "role": "user" } }, { "type": "user", "id": "bob" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void fixtureEntityMissingFromTheBaseSearch_isFlagged() {
		put("""
			{ "results": [ { "type": "user", "id": "alice" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("fixture entities in the results differ"), e.getMessage());
	}

	@Test
	public void fixtureEntityRepeatedInOneSearch_isFlagged() {
		put("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void noResultsArray_fails() {
		put("""
			{ "results": [] }""", """
			{ "page": {} }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
