package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentFetchPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.paymentInitiation.PaymentInitiationPixPaymentsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-dict-pix-response-test",
	displayName = "Payments API test module for dict local instrument pix response",
	summary = "Payments API test module ensuring that the pix response for dict local instrument is correct " +
		"Flow: " +
		"Makes a good payment flow with a local instrument of DICT - expects success. " +
		"Required: " +
		"Consent url pointing at the consent endpoint. " +
		"Resource url pointing at the base url. The test appends on the required payment endpoints " +
		"Config: We manually set the local instrument for both consent and payment to DICT for this test. ",
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
public class PaymentsConsentsApiDICTPixResponseTestModule extends AbstractOBBrasilFunctionalTestModule {


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
		callAndStopOnFailure(SelectDICTCodeLocalInstrument.class);
		callAndStopOnFailure(SelectDICTCodePixLocalInstrument.class);
		callAndStopOnFailure(RemoveTransactionIdentification.class);
		callAndStopOnFailure(RemoveQRCodeFromConfig.class);
		callAndStopOnFailure(EnsureProxyPresentInConfig.class);
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
			.replace(CallProtectedResource.class, sequenceOf(
				condition(AddJWTAcceptHeader.class),
				condition(CallProtectedResource.class)
			)));

	}
}
