package net.openid.conformance.sequence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.FunctionalCondition;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.ConditionSequenceCallBuilder;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestExecutionUnit;
import net.openid.conformance.testmodule.TestFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class Steps_UnitTest {

	private static final List<String> executed = new ArrayList<>();

	private StepsModule module;
	private TestInstanceEventLog eventLog;

	@BeforeEach
	public void setUp() {
		executed.clear();
		module = new StepsModule();
		eventLog = mock(TestInstanceEventLog.class);
		TestInfoService infoService = mock(TestInfoService.class);
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test"), eventLog, null, infoService, null, null);
	}

	private static TestExecutionUnit record(String name) {
		return Steps.action(name, env -> executed.add(name));
	}

	private static ConditionCallBuilder check(String name) {
		return new ConditionCallBuilder(new FunctionalCondition(name, (env, c) -> {
			executed.add(name);
			c.logSuccess(name);
		}));
	}

	@Test
	public void of_runsUnitsInOrder() {
		module.runUnit(Steps.of("steps", record("a"), check("b"), Steps.of("nested", record("c"))));

		assertEquals(List.of("a", "b", "c"), executed);
	}

	@Test
	public void when_runsThenBranchIfPredicateHolds() {
		module.putString("flag", "set");

		module.runUnit(Steps.when("flag is set", env -> env.getString("flag") != null, record("then"), record("otherwise")));

		assertEquals(List.of("then"), executed);
	}

	@Test
	public void when_runsOtherwiseBranchIfPredicateDoesNotHold() {
		module.runUnit(Steps.when("flag is set", env -> env.getString("flag") != null, record("then"), record("otherwise")));

		assertEquals(List.of("otherwise"), executed);
	}

	@Test
	public void when_logsTheSkipIfPredicateDoesNotHoldAndThereIsNoOtherwise() {
		module.runUnit(Steps.when("flag is set", env -> env.getString("flag") != null, record("then")));

		assertEquals(List.of(), executed);
		verify(eventLog).log(eq("flag is set"), anyMap());
	}

	@Test
	public void block_isEndedEvenWhenAStepStopsTheTest() {
		TestExecutionUnit failing = new ConditionCallBuilder(new FunctionalCondition("Fails", (env, c) -> {
			throw c.error("fails");
		}));

		assertThrows(TestFailureException.class, () -> module.runUnit(Steps.block("My block", record("a"), failing)));

		InOrder order = inOrder(eventLog);
		order.verify(eventLog).startBlock("My block");
		order.verify(eventLog).endBlock();
	}

	@Test
	public void forEach_runsTheBodyUnitForEveryElement() {
		module.putObject("config", JsonParser.parseString("{\"list\": [\"x\", \"y\"]}").getAsJsonObject());

		module.runUnit(Steps.forEach("config", "list",
				Steps.action("record", env -> executed.add(env.getString("current"))))
			.currentString("current"));

		assertEquals(List.of("x", "y"), executed);
	}

	@Test
	public void modify_replacesSkipsAndInsertsByNameAndClass() {
		TestExecutionUnit steps = Steps.of("steps", check("a"), new ConditionCallBuilder(RecordingCondition.class), check("c"));

		module.runUnit(Steps.modify(steps)
			.replace("a", record("replaced a"))
			.insertBefore(RecordingCondition.class, record("before b"))
			.insertAfter(RecordingCondition.class, record("after b"))
			.skip("c", "not applicable")
			.butFirst(record("first"))
			.then(record("last"))
			.build());

		assertEquals(List.of("first", "replaced a", "before b", "RecordingCondition", "after b", "last"), executed);
		verify(eventLog).log(eq("c"), anyMap());
	}

	@Test
	public void modify_leavesTheOriginalUnchanged() {
		TestExecutionUnit steps = Steps.of("steps", check("a"));
		TestExecutionUnit modified = Steps.modify(steps).replace("a", record("replaced")).build();

		module.runUnit(modified);
		module.runUnit(steps);

		assertEquals(List.of("replaced", "a"), executed);
	}

	@Test
	public void modify_reachesIntoWhenBranchesBlocksAndForEachBodies() {
		module.putObject("config", JsonParser.parseString("{\"list\": [\"x\"]}").getAsJsonObject());
		TestExecutionUnit steps = Steps.of("steps",
			Steps.when("always", env -> true, check("in when")),
			Steps.block("block", check("in block")),
			Steps.forEach("config", "list", check("in forEach")));

		module.runUnit(Steps.modify(steps)
			.replace("in when", record("when replaced"))
			.replace("in block", record("block replaced"))
			.replace("in forEach", record("forEach replaced"))
			.build());

		assertEquals(List.of("when replaced", "block replaced", "forEach replaced"), executed);
	}

	@Test
	public void modify_reachesIntoLegacySequences() {
		TestExecutionUnit steps = Steps.of("steps", new ConditionSequenceCallBuilder(LegacySequence.class), new LegacySequence());

		module.runUnit(Steps.modify(steps)
			.replace(RecordingCondition.class, record("replaced"))
			.build());

		assertEquals(List.of("legacy a", "replaced", "legacy a", "replaced"), executed);
	}

	@Test
	public void modify_failsWhenTheTargetIsMissing() {
		TestExecutionUnit modified = Steps.modify(Steps.of("steps", check("a")))
			.skip("missing", "not applicable")
			.build();

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> module.runUnit(modified));
		assertTrue(e.getMessage().contains("missing"), e.getMessage());
		assertEquals(List.of(), executed);
	}

	public static class LegacySequence extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			call(check("legacy a", (env, c) -> {
				executed.add("legacy a");
				c.logSuccess("legacy a");
			}));
			call(condition(RecordingCondition.class));
		}
	}

	public static class RecordingCondition extends AbstractCondition {
		@Override
		public Environment evaluate(Environment env) {
			executed.add("RecordingCondition");
			logSuccess("recorded");
			return env;
		}
	}

	@PublishTestModule(
		testName = "steps unit test module",
		displayName = "steps unit test module",
		profile = "UNIT-TEST"
	)
	public static class StepsModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}

		void runUnit(TestExecutionUnit unit) {
			call(unit);
		}

		void putString(String key, String value) {
			env.putString(key, value);
		}

		void putObject(String key, JsonObject value) {
			env.putObject(key, value);
		}
	}
}
