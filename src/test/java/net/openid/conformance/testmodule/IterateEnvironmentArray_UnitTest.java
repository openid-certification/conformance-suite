package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.sequence.AbstractConditionSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class IterateEnvironmentArray_UnitTest {

	private TestIterationModule module;
	private TestInstanceEventLog eventLog;

	@BeforeEach
	public void setUp() {
		RecordCurrentStringCondition.reset();
		RecordCurrentObjectCondition.reset();

		module = new TestIterationModule();
		eventLog = mock(TestInstanceEventLog.class);
		TestInfoService infoService = mock(TestInfoService.class);
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test"), eventLog, null, infoService, null, null);
	}

	@Test
	public void iterateEnvironmentArray_runsSubSequenceForEachStringElement() {
		module.putObject("credentials", JsonParser.parseString("""
			{
				"list": ["alpha", "beta", "gamma"]
			}
			""").getAsJsonObject());

		module.runUnit(new IterateEnvironmentArray("credentials", "list", RecordCurrentStringSequence.class)
			.currentString("credential")
			.iterationIndex("iteration_index")
			.iterationCount("iteration_count"));

		assertEquals(List.of("alpha", "beta", "gamma"), RecordCurrentStringCondition.seenValues);
		assertEquals(List.of(1, 2, 3), RecordCurrentStringCondition.seenIndexes);
		assertEquals(List.of(3, 3, 3), RecordCurrentStringCondition.seenCounts);
	}

	@Test
	public void iterateEnvironmentArray_supportsObjectBindingAndPerIterationLogLabels() {
		module.putObject("items", JsonParser.parseString("""
			{
				"list": [
					{ "name": "first" },
					{ "name": "second" }
				]
			}
			""").getAsJsonObject());

		module.runUnit(new IterateEnvironmentArray("items", "list", RecordCurrentObjectSequence.class)
			.currentElement("current", "item")
			.logBlockLabels(ctx -> "Validate item " + ctx.getIteration() + "/" + ctx.getIterationCount()
				+ " (" + OIDFJSON.getString(ctx.getElement().getAsJsonObject().get("name")) + ")"));

		assertEquals(List.of("first", "second"), RecordCurrentObjectCondition.seenNames);

		InOrder orderedLog = inOrder(eventLog);
		orderedLog.verify(eventLog).startBlock("Validate item 1/2 (first)");
		orderedLog.verify(eventLog).endBlock();
		orderedLog.verify(eventLog).startBlock("Validate item 2/2 (second)");
		orderedLog.verify(eventLog).endBlock();
		verify(eventLog, times(2)).endBlock();
	}

	@Test
	public void iterateEnvironmentArray_skipsBodyForEmptyArray() {
		module.putObject("credentials", JsonParser.parseString("""
			{
				"list": []
			}
			""").getAsJsonObject());

		module.runUnit(new IterateEnvironmentArray("credentials", "list", RecordCurrentStringSequence.class)
			.currentString("credential")
			.iterationIndex("iteration_index")
			.iterationCount("iteration_count")
			.logBlockLabels(ctx -> "Validate item " + ctx.getIteration()));

		assertEquals(List.of(), RecordCurrentStringCondition.seenValues);
		verify(eventLog, never()).startBlock(org.mockito.ArgumentMatchers.anyString());

		// no iteration ran, so no keys were written and none should exist after
		assertNull(module.getStringValue("credential"));
		assertNull(module.getIntegerValue("iteration_index"));
		assertNull(module.getIntegerValue("iteration_count"));
	}

	@Test
	public void iterateEnvironmentArray_rejectsMissingSourcePath() {
		module.putObject("credentials", JsonParser.parseString("""
			{
				"other": []
			}
			""").getAsJsonObject());

		TestFailureException exception = assertThrows(TestFailureException.class,
			() -> module.runUnit(new IterateEnvironmentArray("credentials", "list", RecordCurrentStringSequence.class)));

		assertEquals("Missing environment array for iteration at credentials.list", exception.getMessage());
	}

	@Test
	public void iterateEnvironmentArray_clearsIterationStateAfterLoopCompletes() {
		module.putObject("credentials", JsonParser.parseString("""
			{
				"list": ["alpha", "beta"]
			}
			""").getAsJsonObject());
		module.putObject("current", JsonParser.parseString("""
			{
				"sibling": "kept"
			}
			""").getAsJsonObject());

		module.runUnit(new IterateEnvironmentArray("credentials", "list", RecordCurrentStringSequence.class)
			.currentString("credential")
			.iterationIndex("iteration_index")
			.iterationCount("iteration_count"));

		assertNull(module.getStringValue("credential"));
		assertNull(module.getIntegerValue("iteration_index"));
		assertNull(module.getIntegerValue("iteration_count"));
	}

	@Test
	public void iterateEnvironmentArray_clearsCurrentElementLeafButLeavesSiblings() {
		module.putObject("items", JsonParser.parseString("""
			{
				"list": [
					{ "name": "first" }
				]
			}
			""").getAsJsonObject());
		module.putObject("current", JsonParser.parseString("""
			{
				"sibling": "kept"
			}
			""").getAsJsonObject());

		module.runUnit(new IterateEnvironmentArray("items", "list", RecordCurrentObjectSequence.class)
			.currentElement("current", "item"));

		JsonObject current = module.getObject("current");
		assertEquals(false, current.has("item"));
		assertEquals("kept", OIDFJSON.getString(current.get("sibling")));
	}

	@Test
	public void iterateEnvironmentArray_rejectsNonArraySources() {
		module.putObject("items", JsonParser.parseString("""
			{
				"list": {
					"name": "not-an-array"
				}
			}
			""").getAsJsonObject());

		TestFailureException exception = assertThrows(TestFailureException.class,
			() -> module.runUnit(new IterateEnvironmentArray("items", "list", RecordCurrentStringSequence.class)));

		assertEquals("Expected environment array for iteration at items.list", exception.getMessage());
	}

	@PublishTestModule(
		testName = "Iteration Unit Test Module",
		displayName = "Iteration Unit Test Module",
		profile = "UNIT-TEST"
	)
	public static class TestIterationModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}

		void runUnit(TestExecutionUnit unit) {
			call(unit);
		}

		void putObject(String key, JsonObject value) {
			env.putObject(key, value);
		}

		JsonObject getObject(String key) {
			return env.getObject(key);
		}

		String getStringValue(String key) {
			return env.getString(key);
		}

		Integer getIntegerValue(String key) {
			return env.getInteger(key);
		}
	}

	public static class RecordCurrentStringSequence extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			call(condition(RecordCurrentStringCondition.class));
		}
	}

	public static class RecordCurrentStringCondition extends AbstractCondition {
		private static final List<String> seenValues = new ArrayList<>();
		private static final List<Integer> seenIndexes = new ArrayList<>();
		private static final List<Integer> seenCounts = new ArrayList<>();

		static void reset() {
			seenValues.clear();
			seenIndexes.clear();
			seenCounts.clear();
		}

		public Environment evaluate(Environment env) {
			seenValues.add(env.getString("credential"));
			seenIndexes.add(env.getInteger("iteration_index"));
			seenCounts.add(env.getInteger("iteration_count"));
			return env;
		}
	}

	public static class RecordCurrentObjectSequence extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			call(condition(RecordCurrentObjectCondition.class));
		}
	}

	public static class RecordCurrentObjectCondition extends AbstractCondition {
		private static final List<String> seenNames = new ArrayList<>();

		static void reset() {
			seenNames.clear();
		}

		public Environment evaluate(Environment env) {
			seenNames.add(env.getString("current", "item.name"));
			return env;
		}
	}
}
