package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractFAPICIBAID1_UnitTest {

	@Test
	public void successfulTokenEndpointResponseRequestsProtectedResourceBeforeFinishing() {
		TestableFAPICIBAID1 module = new TestableFAPICIBAID1();

		module.handleSuccessfulTokenEndpointResponse();

		assertThat(module.events).containsExactly("resource", "finished");
	}

	private static class TestableFAPICIBAID1 extends AbstractFAPICIBAID1 {

		private final List<String> events = new ArrayList<>();

		TestableFAPICIBAID1() {
			eventLog = BsonEncoding.testInstanceEventLog();
			profileBehavior = new FAPICIBAServerProfileBehavior();
			profileBehavior.setModule(this);
		}

		@Override
		protected void callAndStopOnFailure(Condition condition, String... requirements) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void callAndStopOnFailure(
			Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail,
			String... requirements
		) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void callAndContinueOnFailure(
			Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail,
			String... requirements
		) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void skipIfMissing(
			String[] required,
			String[] strings,
			Condition.ConditionResult onSkip,
			Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail,
			String... requirements
		) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void skipIfElementMissing(
			String objId,
			String path,
			Condition.ConditionResult onSkip,
			Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail,
			String... requirements
		) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void call(ConditionCallBuilder builder) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void call(ConditionSequence sequence) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void requestProtectedResource() {
			events.add("resource");
		}

		@Override
		public void fireTestFinished() {
			events.add("finished");
		}
	}
}
