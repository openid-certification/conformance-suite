package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.SkippedCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Covers condition dispatch through {@link ExecutionContext} and functional conditions created with check().
 */
public class FunctionalConditionCall_UnitTest {

	private CallModule module;
	private TestInstanceEventLog eventLog;

	@BeforeEach
	public void setUp() {
		InstanceRecordingCondition.instances.clear();
		module = new CallModule();
		eventLog = mock(TestInstanceEventLog.class);
		TestInfoService infoService = mock(TestInfoService.class);
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test"), eventLog, null, infoService, null, null);
	}

	@Test
	public void continueOnFailure_recordsTheOnFailResult() {
		module.runUnit(module.failingCheck().onFail(ConditionResult.WARNING).dontStopOnFailure());

		assertEquals(TestModule.Result.WARNING, module.getResult());
	}

	@Test
	public void stopOnFailure_throwsTestFailureException() {
		assertThrows(TestFailureException.class, () -> module.runUnit(module.failingCheck()));
	}

	@Test
	public void skipOnUnshadowedMissingObject_recordsTheOnSkipResult() {
		module.runUnit(module.failingCheck()
			.skipIfObjectMissing("not_in_env")
			.onSkip(ConditionResult.WARNING));

		assertEquals(TestModule.Result.WARNING, module.getResult());
		verify(eventLog).log(eq("AlwaysFails"), anyMap());
	}

	@Test
	public void functionalCondition_isLoggedUnderItsName() {
		module.runUnit(module.passingCheck("PassingCheck"));

		verify(eventLog).log(eq("PassingCheck"), anyMap());
	}

	@Test
	public void conditionClass_isInstantiatedForEveryCall() {
		ConditionCallBuilder builder = new ConditionCallBuilder(InstanceRecordingCondition.class);

		module.runUnit(builder);
		module.runUnit(builder);

		assertEquals(2, InstanceRecordingCondition.instances.size());
		assertNotSame(InstanceRecordingCondition.instances.get(0), InstanceRecordingCondition.instances.get(1));
	}

	@Test
	public void key_isTheClassOrTheFunctionalName() {
		assertEquals(InstanceRecordingCondition.class, new ConditionCallBuilder(InstanceRecordingCondition.class).key());
		assertEquals("PassingCheck", module.passingCheck("PassingCheck").key());
	}

	@Test
	public void sequence_canReplaceAndSkipFunctionalConditionsByName() {
		ConditionSequence sequence = new TwoCheckSequence()
			.replace("First", new ConditionCallBuilder(InstanceRecordingCondition.class))
			.skip("Second", "not applicable");

		sequence.evaluate();
		List<TestExecutionUnit> units = sequence.getTestExecutionUnits();

		assertEquals(InstanceRecordingCondition.class, units.get(0).key());
		SkippedCondition skipped = assertInstanceOf(SkippedCondition.class, units.get(1));
		assertEquals("Second", skipped.getSource());
	}

	@Test
	public void sequence_rejectsModificationOfUnknownName() {
		ConditionSequence sequence = new TwoCheckSequence().skip("Third", "not applicable");
		sequence.evaluate();

		RuntimeException e = assertThrows(RuntimeException.class, sequence::getTestExecutionUnits);
		assertTrue(e.getMessage().contains("Third"), e.getMessage());
	}

	@Test
	public void sequence_runsItsUnitsThroughTheModule() {
		module.runUnit(new TwoCheckSequence());

		verify(eventLog).log(eq("First"), anyMap());
		verify(eventLog).log(eq("Second"), anyMap());
	}

	public static class TwoCheckSequence extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			call(check("First", (env, c) -> c.logSuccess("first", Map.of())));
			call(check("Second", (env, c) -> c.logSuccess("second", Map.of())));
		}
	}

	@PublishTestModule(
		testName = "functional condition unit test module",
		displayName = "functional condition unit test module",
		profile = "UNIT-TEST"
	)
	public static class CallModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}

		void runUnit(TestExecutionUnit unit) {
			call(unit);
		}

		ConditionCallBuilder failingCheck() {
			return check("AlwaysFails", (env, c) -> {
				throw c.error("always fails");
			});
		}

		ConditionCallBuilder passingCheck(String name) {
			return check(name, (env, c) -> c.logSuccess("passed", Map.of()));
		}
	}

	public static class InstanceRecordingCondition extends AbstractCondition {
		static final List<InstanceRecordingCondition> instances = new ArrayList<>();

		@Override
		public Environment evaluate(Environment env) {
			instances.add(this);
			logSuccess("recorded");
			return env;
		}
	}
}
