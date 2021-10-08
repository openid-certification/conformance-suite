package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.FAPIBrazilCreatePaymentConsentRequest;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-consents-api-manu-test",
	displayName = "Payments Consents API test module for manu local instrument",
	summary = "Payments Consents API test module ensuring a qr code must be absent when the local instrument is MANU",
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
public class PaymentsConsentsApiEnforceMANUTestModule extends AbstractClientCredentialsGrantFunctionalTestModule {

	@Override
	protected void postConfigure(JsonObject config, String baseUrl, String externalUrlOverride) {
		callAndContinueOnFailure(SelectMANUCodeLocalInstrument.class);
		callAndStopOnFailure(EnsureQRCodePresentInConfig.class);
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
			callAndContinueOnFailure(EnsureResponseCodeWas400.class);

		});
	}

}
