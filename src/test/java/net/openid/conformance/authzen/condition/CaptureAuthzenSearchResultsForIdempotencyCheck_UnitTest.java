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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CaptureAuthzenSearchResultsForIdempotencyCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CaptureAuthzenSearchResultsForIdempotencyCheck cond;

	@BeforeEach
	public void setUp() {
		cond = new CaptureAuthzenSearchResultsForIdempotencyCheck();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void aggregatedResults_areCopied() {
		env.putObject("authzen_search_endpoint_aggregated_results", JsonParser.parseString("""
			{ "results": [ { "type": "user", "id": "alice" } ] }""").getAsJsonObject());
		cond.execute(env);
		// Later pages and iterations add to the aggregated array, so the capture must not share it.
		env.getObject("authzen_search_endpoint_aggregated_results").getAsJsonArray("results")
			.add(JsonParser.parseString("{ \"type\": \"user\", \"id\": \"bob\" }"));
		assertEquals(1, env.getObject("authzen_idempotency_first_search_results").getAsJsonArray("results").size());
	}

	@Test
	public void aggregatedWithoutResults_fails() {
		env.putObject("authzen_search_endpoint_aggregated_results", JsonParser.parseString("{}").getAsJsonObject());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
