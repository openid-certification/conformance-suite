package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddEssentialTxnClaimRequestToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.SetConnectIdBindingMessageToPurpose;
import net.openid.conformance.condition.client.SetConnectIdCibaLoginHintFromConfiguration;
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
