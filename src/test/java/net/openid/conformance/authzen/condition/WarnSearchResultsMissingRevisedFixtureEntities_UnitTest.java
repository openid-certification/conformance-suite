package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarnSearchResultsMissingRevisedFixtureEntities_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnSearchResultsMissingRevisedFixtureEntities cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnSearchResultsMissingRevisedFixtureEntities("[ { \"type\": \"record\", \"id\": \"record-2\" } ]");
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void respond(String json) {
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void revisedEntityPresent_succeeds() {
		respond("{ \"results\": [ { \"type\": \"record\", \"id\": \"record-1\" }, { \"type\": \"record\", \"id\": \"record-2\", \"properties\": { \"x\": 1 } } ] }");
		cond.execute(env);
	}

	@Test
	public void revisedEntityMissing_isFlagged() {
		respond("{ \"results\": [ { \"type\": \"record\", \"id\": \"record-1\" } ] }");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("lack an entity that the revised certification fixture adds"), e.getMessage());
	}

	@Test
	public void noResultsArray_fails() {
		respond("{}");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
