package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarnSearchResultsIncludeDeniedFixtureEntities_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnSearchResultsIncludeDeniedFixtureEntities cond(String denied) {
		WarnSearchResultsIncludeDeniedFixtureEntities cond = new WarnSearchResultsIncludeDeniedFixtureEntities(denied);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return cond;
	}

	private void respond(String json) {
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void deniedSubjectReturned_isFlagged() {
		respond("{ \"results\": [ { \"type\": \"user\", \"id\": \"alice\" }, { \"type\": \"user\", \"id\": \"bob\" } ] }");
		WarnSearchResultsIncludeDeniedFixtureEntities cond = cond("[ { \"type\": \"user\", \"id\": \"bob\" } ]");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("fixture denies"), e.getMessage());
	}

	@Test
	public void deniedEntityWithProperties_isFlagged() {
		respond("{ \"results\": [ { \"type\": \"record\", \"id\": \"record-1\", \"properties\": { \"status\": \"active\" } } ] }");
		WarnSearchResultsIncludeDeniedFixtureEntities cond = cond("[ { \"type\": \"record\", \"id\": \"record-1\" } ]");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void deniedActionReturned_isFlagged() {
		respond("{ \"results\": [ { \"name\": \"read\" }, { \"name\": \"write\" } ] }");
		WarnSearchResultsIncludeDeniedFixtureEntities cond = cond("[ { \"name\": \"write\" } ]");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void onlyPermittedAndOtherEntities_succeeds() {
		respond("{ \"results\": [ { \"type\": \"user\", \"id\": \"alice\" }, { \"type\": \"user\", \"id\": \"carol\" } ] }");
		cond("[ { \"type\": \"user\", \"id\": \"bob\" } ]").execute(env);
	}

	@Test
	public void sameIdOfAnotherType_succeeds() {
		respond("{ \"results\": [ { \"type\": \"group\", \"id\": \"bob\" } ] }");
		cond("[ { \"type\": \"user\", \"id\": \"bob\" } ]").execute(env);
	}

	@Test
	public void emptyResults_succeeds() {
		respond("{ \"results\": [] }");
		cond("[ { \"name\": \"write\" } ]").execute(env);
	}

	@Test
	public void missingResults_fails() {
		respond("{ }");
		WarnSearchResultsIncludeDeniedFixtureEntities cond = cond("[ { \"name\": \"write\" } ]");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
