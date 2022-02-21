package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentFetchPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-manu-pix-response-test",
	displayName = "Payments API test module for MANU local instrument pix response",
	summary = "Payments API test module ensuring that the pix response for MANU local instrument is correct" +
		"Flow:" +
		"Makes a good payment flow with a local instrument of MANU - expects success." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Resource url pointing at the base url. The test appends on the required payment endpoints" +
		"Config: Proxy field must be present in both payment and consent so we can remove it. We manually set the local instrument for both consent and payment to MANU for this test.",
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
public class PaymentsConsentsApiMANUPixResponseTestModule extends AbstractOBBrasilFunctionalTestModule {


	@Override
	protected void validateClientConfiguration() {
		callAndStopOnFailure(AddPaymentScope.class);
		super.validateClientConfiguration();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndStopOnFailure(PrepareToPostConsentRequest.class);
		callAndStopOnFailure(SetProtectedResourceUrlToPaymentsEndpoint.class);
		callAndStopOnFailure(SelectMANUCodeLocalInstrument.class);
		callAndStopOnFailure(SelectMANUCodePixLocalInstrument.class);
		callAndStopOnFailure(RemoveTransactionIdentification.class);
		callAndStopOnFailure(RemoveQRCodeFromConfig.class);
		callAndStopOnFailure(RemoveProxyFromConsentConfig.class);
		callAndStopOnFailure(RemoveProxyFromPaymentConfig.class);
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
}
