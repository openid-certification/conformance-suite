package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CreateRefreshTokenRequest;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.FAPIBrazilAddConsentIdToClientScope;
import net.openid.conformance.condition.client.FAPIBrazilCallPaymentConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCreateConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractConsentIdFromConsentEndpointResponse;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetLoginHintToConsentId;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateIdTokenEncrypted;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.RefreshTokenRequestSteps;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.TestExecutionUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenBankingBrazilCibaServerProfileBehavior_UnitTest {

	private final OpenBankingBrazilCibaServerProfileBehavior behavior = new OpenBankingBrazilCibaServerProfileBehavior();

	@Test
	public void validatesTokenEndpointIdTokenIsEncrypted() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(
			behavior.validateTokenEndpointIdToken());

		assertThat(conditionClasses).containsExactly(
			ValidateIdTokenEncrypted.class,
			FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM.class);
	}

	@Test
	public void usesStandardBrazilIdTokenValidationWithoutSpecialExpiry() throws ReflectiveOperationException {
		ConditionSequence sequence = behavior.getProfileIdTokenValidationSteps()
			.getDeclaredConstructor()
			.newInstance();

		List<Class<? extends Condition>> conditionClasses = getConditionClasses(sequence);

		assertThat(conditionClasses).containsExactly(FAPIBrazilValidateIdTokenSigningAlg.class);
	}

	@Test
	public void usesPingModeAsPrimaryMode() {
		assertThat(behavior.shouldCallTokenEndpointBeforePingNotification()).isFalse();
	}

	@Test
	public void doesNotAddDefaultBindingMessageToBrazilHappyPath() {
		assertThat(behavior.shouldAddBindingMessageToAuthorizationEndpointRequest()).isFalse();
	}

	@Test
	public void createsDataConsentBeforeBrazilCibaRequest() {
		TestableFAPICIBAID1 module = new TestableFAPICIBAID1();
		module.addTokenEndpointClientAuthentication = NoOpClientAuthentication.class;
		behavior.setModule(module);

		List<Class<? extends Condition>> conditionClasses = getConditionClasses(
			behavior.getPreAuthorizationSteps().get());

		assertThat(conditionClasses).containsSubsequence(
			CreateTokenEndpointRequestForClientCredentialsGrant.class,
			SetConsentsScopeOnTokenEndpointRequest.class,
			CallTokenEndpointAndReturnFullResponse.class,
			FAPIBrazilOpenBankingCreateConsentRequest.class,
			CallConsentEndpointWithBearerToken.class,
			ExtractConsentIdFromConsentEndpointResponse.class,
			FAPIBrazilAddConsentIdToClientScope.class);
		assertThat(conditionClasses).doesNotContain(
			SetPaymentsScopeOnTokenEndpointRequest.class,
			FAPIBrazilCreatePaymentConsentRequest.class,
			FAPIBrazilSignPaymentConsentRequest.class,
			FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class);
	}

	@Test
	public void setsLoginHintFromConsentId() throws ReflectiveOperationException {
		ConditionSequence sequence = behavior.getProfileAuthorizationEndpointSetupSteps()
			.getDeclaredConstructor()
			.newInstance();

		List<Class<? extends Condition>> conditionClasses = getConditionClasses(sequence);

		assertThat(conditionClasses).containsExactly(SetLoginHintToConsentId.class);
	}

	@Test
	public void usesResourcesAsProtectedResourceEndpoint() throws ReflectiveOperationException {
		ConditionSequence sequence = behavior.getResourceConfiguration()
			.getDeclaredConstructor()
			.newInstance();

		List<Class<? extends Condition>> conditionClasses = getConditionClasses(sequence);

		assertThat(conditionClasses).containsExactly(SetProtectedResourceUrlToSingleResourceEndpoint.class);
	}

	@Test
	public void usesRefreshTokenFlowForResourceRequestUpdates() {
		ConditionSequence sequence = behavior.createUpdateResourceRequestSteps(false, NoOpClientAuthentication.class);
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(sequence);

		assertThat(sequence).isInstanceOf(RefreshTokenRequestSteps.class);
		assertThat(conditionClasses).containsSubsequence(
			CreateRefreshTokenRequest.class,
			CallTokenEndpointAndReturnFullResponse.class,
			ExtractAccessTokenFromTokenResponse.class);
	}

	@Test
	public void plainFapiCanCallTokenEndpointBeforePingNotification() {
		assertThat(new FAPICIBAServerProfileBehavior().shouldCallTokenEndpointBeforePingNotification()).isTrue();
	}

	private List<Class<? extends Condition>> getConditionClasses(ConditionSequence sequence) {
		sequence.evaluate();
		return sequence.getTestExecutionUnits().stream()
			.filter(ConditionCallBuilder.class::isInstance)
			.map(this::getConditionClass)
			.toList();
	}

	private Class<? extends Condition> getConditionClass(TestExecutionUnit unit) {
		return ((ConditionCallBuilder) unit).getConditionClass();
	}

	public static class NoOpClientAuthentication extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			// No client authentication conditions needed for sequence-shape tests.
		}
	}

	private static class TestableFAPICIBAID1 extends AbstractFAPICIBAID1 {
	}

}
