package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-pixscheduling-happy-contenttype-jwt",
	displayName = "Scheduled Payment API basic test module",
	summary = "This test is a variation of the core happy path scheduled payments test where we check that the server is correctly dealing with the accept header on the API Calls.\n\n" +
		" \u2022 Creates a payment consent scheduled for today + 350 days \n" +
		" \u2022 POSTs to the payment endpoint - Both POST Requests will have accept headers that are not application/jwt \n" +
		" \u2022 Validates the payment returned in the self-object can be reached  \n" +
		" \u2022 Validates that the Payment will arrive at a scheduled payment confirmed status \n" +
		" \u2022 Perform 2 Requests to the GET Consents API with different accept header and check that it return a response of type application/jwt  \n" +
		" \u2022 Perform 2 Requests to the GET Payments API with different accept header and check that it return a response of type application/jwt  \n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"client.org_jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"resource.brazilPaymentConsent",
		"resource.brazilPixPayment",
		"resource.brazilOrganizationId"
	}
)
public class PixScheduledPaymentContentTypeJwtTestModule extends AbstractFunctionalTestModule {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(EnforcePresenceOfDebtorAccount.class);

		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void requestProtectedResource() {
		if(!validationStarted) {
			validationStarted = true;
			ConditionSequence pixSequence = new CallPixPaymentsEndpointSequence()
				.replace(CallProtectedResource.class, condition(CallProtectedResourceNoAcceptField.class))
				.replace(CreatePaymentRequestEntityClaims.class, condition(GeneratePaymentRequestEntityClaims.class))
				.skip(EnsureResponseCodeWas201.class, "Skipping 201 check");
			call(pixSequence);
			eventLog.startBlock(currentClientString() + "Validate response");
			validateResponse();

			eventLog.startBlock("Get Payments API with no accept header");
			ConditionSequence GetPaymentNoAcceptSequence = new CallGetPaymentEndpointSequence()
				.replace(CallProtectedResource.class, condition(CallProtectedResourceNoAcceptField.class))
				.replace(EnsureResponseCodeWas200.class, condition(OptionallyAllow200or406.class));

			call(GetPaymentNoAcceptSequence);
			callAndContinueOnFailure(EnsureStatusSASC.class);

			eventLog.startBlock("Get Payments API with application/json accept header");
			ConditionSequence GetPaymentJsonAcceptSequence = new CallGetPaymentEndpointSequence()
				.replace(CallProtectedResource.class, condition(CallProtectedResourceJsonAcceptField.class))
				.replace(EnsureResponseCodeWas200.class, condition(OptionallyAllow200or406.class));
			call(GetPaymentJsonAcceptSequence);
			callAndContinueOnFailure(EnsureStatusSASC.class);

			eventLog.startBlock("Get Consents API with no accept header");
			callAndStopOnFailure(LoadOldValues.class);
			ConditionSequence GetConsentNoAcceptSequence = new CallGetPaymentEndpointSequence()
				.replace(CallProtectedResource.class, condition(CallProtectedResourceNoAcceptField.class))
				.replace(EnsureResponseCodeWas200.class, condition(OptionallyAllow200or406.class));
			call(GetConsentNoAcceptSequence);
			callAndContinueOnFailure(EnsureStatusCONSUMED.class);

			eventLog.startBlock("Get Consents API with application/json accept header");
			callAndStopOnFailure(LoadOldValues.class);
			ConditionSequence GetConsentJsonAcceptSequence = new CallGetPaymentEndpointSequence()
				.replace(CallProtectedResource.class, condition(CallProtectedResourceNoAcceptField.class))
				.replace(EnsureResponseCodeWas200.class, condition(OptionallyAllow200or406.class));
			call(GetConsentJsonAcceptSequence);
			callAndContinueOnFailure(EnsureStatusCONSUMED.class);

			eventLog.endBlock();
		}
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps  = new OpenBankingBrazilPreAuthorizationErrorAgnosticSteps(addTokenEndpointClientAuthentication)
			.replace(FAPIBrazilCallPaymentConsentEndpointWithBearerToken.class, condition(FAPIBrazilCallPaymentConsentEndpointWithBearerTokenNoAcceptField.class))
			.replace(OptionallyAllow201Or422.class, condition(EnsureConsentResponseCodeWas201.class))
			.replace(FAPIBrazilCreatePaymentConsentRequest.class, paymentConsentEditingSequence())
			.insertAfter(FAPIBrazilAddConsentIdToClientScope.class, condition(SaveOldValues.class));
		return preauthSteps;
	}

	private ConditionSequence paymentConsentEditingSequence() {
		return sequenceOf(
			condition(FAPIBrazilGeneratePaymentConsentRequest.class),
			condition(EnsureScheduledPaymentDateIsTodayPlus350.class)
		);
	}

//	@Override
//	protected void createPlaceholder() {
//		callAndStopOnFailure(AskForScreenshotWithNoAccountSelection.class);
//
//		env.putString("error_callback_placeholder", env.getString("payments_placeholder"));
//	}


//	@Override
//	protected void performRedirect() {
//		performRedirectWithPlaceholder();
//	}
//
//	@Override
//	protected void onPostAuthorizationFlowComplete() {
//		waitForPlaceholders();
//
//		eventLog.log(getName(), "All test steps have run. The test will remaining in 'WAITING' state until the required screenshot is uploaded using the 'Upload Images' button at the top of the page. It may take upto 30 seconds for the test to move to 'FINISHED' after the upload.");
//
//	}

	@Override
	protected void validateResponse() {
		repeatSequence(() -> new PollForScheduledPaymentChangeSequence())
			.untilTrue("payment_accepted")
			.times(10)
			.onTimeout(sequenceOf(
					condition(TestTimedOut.class),
					condition(ChuckWarning.class)))
			.run();

//		fireTestFinished();
	}

}
