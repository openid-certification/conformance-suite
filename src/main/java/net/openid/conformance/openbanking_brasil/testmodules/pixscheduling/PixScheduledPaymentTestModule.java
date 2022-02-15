package net.openid.conformance.openbanking_brasil.testmodules.pixscheduling;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-pixscheduling-happy-schedule-accepted",
	displayName = "Scheduled Payment API basic test module",
	summary = "This test is the core happy path scheduled payments test.\n\n" +
		"Flow:\n" +
		"Creates a payment consent scheduled for today + 350 days, POSTs to the the payment endpoint, validates the payment returned in the self object can be reached\n\n" +
		"Required:\n" +
		"Consent url pointing at the consent endpoint.\n" +
		"Resource url pointing at the base url. The test appends on the required payment endpoints\n\n" +
		"A screenshot should be uploaded showing the user is not presented with an option to select an account at the bank.",
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
public class PixScheduledPaymentTestModule extends AbstractDictVerifiedPaymentTestModule {

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
				.replace(CreatePaymentRequestEntityClaims.class, condition(GeneratePaymentRequestEntityClaims.class))
				.replace(CallProtectedResourceWithBearerTokenAndCustomHeaders.class,
					condition(CallProtectedResourceWithBearerTokenAndCustomHeadersOptionalError.class))
				.skip(EnsureHttpStatusCodeIs201.class, "Skipping 201 check");
			resourceCreationErrorMessageCondition().ifPresent(c -> {
				pixSequence.insertAfter(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, condition(c));
			});
			call(pixSequence);
			eventLog.startBlock(currentClientString() + "Validate response");
			validateResponse();
			eventLog.endBlock();
		}
	}

	@Override
	protected void configureDictInfo() {

	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		ConditionSequence preauthSteps = super.createOBBPreauthSteps()
			.replace(FAPIBrazilCreatePaymentConsentRequest.class, paymentConsentEditingSequence());
		return preauthSteps;
	}

	private ConditionSequence paymentConsentEditingSequence() {
		return sequenceOf(
			condition(FAPIBrazilGeneratePaymentConsentRequest.class),
			condition(EnsureScheduledPaymentDateIsTodayPlus350.class)
		);
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(AskForScreenshotWithNoAccountSelection.class);

		env.putString("error_callback_placeholder", env.getString("payments_placeholder"));
	}


	@Override
	protected void performRedirect() {
		performRedirectWithPlaceholder();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		waitForPlaceholders();

		eventLog.log(getName(), "All test steps have run. The test will remaining in 'WAITING' state until the required screenshot is uploaded using the 'Upload Images' button at the top of the page. It may take upto 30 seconds for the test to move to 'FINISHED' after the upload.");

	}

	@Override
	protected ConditionSequence statusValidationSequence() {
		return sequenceOf(
			condition(PaymentsProxyCheckForScheduledAcceptedStatus.class),
			condition(PaymentsProxyCheckForInvalidStatus.class));
	}

}
