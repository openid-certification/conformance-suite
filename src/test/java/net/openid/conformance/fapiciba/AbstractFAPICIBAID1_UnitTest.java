package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddBindingMessageToAuthorizationEndpointRequest;
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

	@Test
	public void createAuthorizationRequestAddsBindingMessageForDefaultProfile() {
		TestableFAPICIBAID1 module = new TestableFAPICIBAID1();

		module.createAuthorizationRequest();

		assertThat(module.conditionClasses)
			.contains(AddBindingMessageToAuthorizationEndpointRequest.class);
	}

	@Test
	public void createAuthorizationRequestCanSkipBindingMessageForProfile() {
		TestableFAPICIBAID1 module = new TestableFAPICIBAID1(new NoBindingMessageProfileBehavior());

		module.createAuthorizationRequest();

		assertThat(module.conditionClasses)
			.doesNotContain(AddBindingMessageToAuthorizationEndpointRequest.class);
	}

	private static class TestableFAPICIBAID1 extends AbstractFAPICIBAID1 {

		private final List<String> events = new ArrayList<>();
		private final List<Class<? extends Condition>> conditionClasses = new ArrayList<>();

		TestableFAPICIBAID1() {
			this(new FAPICIBAServerProfileBehavior());
		}

		TestableFAPICIBAID1(FAPICIBAServerProfileBehavior profileBehavior) {
			eventLog = BsonEncoding.testInstanceEventLog();
			this.profileBehavior = profileBehavior;
			this.profileBehavior.setModule(this);
		}

		@Override
		protected void callAndStopOnFailure(Condition condition, String... requirements) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
			conditionClasses.add(conditionClass);
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
		protected void modeSpecificAuthorizationEndpointRequest() {
			// No mode-specific conditions needed for the request-construction tests.
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

	private static class NoBindingMessageProfileBehavior extends FAPICIBAServerProfileBehavior {
		@Override
		public boolean shouldAddBindingMessageToAuthorizationEndpointRequest() {
			return false;
		}
	}
}
