package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentFetchPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-test-no-debtor-account",
	displayName = "Payments API test module to allow user to select debtor account",
	summary = "This test checks a user is asked to select a debtor account when not provided in the request.\n\n" +
		"Flow:\n" +
		"Makes a good payment flow - expects success. Screenshot should be provided proving the user had to select a debtor account.\n\n" +
		"Required:\n" +
		"Consent url pointing at the consent endpoint.\n" +
		"Resource url pointing at the base url. The test appends on the required payment endpoints\n" +
		"Config: Debtor account is removed from the config by the test\n\n" +
		"A screenshot should be uploaded showing the user is presented with an option to select an account.",
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
		"resource.resourceUrl"
	}
)
public class PaymentApiNoDebtorProvidedTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(EnforceAbsenceOfDebtorAccount.class);
		callAndStopOnFailure(SanitiseQrCodeConfig.class);

		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(AskForScreenshotWithAccountSelection.class);

		env.putString("error_callback_placeholder", env.getString("payments_placeholder"));
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(PaymentInitiationPixPaymentsValidator.class, Condition.ConditionResult.FAILURE);
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
			.replace(CallProtectedResourceWithBearerToken.class, sequenceOf(
				condition(AddJWTAcceptHeader.class),
				condition(CallProtectedResourceWithBearerTokenAndCustomHeaders.class)
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
