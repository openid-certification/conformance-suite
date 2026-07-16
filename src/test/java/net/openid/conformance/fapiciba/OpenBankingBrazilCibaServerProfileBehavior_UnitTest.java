package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.CheckCIBAModeIsPing;
import net.openid.conformance.condition.as.FAPIEnsureClientJwksContainsAnEncryptionKey;
import net.openid.conformance.condition.as.FAPIBrazilSetRequiredIdTokenEncryptionConfig;
import net.openid.conformance.condition.client.AddClientX509CertificateClaimToPublicJWKs;
import net.openid.conformance.condition.client.AddJwksUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddPublicJwksToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSoftwareStatementToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CallConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.ClientManagementEndpointAndAccessTokenRequired;
import net.openid.conformance.condition.client.CopyOrgJwksFromDynamicRegistrationTemplateToClientConfiguration;
import net.openid.conformance.condition.client.CreateRefreshTokenRequest;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.ExtractDirectoryConfiguration;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClient2Configuration;
import net.openid.conformance.condition.client.ExtractJWKSDirectFromClientConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.FAPIBrazilAddConsentIdToClientScope;
import net.openid.conformance.condition.client.FAPIBrazilAddSoftwareStatementRedirectUrisToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilCallPaymentConsentEndpointWithBearerToken;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilExtractJwksUriFromSoftwareStatement;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCreateConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentConsentRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenEncryptedUsingRSAOAEPA256GCM;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.GenerateMTLSCertificateFromJWKs;
import net.openid.conformance.condition.client.GeneratePS256ClientJWKsWithKeyID;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractConsentIdFromConsentEndpointResponse;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetHintTypeToLoginHint;
import net.openid.conformance.condition.client.SetLoginHintToConsentId;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetProtectedResourceUrlToSingleResourceEndpoint;
import net.openid.conformance.condition.client.ValidateIdTokenEncrypted;
import net.openid.conformance.condition.client.ValidateOpenBankingBrazilCibaDynamicRegistrationResponse;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.RefreshTokenRequestSteps;
import net.openid.conformance.testmodule.ConditionCallBuilder;
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
	public void defaultDynamicRegistrationGeneratesCredentialsAndPublishesInlineJwks() {
		FAPICIBAServerProfileBehavior defaultBehavior = new FAPICIBAServerProfileBehavior();

		assertThat(getConditionClasses(defaultBehavior.getClientRegistrationCredentialSetupSteps(false)))
			.containsExactly(
				GeneratePS256ClientJWKsWithKeyID.class,
				GenerateMTLSCertificateFromJWKs.class,
				AddClientX509CertificateClaimToPublicJWKs.class);
		assertThat(getConditionClasses(defaultBehavior.getClientRegistrationKeyPublicationSteps()))
			.containsExactly(AddPublicJwksToDynamicRegistrationRequest.class);
		assertThat(defaultBehavior.shouldUseInitialAccessTokenForRegistration()).isTrue();
	}

	@Test
	public void brazilDynamicRegistrationUsesConfiguredCredentialsAndDirectoryMetadata() {
		List<Class<? extends Condition>> credentialSetup = getConditionClasses(
			behavior.getClientRegistrationCredentialSetupSteps(false));
		List<Class<? extends Condition>> keyPublication = getConditionClasses(
			behavior.getClientRegistrationKeyPublicationSteps());

		assertThat(credentialSetup).containsSubsequence(
			ExtractMTLSCertificatesFromConfiguration.class,
			ExtractJWKSDirectFromClientConfiguration.class,
			ExtractDirectoryConfiguration.class,
			CreateTokenEndpointRequestForClientCredentialsGrant.class,
			FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken.class);
		assertThat(credentialSetup).doesNotContain(
			GeneratePS256ClientJWKsWithKeyID.class,
			GenerateMTLSCertificateFromJWKs.class);
		assertThat(keyPublication).containsExactly(
			FAPIBrazilExtractJwksUriFromSoftwareStatement.class,
			AddJwksUriToDynamicRegistrationRequest.class,
			FAPIBrazilAddSoftwareStatementRedirectUrisToDynamicRegistrationRequest.class,
			AddSoftwareStatementToDynamicRegistrationRequest.class);
		assertThat(keyPublication).doesNotContain(AddPublicJwksToDynamicRegistrationRequest.class);
		assertThat(behavior.shouldUseInitialAccessTokenForRegistration()).isFalse();
	}

	@Test
	public void brazilSecondClientDynamicRegistrationUsesSecondClientCredentials() {
		List<Class<? extends Condition>> credentialSetup = getConditionClasses(
			behavior.getClientRegistrationCredentialSetupSteps(true));

		assertThat(credentialSetup).containsSubsequence(
			ExtractMTLSCertificates2FromConfiguration.class,
			ExtractJWKSDirectFromClient2Configuration.class,
			ExtractDirectoryConfiguration.class);
		assertThat(credentialSetup).doesNotContain(
			ExtractMTLSCertificatesFromConfiguration.class,
			ExtractJWKSDirectFromClientConfiguration.class);
	}

	@Test
	public void brazilValidatesRegistrationResponseAndEffectiveEncryptionConfiguration() {
		List<Class<? extends Condition>> responseValidation = getConditionClasses(
			behavior.getClientRegistrationResponseValidationSteps());

		assertThat(responseValidation).containsExactly(
			ClientManagementEndpointAndAccessTokenRequired.class,
			ValidateOpenBankingBrazilCibaDynamicRegistrationResponse.class,
			CopyOrgJwksFromDynamicRegistrationTemplateToClientConfiguration.class,
			FAPIBrazilSetRequiredIdTokenEncryptionConfig.class,
			FAPIEnsureClientJwksContainsAnEncryptionKey.class);
	}

	@Test
	public void doesNotAddDefaultBindingMessageToBrazilHappyPath() {
		assertThat(behavior.shouldAddBindingMessageToAuthorizationEndpointRequest()).isFalse();
	}

	@Test
	public void citesCurrentBeta1SectionsForPingModeAndLoginHint() {
		List<ConditionCallBuilder> conditionCalls = getConditionCalls(behavior.onConfigure());

		assertThat(conditionCalls).hasSize(2);
		assertThat(conditionCalls.get(0).getConditionClass()).isEqualTo(CheckCIBAModeIsPing.class);
		assertThat(conditionCalls.get(0).getRequirements()).containsExactly("BrazilCIBA-6.2.2");
		assertThat(conditionCalls.get(1).getConditionClass()).isEqualTo(SetHintTypeToLoginHint.class);
		assertThat(conditionCalls.get(1).getRequirements()).containsExactly("BrazilCIBA-6.2.3");
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
		return getConditionCalls(sequence).stream()
			.map(ConditionCallBuilder::getConditionClass)
			.toList();
	}

	private List<ConditionCallBuilder> getConditionCalls(ConditionSequence sequence) {
		sequence.evaluate();
		return sequence.getTestExecutionUnits().stream()
			.filter(ConditionCallBuilder.class::isInstance)
			.map(ConditionCallBuilder.class::cast)
			.toList();
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
