package net.openid.conformance.openbanking_brasil.testmodules.v2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "consent-api-expired-consent-test-v2",
	displayName = "Validate that consents can expire",
	summary = "Consent will be created with a 3-minute expiry. The user will be sent to the authorization endpoint before the consent has expired. Conformance Suite will call the Consents API after the consent has expired to make sure the consent is marked as CONSENT_MAX_DATE_REACHED . \n" +
		"\u2022 Creates a Consent with all of the existing permissions and set expiration to be of 3 minutes\n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Expects a valid consent creation 201\n" +
		"\u2022 Redirects the User - He should accept the Consent \n" +
		"\u2022 Conformance Suite Will be set to sleep for 3 minutes \n" +
		"\u2022 Call the Consents API for the authorized ConsentID \n" +
		"\u2022 Expect a success 200 - Make sure Status is set to REJECTED. Make Sure RejectedBy is set to ASPSP. Make sure Reason is set to \"CONSENT_MAX_DATE_REACHED\"",

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
public class ConsentsApiConsentExpiredTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		//Arbitrary resource
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		return super.createOBBPreauthSteps().
			replace(FAPIBrazilAddExpirationToConsentRequest.class, condition(AddExpirationInThreeMinute.class));
	}

	@Override
	protected void requestProtectedResource(){

	}

	@Override
	protected void performPreAuthorizationSteps() {
		super.performPreAuthorizationSteps();
		callAndContinueOnFailure(WaitFor2Seconds.class);
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		runInBlock("Validating get consent response", () -> {
			callAndContinueOnFailure(WaitFor180Seconds.class);

			callAndStopOnFailure(PrepareToFetchConsentRequest.class);
			callAndStopOnFailure(TransformConsentRequestForProtectedResource.class);
			call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));
			preCallProtectedResource("Fetch consent");

			callAndContinueOnFailure(EnsureConsentWasRejected.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(ValidateConsentRejection.class);
			callAndStopOnFailure(EnsureConsentRejectedByASPSP.class);
			callAndStopOnFailure(EnsureConsentReasonConsentMaxDateReached.class);

		});
		fireTestFinished();
	}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void validateResponse() {
	}

}
