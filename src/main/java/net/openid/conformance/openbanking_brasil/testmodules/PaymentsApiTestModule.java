package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentFetchPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.EnsureNoRejectionReasonIFStatusIsNotRJCT;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-test",
	displayName = "Payments API basic test module",
	summary = "This test is the core happy path payments test.\n\n" +
		"Flow:\n" +
		"Creates a payment consent, POSTs to the the payment endpoint, validates the payment returned in the self object can be reached\n\n" +
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
public class PaymentsApiTestModule extends AbstractOBBrasilFunctionalTestModule {

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
	protected void createPlaceholder() {
		callAndStopOnFailure(AskForScreenshotWithNoAccountSelection.class);

		env.putString("error_callback_placeholder", env.getString("payments_placeholder"));
	}

	@Override
	@CallProtectedResource.FixMe
	protected void validateResponse() {
		callAndStopOnFailure(PaymentInitiationPixPaymentsValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureNoRejectionReasonIFStatusIsNotRJCT.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureSelfLinkEndsInPaymentId.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		eventLog.startBlock("Checking the self endpoint - Expecting 200, validating response");
		call(new ValidateSelfEndpoint()
			.insertAfter(
				EnsureResponseCodeWas200.class, sequenceOf(
					condition(EnsureResponseWasJwt.class),
					condition(PaymentFetchPixPaymentsValidator.class)
			))
			// TODO use CalLProtectedResource
//			.replace(CallProtectedResourceWithBearerToken.class, sequenceOf(
			.replace(CallProtectedResource.class, sequenceOf(
				condition(AddJWTAcceptHeader.class),
//				condition(CallProtectedResourceWithBearerTokenAndCustomHeaders.class)
				condition(CallProtectedResource.class)
			)));
	}

	@Override
	protected void performRedirect() {
		performRedirectWithPlaceholder();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		waitForPlaceholders();

		eventLog.log(getName(), "All test steps have run. The test will remaining in 'WAITING' state until the required screenshot is uploaded using the 'Upload Images' button at the top of the page. It may take upto 30 seconds for the test to move to 'FINISHED' after the upload.");

		setStatus(Status.WAITING);
	}
}
