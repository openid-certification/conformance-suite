package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.TestInstanceEventLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies the {@code skipIfStringPresent} predicate added to
 * {@link ConditionCallBuilder} for GitLab issue #1794. The predicate mirrors the
 * existing {@code skipIfStringMissing} — a condition tagged with it must not run
 * when the referenced top-level env string is set.
 *
 * <p>Issue #1794 needs this predicate to gate c_nonce-consuming credential-endpoint
 * conditions on {@code resource_endpoint_dpop_nonce_error}, so a wallet that sent a
 * stale DPoP nonce still has an unburned c_nonce to retry with after seeing the
 * 401-with-fresh-nonce response.
 */
public class ConditionCallBuilder_SkipIfStringPresent_UnitTest {

	private SkipDispatchModule module;

	@BeforeEach
	public void setUp() {
		RecordingCondition.reset();
		module = new SkipDispatchModule();
		TestInstanceEventLog eventLog = mock(TestInstanceEventLog.class);
		TestInfoService infoService = mock(TestInfoService.class);
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test"), eventLog, null, infoService, null, null);
	}

	@Test
	public void skipIfStringPresent_skipsConditionWhenStringIsSet() {
		module.putString("resource_endpoint_dpop_nonce_error", "fresh-nonce");

		module.runConditionGuardedBy("resource_endpoint_dpop_nonce_error");

		assertFalse(RecordingCondition.executed, "condition must be skipped when the named env string is set");
	}

	@Test
	public void skipIfStringPresent_runsConditionWhenStringIsAbsent() {
		// resource_endpoint_dpop_nonce_error not set
		module.runConditionGuardedBy("resource_endpoint_dpop_nonce_error");

		assertTrue(RecordingCondition.executed, "condition must run when the named env string is absent");
	}

	@Test
	public void skipIfStringPresent_skipsWhenAnyOfMultipleStringsIsSet() {
		module.putString("first_gate", "set");
		// second_gate intentionally not set

		module.runConditionGuardedByAnyOf("first_gate", "second_gate");

		assertFalse(RecordingCondition.executed, "condition must skip if ANY of the listed strings is present");
	}

	@Test
	public void skipIfStringPresent_runsWhenNoneOfMultipleStringsIsSet() {
		module.runConditionGuardedByAnyOf("first_gate", "second_gate");

		assertTrue(RecordingCondition.executed, "condition must run when none of the listed strings are present");
	}

	@Test
	public void skipIfStringPresent_independentFromSkipIfStringMissing() {
		// Sanity check that the new predicate does not interfere with the existing
		// skipIfStringMissing predicate — they have opposite semantics.
		module.putString("required_input", "value");
		module.runConditionRequiringStringAndForbiddingString("required_input", "forbidden_gate");

		assertTrue(RecordingCondition.executed,
			"condition runs when required string is present and forbidden string is absent");
	}

	@PublishTestModule(
		testName = "skipIfStringPresent unit test module",
		displayName = "skipIfStringPresent unit test module",
		profile = "UNIT-TEST"
	)
	public static class SkipDispatchModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}

		void putString(String key, String value) {
			env.putString(key, value);
		}

		void runConditionGuardedBy(String envStringName) {
			call(condition(RecordingCondition.class)
				.skipIfStringPresent(envStringName)
				.dontStopOnFailure());
		}

		void runConditionGuardedByAnyOf(String... envStringNames) {
			call(condition(RecordingCondition.class)
				.skipIfStringsPresent(envStringNames)
				.dontStopOnFailure());
		}

		void runConditionRequiringStringAndForbiddingString(String requiredString, String forbiddenString) {
			call(condition(RecordingCondition.class)
				.skipIfStringMissing(requiredString)
				.skipIfStringPresent(forbiddenString)
				.dontStopOnFailure());
		}
	}

	public static class RecordingCondition extends AbstractCondition {
		static boolean executed = false;

		static void reset() {
			executed = false;
		}

		@Override
		public Environment evaluate(Environment env) {
			executed = true;
			logSuccess("recorded");
			return env;
		}
	}
}
