package net.openid.conformance.openbanking_brasil.testmodules.v2.consents;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "consents-api-revoked-aspsp",
	displayName = "Validate that when a consent is manually revoked the rejected by is 'CUSTOMER_MANUALLY_REVOKED'",
	summary = "Makes sure that the user can revoke its created consent inside the ASPSP." +
		"\u2022 For this test, the Tester will have to manually revoke the created consent on the ASPSP platform while the server is set to poll the consent API. \n" +
		"\u2022 Creates a Consent with all of the existing permissions\n" +
		"\u2022 Expects a success - 201\n" +
		"\u2022 Redirect the user to authorize the consent\n" +
		"\u2022 Call the GET Consents API\n" +
		"\u2022 POLL the GET Consents API for 10 minutes, one call every 60 seconds.\n" +
		"\u2022 Continue Polling until the Consent Status reaches status REJECTED. Make Sure RejectedBy is set to USER. Make sure Reason is set to \"CUSTOMER_MANUALLY_REVOKED”\n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class ConsentsApiRevokedAspspTestModule extends AbstractFunctionalTestModule {

	@Override
	protected void configureClient(){
		//Arbitrary resource
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}


	@Override
	protected void validateClientConfiguration() {
		super.validateClientConfiguration();
		callAndStopOnFailure(AddConsentScope.class);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationConsentApi(addTokenEndpointClientAuthentication);

		return preauthSteps;
	}

	@Override
	protected void requestProtectedResource(){
		call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));

		repeatSequence(() -> getPreConsentWithBearerTokenSequence()
			.then(getValidateConsentResponsePollingSequence()))
			.untilTrue("code_returned")
			.times(10)
			.trailingPause(60)
			.onTimeout(sequenceOf(
				condition(TestTimedOut.class),
				condition(ChuckWarning.class)))
			.run();
	}

	@Override
	protected void validateResponse() {}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

	protected ConditionSequence getValidateConsentResponsePollingSequence(){
		return sequenceOf(
			condition(ConsentDetailsIdentifiedByConsentIdValidator.class),
			condition(EnsureConsentAspspRevoked.class)
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
