package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-qrdn-code-enforcement-test",
	displayName = "Payments Consents API test module to enforce qrdn local instrument",
	summary = "Payments Consents API test module ensuring a qr code is mandatory when the local instrument is QRDN. Test will do a POST Consents using localinstrument equal to qrdn but won't send a qrcode on the request payload. Test will expect a 422 response from the server",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilPaymentConsent"
	}
)
public class PaymentsConsentsApiEnforceQRDNTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndStopOnFailure(AddBrazilPixPaymentToTheResource.class);
		callAndContinueOnFailure(SelectQRDNCodeLocalInstrument.class);
		callAndContinueOnFailure(RemoveQRCodeFromConfig.class);
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
			callAndStopOnFailure(EnsureConsentResponseCodeWas422.class);

			callAndContinueOnFailure(ValidateErrorAndMetaFieldNames.class, Condition.ConditionResult.FAILURE);
		});
	}
}
