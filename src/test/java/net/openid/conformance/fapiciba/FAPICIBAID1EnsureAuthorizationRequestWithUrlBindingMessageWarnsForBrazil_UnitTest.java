package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddBindingMessageToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessageOrInvalidRequest;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestBindingMessageToUrl;
import net.openid.conformance.condition.client.WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FAPICIBAID1EnsureAuthorizationRequestWithUrlBindingMessageWarnsForBrazil_UnitTest {

	@Test
	public void createAuthorizationRequestAddsUrlBindingMessageAndSkipsDefaultBrazilBindingMessage() {
		TestableModule module = new TestableModule(responseWithoutError());

		module.createAuthorizationRequest();

		assertThat(module.stopCalls)
			.contains(SetAuthorizationEndpointRequestBindingMessageToUrl.class)
			.doesNotContain(AddBindingMessageToAuthorizationEndpointRequest.class);
	}

	@Test
	public void acceptedBackchannelResponseWarnsIfUrlBindingMessageWasAccepted() {
		TestableModule module = new TestableModule(responseWithoutError());

		module.performAuthorizationFlow();

		assertThat(module.events).contains("pre", "request", "validate-success", "post");
		assertThat(module.continueCalls).anySatisfy(call -> {
			assertThat(call.conditionClass)
				.isEqualTo(WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl.class);
			assertThat(call.onFail).isEqualTo(Condition.ConditionResult.WARNING);
		});
		assertThat(module.continueCalls)
			.noneMatch(call -> call.conditionClass
				.equals(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessageOrInvalidRequest.class));
	}

	@Test
	public void errorBackchannelResponseAllowsInvalidBindingMessageOrInvalidRequest() {
		TestableModule module = new TestableModule(responseWithError());

		module.performAuthorizationFlow();

		assertThat(module.events).contains("pre", "request", "validate-error", "finished");
		assertThat(module.continueCalls).anySatisfy(call -> {
			assertThat(call.conditionClass)
				.isEqualTo(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessageOrInvalidRequest.class);
			assertThat(call.onFail).isEqualTo(Condition.ConditionResult.FAILURE);
		});
		assertThat(module.continueCalls)
			.noneMatch(call -> call.conditionClass.equals(WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl.class));
	}

	@Test
	public void genericBindingMessageModulesAreNotApplicableToBrazil() {
		assertThat(isNotApplicableToBrazil(FAPICIBAID1EnsureAuthorizationRequestWithBindingMessageSucceeds.class))
			.isTrue();
		assertThat(isNotApplicableToBrazil(FAPICIBAID1EnsureAuthorizationRequestWithPotentiallyBadBindingMessage.class))
			.isTrue();
	}

	private static boolean isNotApplicableToBrazil(Class<?> testModuleClass) {
		return Arrays.stream(testModuleClass.getAnnotationsByType(VariantNotApplicable.class))
			.anyMatch(annotation -> annotation.parameter().equals(FAPICIBAProfile.class)
				&& Arrays.asList(annotation.values()).contains("openbanking_brazil"));
	}

	private static JsonObject responseWithoutError() {
		JsonObject response = new JsonObject();
		response.addProperty("auth_req_id", "test-auth-req-id");
		return response;
	}

	private static JsonObject responseWithError() {
		JsonObject response = new JsonObject();
		response.addProperty("error", "invalid_request");
		return response;
	}

	private static class TestableModule extends FAPICIBAID1EnsureAuthorizationRequestWithUrlBindingMessageWarnsForBrazil {

		private final JsonObject response;
		private final List<Class<? extends Condition>> stopCalls = new ArrayList<>();
		private final List<ConditionCall> continueCalls = new ArrayList<>();
		private final List<String> events = new ArrayList<>();

		TestableModule(JsonObject response) {
			this.response = response;
			eventLog = BsonEncoding.testInstanceEventLog();
			profileBehavior = new OpenBankingBrazilCibaServerProfileBehavior();
			profileBehavior.setModule(this);
		}

		@Override
		protected void performPreAuthorizationSteps() {
			events.add("pre");
		}

		@Override
		protected void performProfileAuthorizationEndpointSetup() {
			// No profile setup conditions needed for control-flow tests.
		}

		@Override
		protected void modeSpecificAuthorizationEndpointRequest() {
			// No CIBA mode-specific conditions needed for control-flow tests.
		}

		@Override
		protected void performAuthorizationRequest() {
			events.add("request");
			env.putObject("backchannel_authentication_endpoint_response", response);
		}

		@Override
		protected void performValidateAuthorizationResponse() {
			events.add("validate-success");
		}

		@Override
		protected void validateErrorFromBackchannelAuthorizationRequestResponse() {
			events.add("validate-error");
		}

		@Override
		protected void performPostAuthorizationResponse() {
			events.add("post");
		}

		@Override
		public void fireTestFinished() {
			events.add("finished");
		}

		@Override
		protected void callAndStopOnFailure(Condition condition, String... requirements) {
			// No condition execution needed; this test verifies the control-flow handoff.
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
			stopCalls.add(conditionClass);
		}

		@Override
		protected void callAndStopOnFailure(
			Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail,
			String... requirements
		) {
			stopCalls.add(conditionClass);
		}

		@Override
		protected void callAndContinueOnFailure(
			Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail,
			String... requirements
		) {
			continueCalls.add(new ConditionCall(conditionClass, onFail));
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
	}

	private static class ConditionCall {

		private final Class<? extends Condition> conditionClass;
		private final Condition.ConditionResult onFail;

		ConditionCall(Class<? extends Condition> conditionClass, Condition.ConditionResult onFail) {
			this.conditionClass = conditionClass;
			this.onFail = onFail;
		}
	}
}
