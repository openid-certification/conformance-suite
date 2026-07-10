package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckCIBAModeIsPing;
import net.openid.conformance.condition.as.EnsureScopeContainsConsents;
import net.openid.conformance.condition.as.EnsureScopeContainsResources;
import net.openid.conformance.condition.as.EncryptIdToken;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilChangeConsentStatusToAuthorized;
import net.openid.conformance.condition.as.FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant;
import net.openid.conformance.condition.as.FAPIBrazilSetRequiredIdTokenEncryptionConfig;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.FAPIEnsureClientJwksContainsAnEncryptionKey;
import net.openid.conformance.condition.as.GenerateIdTokenClaims;
import net.openid.conformance.condition.as.GenerateIdTokenClaimsWith181DayExp;
import net.openid.conformance.condition.as.SignIdToken;
import net.openid.conformance.condition.as.SignIdTokenWithX5tS256;
import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.TestFailureException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OpenBankingBrazilCibaRPProfileBehavior_UnitTest {

	private final OpenBankingBrazilCibaRPProfileBehavior behavior = new OpenBankingBrazilCibaRPProfileBehavior();

	@Test
	public void claimsCustomerDataMtlsPaths() {
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH)).isTrue();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/consent-id")).isTrue();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_RESOURCE_PATH)).isTrue();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH)).isFalse();
		assertThat(behavior.acceptsGenericAccountsEndpoint()).isFalse();
	}

	@Test
	public void doesNotClaimPaymentMtlsPaths() {
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH)).isFalse();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/consent-id")).isFalse();
		assertThat(behavior.claimsProfileSpecificMtlsPath(FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH)).isFalse();
	}

	@Test
	public void dispatchesOnlyCustomerDataMtlsPaths() {
		TestableFAPICIBAClientTest module = new TestableFAPICIBAClientTest();
		behavior.setModule(module);

		assertThat(behavior.handleProfileSpecificMtlsPath("request-id", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH))
			.isEqualTo("new-consent");
		assertThat(module.newConsentWasPayments).isFalse();
		assertThat(behavior.handleProfileSpecificMtlsPath("request-id", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/consent-id"))
			.isEqualTo("get-consent");
		assertThat(module.getConsentWasPayments).isFalse();
		assertThat(behavior.handleProfileSpecificMtlsPath("request-id", FAPIBrazilRsPathConstants.BRAZIL_RESOURCE_PATH))
			.isEqualTo("resources");

		assertThatThrownBy(() -> behavior.handleProfileSpecificMtlsPath("request-id", FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH))
			.isInstanceOf(TestFailureException.class);
		assertThatThrownBy(() -> behavior.handleProfileSpecificMtlsPath("request-id", FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH))
			.isInstanceOf(TestFailureException.class);
	}

	@Test
	public void exposesPingOnlyServerConfiguration() {
		List<ConditionCallBuilder> conditionCalls = getConditionCalls(behavior.applyProfileSpecificServerConfigurationSetup());

		ConditionCallBuilder pingModeCall = conditionCalls.stream()
			.filter(call -> call.getConditionClass().equals(CheckCIBAModeIsPing.class))
			.findFirst()
			.orElseThrow();
		assertThat(pingModeCall.getRequirements()).containsExactly("BrazilCIBA-6.3.4");
	}

	@Test
	public void retriesTransientPingDeliveryFailuresOnlyForBrazil() {
		ConditionCallBuilder brazilPingCall = getConditionCalls(behavior.getPingNotificationEndpointCallSteps()).getFirst();
		ConditionCallBuilder genericPingCall = getConditionCalls(
			new FAPICIBARPProfileBehavior().getPingNotificationEndpointCallSteps()).getFirst();

		assertThat(brazilPingCall.getConditionClass())
			.isEqualTo(PingClientNotificationEndpointWithRetriesForBrazil.class);
		assertThat(brazilPingCall.getRequirements()).containsExactly("CIBA", "BrazilCIBA-6.2.8");
		assertThat(genericPingCall.getConditionClass()).isEqualTo(PingClientNotificationEndpoint.class);
		assertThat(genericPingCall.getRequirements()).containsExactly("CIBA");
	}

	@Test
	public void validatesBrazilBackchannelRequestBoundariesAndAuthorizesConsent() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.applyProfileSpecificBackchannelRequestChecks());

		assertThat(conditionClasses).containsExactly(
			EnsureBackchannelRequestDoesNotContainRequestedExpiryForBrazil.class,
			EnsureBackchannelRequestObjectDoesNotContainUserCode.class,
			EnsureBackchannelRequestObjectBindingMessageDoesNotContainUrl.class,
			EnsureLoginHintEqualsConsentId.class,
			FAPIBrazilChangeConsentStatusToAuthorized.class);
	}

	@Test
	public void keepsGenericBackchannelRequestChecksUnchanged() {
		FAPICIBARPProfileBehavior genericBehavior = new FAPICIBARPProfileBehavior();
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(
			genericBehavior.applyProfileSpecificBackchannelRequestChecks());

		assertThat(conditionClasses)
			.containsExactly(BackchannelRequestRequestedExpiryIsAnInteger.class)
			.doesNotContain(
				EnsureBackchannelRequestDoesNotContainRequestedExpiryForBrazil.class,
				EnsureBackchannelRequestObjectDoesNotContainUserCode.class,
				EnsureLoginHintEqualsConsentId.class);
	}

	@Test
	public void requiresConsentAndResourcesScopesForBackchannelRequest() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.applyProfileSpecificBackchannelScopeChecks());

		assertThat(conditionClasses).containsExactly(
			FAPIBrazilValidateConsentScope.class,
			EnsureScopeContainsConsents.class,
			EnsureScopeContainsResources.class);
	}

	@Test
	public void extractsClientCredentialsScopeForConsentCreation() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.getClientCredentialsGrantTypeSteps());

		assertThat(conditionClasses).containsExactly(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
	}

	@Test
	public void usesNormalIdTokenExpiryWithBrazilClaims() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.applyProfileSpecificIdTokenClaims());

		assertThat(conditionClasses)
			.containsExactly(GenerateIdTokenClaims.class, FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class)
			.doesNotContain(GenerateIdTokenClaimsWith181DayExp.class);
	}

	@Test
	public void usesStandardIdTokenSigningCondition() {
		assertThat(behavior.getSignIdTokenCondition())
			.isEqualTo(SignIdToken.class)
			.isNotEqualTo(SignIdTokenWithX5tS256.class);
	}

	@Test
	public void validatesIdTokenEncryptionConfiguration() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.applyProfileSpecificClientConfigurationValidation());

		assertThat(conditionClasses).containsExactly(
			FAPIBrazilSetRequiredIdTokenEncryptionConfig.class,
			FAPIEnsureClientJwksContainsAnEncryptionKey.class);
	}

	@Test
	public void encryptsIdToken() {
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(behavior.applyProfileSpecificIdTokenEncryption());

		assertThat(conditionClasses).containsExactly(EncryptIdToken.class);
	}

	private List<Class<? extends Condition>> getConditionClasses(ConditionSequence sequence) {
		return getConditionCalls(sequence).stream()
			.map(ConditionCallBuilder::getConditionClass)
			.toList();
	}

	private List<ConditionCallBuilder> getConditionCalls(ConditionSequence sequence) {
		sequence.evaluate();
		return sequence.getTestExecutionUnits().stream()
			.map(ConditionCallBuilder.class::cast)
			.toList();
	}

	private static class TestableFAPICIBAClientTest extends AbstractFAPICIBAClientTest {

		private boolean newConsentWasPayments;
		private boolean getConsentWasPayments;

		@Override
		protected Object brazilHandleNewConsentRequest(String requestId, boolean isPayments) {
			newConsentWasPayments = isPayments;
			return "new-consent";
		}

		@Override
		protected Object brazilHandleGetConsentRequest(String requestId, String path, boolean isPayments) {
			getConsentWasPayments = isPayments;
			return "get-consent";
		}

		@Override
		protected Object resourcesEndpoint(String requestId) {
			return "resources";
		}
	}
}
