package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-test-no-debtor-account",
	displayName = "Payments API test module to allow user to select debtor account",
	summary = "This test checks a user is asked to select a debtor account when not provided in the request." +
		"Flow:" +
		"Makes a good payment flow - expects success. Screenshot should be provided proving the user had to select a debtor account." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Resource url pointing at the base url. The test appends on the required payment endpoints" +
		"Config: Debtor account is removed from the config by the test",
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
		createPlaceholder();

		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(AskForScreenshotWithAccountSelection.class);

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(PaymentInitiationPixPaymentsValidator.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(EnsureSelfLinkEndsInPaymentId.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(new ValidateSelfEndpoint()
			.replace(CallProtectedResourceWithBearerToken.class, sequenceOf(
				condition(AddJWTAcceptHeader.class),
				condition(CallProtectedResourceWithBearerTokenAndCustomHeaders.class)
			)));
		fireTestReviewNeeded();
	}

}
