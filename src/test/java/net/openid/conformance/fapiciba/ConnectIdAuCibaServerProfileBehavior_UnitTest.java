package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddEssentialTxnClaimRequestToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.SetConnectIdBindingMessageToPurpose;
import net.openid.conformance.condition.client.SetConnectIdCibaLoginHintFromConfiguration;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.TestExecutionUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectIdAuCibaServerProfileBehavior_UnitTest {

	@Test
	public void testDefaultAuthorizationEndpointSetupIncludesLoginHintAndCommonSetup() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(
			new ConnectIdAuCibaServerProfileBehavior.AuthorizationEndpointSetupSteps());

		assertThat(conditionClasses)
			.startsWith(SetConnectIdCibaLoginHintFromConfiguration.class)
			.contains(SetConnectIdBindingMessageToPurpose.class)
			.contains(AddEssentialTxnClaimRequestToAuthorizationEndpointRequest.class);
	}

	@Test
	public void testCommonAuthorizationEndpointSetupDoesNotRequireLoginHintFromConfiguration() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(
			new ConnectIdAuCibaServerProfileBehavior.CommonAuthorizationEndpointSetupSteps());

		assertThat(conditionClasses)
			.doesNotContain(SetConnectIdCibaLoginHintFromConfiguration.class)
			.contains(SetConnectIdBindingMessageToPurpose.class)
			.contains(AddEssentialTxnClaimRequestToAuthorizationEndpointRequest.class);
	}

	@Test
	public void testResourceEndpointHeadersIncludeInteractionIdForFirstAndSecondClient() {
		ConnectIdAuCibaServerProfileBehavior behavior = new ConnectIdAuCibaServerProfileBehavior();

		List<Class<? extends Condition>> firstClientConditionClasses = getConditionClasses(
			behavior.addResourceEndpointProfileHeaders(false));
		List<Class<? extends Condition>> secondClientConditionClasses = getConditionClasses(
			behavior.addResourceEndpointProfileHeaders(true));

		assertThat(firstClientConditionClasses)
			.containsExactly(
				AddFAPIAuthDateToResourceEndpointRequest.class,
				CreateRandomFAPIInteractionId.class,
				AddFAPIInteractionIdToResourceEndpointRequest.class);
		assertThat(secondClientConditionClasses)
			.containsExactly(
				CreateRandomFAPIInteractionId.class,
				AddFAPIInteractionIdToResourceEndpointRequest.class);
	}

	@Test
	public void testResourceEndpointResponseHeadersValidateMatchingInteractionIdForFirstAndSecondClient() {
		ConnectIdAuCibaServerProfileBehavior behavior = new ConnectIdAuCibaServerProfileBehavior();

		List<Class<? extends Condition>> firstClientConditionClasses = getConditionClasses(
			behavior.validateResourceEndpointResponseHeaders(false));
		List<Class<? extends Condition>> secondClientConditionClasses = getConditionClasses(
			behavior.validateResourceEndpointResponseHeaders(true));

		assertThat(firstClientConditionClasses)
			.containsExactly(
				CheckForFAPIInteractionIdInResourceResponse.class,
				EnsureMatchingFAPIInteractionId.class);
		assertThat(secondClientConditionClasses)
			.containsExactly(
				CheckForFAPIInteractionIdInResourceResponse.class,
				EnsureMatchingFAPIInteractionId.class);
	}

	private List<Class<? extends Condition>> getConditionClasses(ConditionSequence sequence) {
		sequence.evaluate();
		return sequence.getTestExecutionUnits().stream()
			.map(this::getConditionClass)
			.toList();
	}

	private List<Class<? extends Condition>> getConditionClasses(
		ConnectIdAuCibaServerProfileBehavior.CommonAuthorizationEndpointSetupSteps sequence) {
		sequence.evaluate();
		return sequence.getTestExecutionUnits().stream()
			.map(this::getConditionClass)
			.toList();
	}

	private List<Class<? extends Condition>> getConditionClasses(
		ConnectIdAuCibaServerProfileBehavior.AuthorizationEndpointSetupSteps sequence) {
		sequence.evaluate();
		return sequence.getTestExecutionUnits().stream()
			.map(this::getConditionClass)
			.toList();
	}

	private Class<? extends Condition> getConditionClass(TestExecutionUnit unit) {
		return ((ConditionCallBuilder) unit).getConditionClass();
	}
}
