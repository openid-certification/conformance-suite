package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.PaymentConsentsErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-manu-fail-test",
	displayName = "Payments Consents API test module for manu local instrument",
	summary = "Payments Consents API test module ensuring a qr code must be absent when the local instrument is MANU" +
		"Flow:" +
		"Makes a payment flow. Expects a 422 due to a present QR code." +
		"Required:" +
		"Consent url pointing at the consent endpoint." +
		"Resource url pointing at the base url. The test appends on the required payment endpoints" +
		"Config: QR code will be provided by the test config. Proxy field must be present in both the consents and payment config so we can remove it",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl"
	}
)
public class PaymentsConsentsApiEnforceMANUTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void preConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndStopOnFailure(AddResourceUrlToConfig.class);
	}

	@Override
	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndStopOnFailure(AddBrazilPixPaymentToTheResource.class);
		eventLog.startBlock("Setting date to today");
		callAndStopOnFailure(EnsurePaymentDateIsToday.class);
		callAndContinueOnFailure(SelectMANUCodeLocalInstrument.class);
		callAndStopOnFailure(EnsureQRCodePresentInConfig.class);
		callAndStopOnFailure(RemoveProxyFromConsentConfig.class);
		callAndStopOnFailure(RemoveProxyFromPaymentConfig.class);
	}

	@Override
	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainPaymentsAccessTokenWithClientCredentials(clientAuthSequence);
	}

	@Override
	protected void runTests() {
		runInBlock("Validate payment initiation consent", () -> {
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(sequence(PaymentConsentErrorTestingSequence.class));
			callAndContinueOnFailure(EnsureConsentResponseCodeWas422.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Checking consent is rejected if MANU has a proxy in the payload.", () -> {
			callAndStopOnFailure(RemoveQRCodeFromConfig.class);
			callAndStopOnFailure(EnsureProxyPresentInConfig.class);
			callAndStopOnFailure(PrepareToPostConsentRequest.class);
			callAndStopOnFailure(FAPIBrazilCreatePaymentConsentRequest.class);

			call(sequence(PaymentConsentErrorTestingSequence.class));
			callAndStopOnFailure(EnsureConsentResponseCodeWas422.class);
			callAndStopOnFailure(PaymentConsentsErrorValidator.class);
		});
	}

}
