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
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
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

		assertThat(conditionCalls).extracting(ConditionCallBuilder::getConditionClass)
			.startsWith(ExtractMTLSCertificatesFromConfiguration.class);

		ConditionCallBuilder pingModeCall = conditionCalls.stream()
			.filter(call -> call.getConditionClass().equals(CheckCIBAModeIsPing.class))
			.findFirst()
			.orElseThrow();
		assertThat(pingModeCall.getRequirements()).containsExactly("BrazilCIBA-6.3.4");

		ConditionCallBuilder signingAlgorithmCall = conditionCalls.stream()
			.filter(call -> call.getConditionClass().equals(SetServerSigningAlgToPS256.class))
			.findFirst()
			.orElseThrow();
		assertThat(signingAlgorithmCall.getRequirements()).containsExactly("BrazilOB22-6.2");
	}

	@Test
	public void retriesTransientPingDeliveryFailuresOnlyForBrazil() {
		ConditionCallBuilder brazilPingCall = getConditionCalls(behavior.getPingNotificationEndpointCallSteps()).getFirst();
		ConditionCallBuilder genericPingCall = getConditionCalls(
			new FAPICIBARPProfileBehavior().getPingNotificationEndpointCallSteps()).getFirst();

		assertThat(brazilPingCall.getConditionClass())
			.isEqualTo(PingClientNotificationEndpointWithRetriesForBrazil.class);
		assertThat(brazilPingCall.getRequirements()).containsExactly("CIBA-10.2", "BrazilCIBA-6.2.8");
		assertThat(genericPingCall.getConditionClass()).isEqualTo(PingClientNotificationEndpoint.class);
		assertThat(genericPingCall.getRequirements()).containsExactly("CIBA-10.2");
	}

	@Test
	public void acceptsOptionalPositiveRequestedExpiryForBrazil() {
		List<ConditionCallBuilder> conditionCalls = getConditionCalls(
			behavior.applyProfileSpecificBackchannelRequestChecks());

		assertThat(conditionCalls).extracting(ConditionCallBuilder::getConditionClass).containsExactly(
			BackchannelRequestRequestedExpiryIsAnInteger.class,
			EnsureBackchannelRequestObjectDoesNotContainUserCode.class,
			EnsureBackchannelRequestObjectBindingMessageDoesNotContainUrl.class,
			EnsureLoginHintEqualsConsentId.class,
			FAPIBrazilChangeConsentStatusToAuthorized.class);
		assertThat(conditionCalls.getFirst().getRequirements())
			.containsExactly("CIBA-7.1", "CIBA-7.1.1", "BrazilCIBA-6.3.7");
	}

	@Test
	public void configuresDataConsentAuthenticationRequestMaximum() {
		List<ConditionCallBuilder> conditionCalls = getConditionCalls(
			behavior.applyProfileSpecificBackchannelEndpointResponse());

		assertThat(conditionCalls).extracting(ConditionCallBuilder::getConditionClass)
			.containsExactly(SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry.class);
		assertThat(conditionCalls.getFirst().getRequirements()).containsExactly("BrazilCIBA-6.2.6");
	}

	@Test
	public void keepsGenericBackchannelRequestChecksUnchanged() {
		FAPICIBARPProfileBehavior genericBehavior = new FAPICIBARPProfileBehavior();
		List<Class<? extends Condition>> conditionClasses = getConditionClasses(
			genericBehavior.applyProfileSpecificBackchannelRequestChecks());

		assertThat(conditionClasses)
			.containsExactly(BackchannelRequestRequestedExpiryIsAnInteger.class)
			.doesNotContain(
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
			.isEqualTo(SignIdToken.class);
	}

	@Test
	public void validatesIdTokenEncryptionConfiguration() {
		List<ConditionCallBuilder> conditionCalls = getConditionCalls(
			behavior.applyProfileSpecificClientConfigurationValidation());

		assertThat(conditionCalls).extracting(ConditionCallBuilder::getConditionClass).containsExactly(
			FAPIBrazilSetRequiredIdTokenEncryptionConfig.class,
			FAPIEnsureClientJwksContainsAnEncryptionKey.class);
		assertThat(conditionCalls.get(0).getRequirements()).containsExactly(
			"BrazilOB22-5.1.1-1", "BrazilOB22-6.3");
		assertThat(conditionCalls.get(1).getRequirements()).contains(
			"BrazilOB22-5.1.1-2");
	}

	@Test
	public void encryptsIdToken() {
		List<ConditionCallBuilder> conditionCalls = getConditionCalls(behavior.applyProfileSpecificIdTokenEncryption());

		assertThat(conditionCalls).extracting(ConditionCallBuilder::getConditionClass)
			.containsExactly(EncryptIdToken.class);
		assertThat(conditionCalls.getFirst().getRequirements()).contains(
			"BrazilOB22-5.1.1-1", "BrazilOB22-6.3");
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
