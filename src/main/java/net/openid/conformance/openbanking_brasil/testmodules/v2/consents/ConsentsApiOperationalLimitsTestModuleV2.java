package net.openid.conformance.openbanking_brasil.testmodules.v2.consents;

import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.consent.v2.ConsentDetailsIdentifiedByConsentIdValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2.OpenBankingBrazilPreAuthorizationConsentApiV2;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consents-api-operational-limits-v2",
	displayName = "Consents Api operational limits test module V2",
	summary = "Make sure that access is not blocked on the Consents API V2 regardless of the number of calls done against it.\n\n"+
		"\u2022 Create a consent using the CPF and CNPJ provided for the Operational Limits tests. Send the permissions for either customer business or customer personal data, based on what has been provided on the test configuration\n" +
		"\u2022 Redirect the user to authorise the Consent with the customer and the created consent scopes- Expect a success on the redirect\n" +
		"\u2022 Call the GET Consents API 20 Times using the Authorized ConsentID\n" +
		"\u2022 Expect every single call to return a 200 - OK Code\n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.brazilCpfOperational",
		"resource.brazilCnpjOperational"
	}
)
public class ConsentsApiOperationalLimitsTestModuleV2 extends AbstractFunctionalTestModule {

	@Override
	protected void configureClient() {
		//Arbitrary resource
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}


	@Override
	protected void validateClientConfiguration() {
		super.validateClientConfiguration();
		callAndStopOnFailure(AddConsentScope.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationConsentApiV2(addTokenEndpointClientAuthentication);
		return preauthSteps;
	}

	@Override
	protected void requestProtectedResource() {
		call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));
		repeatSequence(() -> getPreConsentWithBearerTokenSequence()
			.then(getValidateConsentResponsePollingSequence()))
			.times(20)
			.trailingPause(1)
			.run();
	}

	@Override
	protected void validateResponse() {}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

	protected ConditionSequence getValidateConsentResponsePollingSequence() {
		return sequenceOf(
			condition(ConsentDetailsIdentifiedByConsentIdValidatorV2.class),
			condition(EnsureConsentResponseWas200.class)
		);
	}
	protected ConditionSequence getPreConsentWithBearerTokenSequence(){
		return sequenceOf(
			condition(ConsentIdExtractor.class),
			condition(PrepareToFetchConsentRequest.class),
			condition(CallConsentApiWithBearerToken.class)
		);
	}
}
