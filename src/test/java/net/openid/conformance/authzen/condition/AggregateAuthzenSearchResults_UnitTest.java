package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AggregateAuthzenSearchResults_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AggregateAuthzenSearchResults cond;

	@BeforeEach
	public void setUp() {
		cond = new AggregateAuthzenSearchResults();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putPageResponse(String resultsJson) {
		JsonObject response = new JsonObject();
		response.add("results", JsonParser.parseString(resultsJson).getAsJsonArray());
		env.putObject("authzen_search_endpoint_response", response);
	}

	private JsonArray aggregated() {
		return env.getObject("authzen_search_endpoint_aggregated_results").getAsJsonArray("results");
	}

	@Test
	public void firstPage_initializesAccumulator() {
		putPageResponse("""
			[
				{ "name": "view" },
				{ "name": "edit" }
			]""");

		cond.execute(env);

		assertEquals(2, aggregated().size());
	}

	@Test
	public void secondPage_appendsToAccumulator() {
		putPageResponse("""
			[
				{ "name": "view" }
			]""");
		cond.execute(env);

		putPageResponse("""
			[
				{ "name": "edit" },
				{ "name": "delete" }
			]""");
		cond.execute(env);

		JsonArray agg = aggregated();
		assertEquals(3, agg.size());
		assertTrue(agg.toString().contains("view"));
		assertTrue(agg.toString().contains("edit"));
		assertTrue(agg.toString().contains("delete"));
	}

	@Test
	public void emptyPage_doesNotChangeAccumulator() {
		putPageResponse("""
			[
				{ "name": "view" }
			]""");
		cond.execute(env);

		putPageResponse("""
			[]""");
		cond.execute(env);

		assertEquals(1, aggregated().size());
	}

	@Test
	public void firstPageEmpty_initializesEmptyAccumulator() {
		putPageResponse("""
			[]""");

		cond.execute(env);

		assertEquals(0, aggregated().size());
	}

	@Test
	public void noResultsArray_throws() {
		env.putObject("authzen_search_endpoint_response", new JsonObject());
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no results array"));
	}
}
