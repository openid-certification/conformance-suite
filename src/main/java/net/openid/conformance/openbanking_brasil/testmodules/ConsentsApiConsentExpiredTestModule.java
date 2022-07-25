package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddExpirationInOneMinute;
import net.openid.conformance.openbanking_brasil.testmodules.account.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.CheckAuthorizationEndpointHasError;
import net.openid.conformance.openbanking_brasil.testmodules.support.ChuckWarning;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.ConsentHasExpiredInsteadOfBeenRejected;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "consent-api-expired-consent-test",
	displayName = "Validate that consents can expire",
	summary = "Consent will be created with a 1-minute expiry, and the user will be sent to the authorization endpoint after the consent has expired. The authorization server must return the browser to the redirect URL with a valid OAuth2 error response.\n" +
		"\u2022 Creates a Consent with all of the existing permissions\n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Expects a valid consent creation 201\n" +
		"\u2022 Redirects the User - He should not accept the consent, waiting for the consent to reach an expired state\n" +
		"\u2022 Verifies on the authorization endpoint response if the expiration of the consent resulted in an error message",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.client_id",
	"client.org_jwks"
})
public class ConsentsApiConsentExpiredTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		//Arbitrary resource
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		return super.createOBBPreauthSteps().
			replace(FAPIBrazilAddExpirationToConsentRequest.class, condition(AddExpirationInOneMinute.class));
	}

	@Override
	protected void performPreAuthorizationSteps() {
		super.performPreAuthorizationSteps();
		callAndContinueOnFailure(WaitFor2Seconds.class);
		callAndContinueOnFailure(WaitFor60Seconds.class);
	}



	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckMatchingCallbackParameters.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(RejectStateInUrlQueryForHybridFlow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CheckAuthorizationEndpointHasError.class);

		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.2.2.5", "JARM-4.4-2");

		callAndContinueOnFailure(ValidateIssInAuthorizationResponse.class, Condition.ConditionResult.WARNING, "OAuth2-iss-2");

		callAndStopOnFailure(ConsentHasExpiredInsteadOfBeenRejected.class);
		callAndContinueOnFailure(ChuckWarning.class, Condition.ConditionResult.WARNING);

		fireTestFinished();
	}


	@Override
	protected void validateResponse() {

	}

}
